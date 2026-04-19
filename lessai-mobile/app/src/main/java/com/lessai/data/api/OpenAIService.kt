package com.lessai.data.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.ResponseBody
import retrofit2.Response

data class ChatMessage(val role: String, val content: String)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.3f
)

data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage,
    val finish_reason: String?
)

data class StreamChunk(
    val id: String?,
    val choices: List<StreamChoice>?
)

data class StreamChoice(
    val delta: Delta?,
    val finish_reason: String?
)

data class Delta(
    val role: String?,
    val content: String?
)

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
    
    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatStream(@Body request: ChatRequest): Response<ResponseBody>
}