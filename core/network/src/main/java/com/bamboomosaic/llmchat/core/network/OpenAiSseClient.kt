package com.bamboomosaic.llmchat.core.network

import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.MessageRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/** network 层内部事件；data 层把 Text 发射给 UI、把 Usage 写入 Room。 */
sealed interface SseEvent {
    data class Text(val chunk: String) : SseEvent
    data class Usage(val promptTokens: Long, val completionTokens: Long) : SseEvent
}

@Serializable
private data class ChatRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    @SerialName("stream_options") val streamOptions: StreamOptionsDto = StreamOptionsDto(),
    // DeepSeek 系：thinking.type = enabled/disabled
    @SerialName("thinking") val thinking: ThinkingDto? = null,
    // OpenAI 系：reasoning_effort
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
)

@Serializable
private data class ThinkingDto(val type: String)

@Serializable
private data class StreamOptionsDto(
    @SerialName("include_usage") val includeUsage: Boolean = true,
)

@Serializable
private data class MessageDto(val role: String, val content: String)

@Serializable
private data class StreamChunkDto(
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
)

@Serializable
private data class ChoiceDto(val delta: DeltaDto = DeltaDto())

@Serializable
private data class DeltaDto(@SerialName("content") val content: String? = null)

@Serializable
private data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Long = 0,
    @SerialName("completion_tokens") val completionTokens: Long = 0,
)

/**
 * 手写 OkHttp SSE 客户端，调用 OpenAI 兼容的 /v1/chat/completions。
 * 无状态：所有配置通过参数传入；以 SseEvent 流发射文本片段与最终 usage。
 */
class OpenAiSseClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun stream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        thinkingEnabled: Boolean = false,
    ): Flow<SseEvent> = callbackFlow {
        // 各厂商"深度思考"参数不同：DeepSeek 用 thinking.type，OpenAI 系用 reasoning_effort
        val isDeepSeek = baseUrl.contains("deepseek", ignoreCase = true)
        val payload = ChatRequestDto(
            model = model,
            messages = messages.map { MessageDto(it.role.wireValue(), it.content) },
            thinking = if (isDeepSeek) {
                ThinkingDto(if (thinkingEnabled) "enabled" else "disabled")
            } else {
                null
            },
            reasoningEffort = if (thinkingEnabled && !isDeepSeek) "high" else null,
        )
        val body = json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE)
        // baseUrl 约定已包含版本段（如 https://api.openai.com/v1），这里只拼 /chat/completions，
        // 避免出现 /v1/v1/chat/completions。
        val endpoint = "${baseUrl.trimEnd('/')}/chat/completions"
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(IOException("HTTP ${resp.code}: ${resp.message}"))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(IOException("Empty response body"))
                        return
                    }
                    try {
                        // 用 trySend 的返回值判断下游是否已关闭，避免使用 DelicateCoroutinesApi 的 isClosedForSend
                        loop@ while (true) {
                            val line = source.readUtf8Line() ?: break@loop
                            if (!line.startsWith(DATA_PREFIX)) continue@loop
                            val data = line.removePrefix(DATA_PREFIX).trim()
                            if (data == DONE_TOKEN) break@loop
                            if (data.isEmpty()) continue@loop
                            val chunk = json.decodeFromString<StreamChunkDto>(data)

                            // usage chunk：choices 为空、usage 非空
                            chunk.usage?.let { usage ->
                                val sent = trySend(
                                    SseEvent.Usage(
                                        promptTokens = usage.promptTokens,
                                        completionTokens = usage.completionTokens,
                                    ),
                                ).isSuccess
                                if (!sent) break@loop
                            }
                            chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                                if (content.isNotEmpty()) {
                                    if (!trySend(SseEvent.Text(content)).isSuccess) break@loop
                                }
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }

    private fun MessageRole.wireValue(): String = when (this) {
        MessageRole.SYSTEM -> "system"
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val DATA_PREFIX = "data:"
        const val DONE_TOKEN = "[DONE]"
    }
}
