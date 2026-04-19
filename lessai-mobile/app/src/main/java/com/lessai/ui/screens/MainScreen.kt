package com.lessai.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lessai.data.model.SegmentStatus
import com.lessai.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("工作台", "审阅", "设置")
    val context = LocalContext.current
    
    // SAF文件选择器
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importTxt(context, it) }
    }
    
    // 初始化时加载设置
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "LessAI",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        uiState.fileName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.exportResult(context) },
                        enabled = uiState.hasContent
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "导出")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when(index) {
                                    0 -> Icons.Default.Edit
                                    1 -> Icons.Default.Reviews
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0 && !uiState.isProcessing && uiState.segments.isNotEmpty(),
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.startRewrite() },
                    icon = { Icon(Icons.Default.AutoFixHigh, null) },
                    text = { Text("开始改写") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    ) { padding ->
        when(selectedTab) {
            0 -> WorkbenchScreen(
                uiState = uiState,
                onImport = { filePicker.launch(arrayOf("text/plain")) },
                onApply = viewModel::applyRewrite,
                onIgnore = viewModel::ignoreRewrite,
                modifier = Modifier.padding(padding)
            )
            1 -> ReviewScreen(
                uiState = uiState,
                onApply = viewModel::applyRewrite,
                onIgnore = viewModel::ignoreRewrite,
                modifier = Modifier.padding(padding)
            )
            2 -> SettingsScreen(
                modifier = Modifier.padding(padding)
            )
        }
    }
    
    // 错误提示
    uiState.errorMessage?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = viewModel::clearError
        )
    }
    
    // 成功提示
    uiState.successMessage?.let { message ->
        SuccessSnackBar(
            message = message,
            onDismiss = viewModel::clearSuccess
        )
    }
}

@Composable
fun WorkbenchScreen(
    uiState: MainUiState,
    onImport: () -> Unit,
    onApply: (String) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 导入按钮
        if (!uiState.hasContent) {
            ImportCard(onImport)
        }
        
        // 处理进度
        if (uiState.isProcessing) {
            ProcessingIndicator(
                current = uiState.segments.count { it.status != SegmentStatus.PENDING },
                total = uiState.segments.size
            )
        }
        
        // 段落列表
        AnimatedContent(
            targetState = uiState.segments.isNotEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { hasContent ->
            if (hasContent) {
                SegmentList(
                    segments = uiState.segments,
                    onApply = onApply,
                    onIgnore = onIgnore,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ImportCard(onImport: () -> Unit) {
    Card(
        onClick = onImport,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.FileOpen,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "点击导入TXT文件",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "支持.txt纯文本文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ProcessingIndicator(current: Int, total: Int) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "正在改写...",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "$current / $total",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SegmentList(
    segments: List<SegmentUiState>,
    onApply: (String) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(segments, key = { it.id }) { segment ->
            SegmentCard(
                segment = segment,
                onApply = { onApply(segment.id) },
                onIgnore = { onIgnore(segment.id) }
            )
        }
    }
}

@Composable
fun SegmentCard(
    segment: SegmentUiState,
    onApply: () -> Unit,
    onIgnore: () -> Unit
) {
    val statusColor = when(segment.status) {
        SegmentStatus.PENDING -> MaterialTheme.colorScheme.outline
        SegmentStatus.GENERATING -> MaterialTheme.colorScheme.primary
        SegmentStatus.REVIEWING -> MaterialTheme.colorScheme.tertiary
        SegmentStatus.APPLIED -> MaterialTheme.colorScheme.primary
        SegmentStatus.IGNORED -> MaterialTheme.colorScheme.error
    }
    
    val statusText = when(segment.status) {
        SegmentStatus.PENDING -> "待处理"
        SegmentStatus.GENERATING -> "生成中..."
        SegmentStatus.REVIEWING -> "待审阅"
        SegmentStatus.APPLIED -> "已应用"
        SegmentStatus.IGNORED -> "已忽略"
    }
    
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态指示点
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "段落 ${segment.index + 1}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 原文
            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // 改写结果
            AnimatedVisibility(
                visible = segment.rewritten.isNotEmpty(),
                enter = expandVertically() + fadeIn()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(
                        color = statusColor.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "改写建议:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = segment.rewritten,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 操作按钮
            if (segment.status == SegmentStatus.REVIEWING) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onIgnore,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(Modifier.width(4.dp))
                        Text("忽略")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onApply) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("应用")
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(
    uiState: MainUiState,
    onApply: (String) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reviewingSegments = uiState.segments.filter { 
        it.status == SegmentStatus.REVIEWING || it.status == SegmentStatus.APPLIED || it.status == SegmentStatus.IGNORED 
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (reviewingSegments.isEmpty()) {
            EmptyReviewState()
        } else {
            Text(
                "审阅 (${reviewingSegments.count { it.status == SegmentStatus.REVIEWING }} 待处理)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SegmentList(
                segments = reviewingSegments,
                onApply = onApply,
                onIgnore = onIgnore,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun EmptyReviewState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Reviews,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "暂无需要审阅的内容",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            "先在工作台导入并改写文本",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    // 简化版设置界面
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "API 设置",
            style = MaterialTheme.typography.headlineSmall
        )
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("API Base URL") },
            placeholder = { Text("https://api.openai.com/v1") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Link, null) }
        )
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Key, null) }
        )
        
        OutlinedTextField(
            value = "gpt-4.1-mini",
            onValueChange = {},
            label = { Text("模型") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.SmartToy, null) }
        )
        
        Divider()
        
        Text(
            "分段策略",
            style = MaterialTheme.typography.titleMedium
        )
        
        SegmentedButton(
            options = listOf("小句", "整句", "段落"),
            selectedIndex = 1,
            onSelectionChange = {}
        )
    }
}

@Composable
fun SegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { onSelectionChange(index) },
                label = { Text(option) },
                modifier = Modifier.weight(1f),
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Error, null) },
        title = { Text("出错了") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

@Composable
fun SuccessSnackBar(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(2000)
        onDismiss()
    }
    
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    ) {
        Text(message)
    }
}

// UI State
data class MainUiState(
    val sessionId: String? = null,
    val fileName: String? = null,
    val segments: List<SegmentUiState> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val hasContent: Boolean = false,
    val apiBaseUrl: String = "",
    val model: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class SegmentUiState(
    val id: String,
    val index: Int,
    val text: String,
    val status: SegmentStatus = SegmentStatus.PENDING,
    val rewritten: String = ""
)