package com.lessai.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lessai.data.api.OpenAIService
import com.lessai.data.api.ChatMessage
import com.lessai.data.api.ChatRequest
import com.lessai.data.local.AppDatabase
import com.lessai.data.local.DataStoreManager
import com.lessai.data.local.SegmentEntity
import com.lessai.data.local.SessionEntity
import com.lessai.data.model.SegmentStatus
import com.lessai.ui.screens.MainUiState
import com.lessai.ui.screens.SegmentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import okhttp3.ResponseBody
import org.json.JSONObject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val database: AppDatabase,
    private val dataStore: DataStoreManager,
    private val openAIService: OpenAIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private var currentSessionId: String? = null

    fun loadSettings() {
        viewModelScope.launch {
            val baseUrl = dataStore.apiBaseUrl.first()
            val model = dataStore.model.first()
            _uiState.value = _uiState.value.copy(
                apiBaseUrl = baseUrl,
                model = model
            )
        }
    }

    fun importTxt(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val content = readTextFromUri(context, uri)
                val fileName = getFileName(context, uri) ?: "unknown.txt"
                
                // 分段策略
                val strategy = dataStore.segmentStrategy.first()
                val segments = splitText(content, strategy)
                
                // 创建会话
                val sessionId = UUID.randomUUID().toString()
                currentSessionId = sessionId
                
                // 保存到数据库
                val sessionEntity = SessionEntity(
                    id = sessionId,
                    fileName = fileName,
                    fullText = content
                )
                database.sessionDao().insertSession(sessionEntity)
                
                val segmentEntities = segments.mapIndexed { index, text ->
                    SegmentEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        index = index,
                        originalText = text,
                        rewrittenText = null,
                        status = SegmentStatus.PENDING.name
                    )
                }
                database.sessionDao().insertSegments(segmentEntities)
                
                // 更新UI
                _uiState.value = _uiState.value.copy(
                    sessionId = sessionId,
                    fileName = fileName,
                    segments = segmentEntities.mapIndexed { index, entity ->
                        SegmentUiState(
                            id = entity.id,
                            index = index,
                            text = entity.originalText,
                            status = SegmentStatus.PENDING
                        )
                    },
                    isLoading = false,
                    hasContent = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "导入失败: ${e.message}"
                )
            }
        }
    }

    fun startRewrite() {
        viewModelScope.launch {
            val segments = _uiState.value.segments.filter { it.status == SegmentStatus.PENDING }
            if (segments.isEmpty()) return@launch
            
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            val apiKey = dataStore.apiKey.first()
            val model = dataStore.model.first()
            val baseUrl = dataStore.apiBaseUrl.first()
            
            if (apiKey.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "请先在设置中配置API Key"
                )
                return@launch
            }

            // 逐个处理段落
            segments.forEach { segment ->
                try {
                    // 更新状态为生成中
                    updateSegmentStatus(segment.id, SegmentStatus.GENERATING)
                    
                    // 调用AI API
                    val request = ChatRequest(
                        model = model,
                        messages = listOf(
                            ChatMessage("system", "你是一个中文文本改写助手。请改写以下文本，使其更加流畅自然，保持原意不变。只返回改写后的文本，不要添加解释。"),
                            ChatMessage("user", segment.text)
                        ),
                        stream = false
                    )
                    
                    val response = openAIService.chat(request)
                    val rewrittenText = response.choices.firstOrNull()?.message?.content?.trim()
                    
                    if (rewrittenText != null) {
                        // 更新数据库
                        currentSessionId?.let { sessionId ->
                            database.sessionDao().updateRewrite(
                                segment.id,
                                rewrittenText,
                                SegmentStatus.REVIEWING.name
                            )
                        }
                        
                        // 更新UI
                        updateSegmentRewritten(segment.id, rewrittenText, SegmentStatus.REVIEWING)
                    }
                    
                } catch (e: Exception) {
                    updateSegmentStatus(segment.id, SegmentStatus.PENDING)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "改写失败: ${e.message}"
                    )
                }
            }
            
            _uiState.value = _uiState.value.copy(isProcessing = false)
        }
    }

    fun applyRewrite(segmentId: String) {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                val segment = database.sessionDao().getSegments(sessionId)
                    .find { it.id == segmentId } ?: return@launch
                
                database.sessionDao().updateRewrite(
                    segmentId,
                    segment.rewrittenText ?: "",
                    SegmentStatus.APPLIED.name
                )
                
                updateSegmentStatus(segmentId, SegmentStatus.APPLIED)
            }
        }
    }

    fun ignoreRewrite(segmentId: String) {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                database.sessionDao().updateRewrite(
                    segmentId,
                    null,
                    SegmentStatus.IGNORED.name
                )
                
                updateSegmentStatus(segmentId, SegmentStatus.IGNORED)
            }
        }
    }

    fun exportResult(context: Context) {
        viewModelScope.launch {
            try {
                val segments = _uiState.value.segments
                val appliedSegments = segments.filter { 
                    it.status == SegmentStatus.APPLIED || it.status == SegmentStatus.REVIEWING 
                }
                
                val result = appliedSegments.joinToString("\n\n") { 
                    it.rewritten.ifEmpty { it.text }
                }
                
                // 保存到文件
                saveToFile(context, result)
                
                _uiState.value = _uiState.value.copy(
                    successMessage = "已导出到Download/lessai_export.txt"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "导出失败: ${e.message}"
                )
            }
        }
    }

    private fun updateSegmentStatus(segmentId: String, status: SegmentStatus) {
        val segments = _uiState.value.segments.map {
            if (it.id == segmentId) it.copy(status = status) else it
        }
        _uiState.value = _uiState.value.copy(segments = segments)
    }

    private fun updateSegmentRewritten(segmentId: String, rewritten: String, status: SegmentStatus) {
        val segments = _uiState.value.segments.map {
            if (it.id == segmentId) it.copy(rewritten = rewritten, status = status) else it
        }
        _uiState.value = _uiState.value.copy(segments = segments)
    }

    private suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: throw Exception("无法读取文件")
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index)
            }
        }
        return name
    }

    private fun splitText(text: String, strategy: String): List<String> {
        return when (strategy) {
            "sentence" -> text.split(Regex("(?<=[。！？.!?])")).filter { it.isNotBlank() }
            "paragraph" -> text.split(Regex("\n\n+")).filter { it.isNotBlank() }
            else -> text.chunked(500) // 小句模式，每500字符
        }
    }

    private suspend fun saveToFile(context: Context, content: String) = withContext(Dispatchers.IO) {
        val file = java.io.File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "Download/lessai_export.txt")
        file.writeText(content)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}