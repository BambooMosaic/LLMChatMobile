package com.bamboomosaic.llmchat.core.network

import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.MessageRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

    /** 推理模型的思考内容（reasoning_content）；当前只用于兜底，不直接展示。 */
    data class Reasoning(val chunk: String) : SseEvent

    /** 联网搜索：一次搜索类工具调用开始（DeepSeek server_tool_event）。 */
    data object SearchStarted : SseEvent

    /** 联网搜索：一次搜索类工具调用完成，携带本次返回的网页结果条数。 */
    data class SearchCompleted(val pageCount: Int) : SseEvent
    data class Usage(val promptTokens: Long, val completionTokens: Long) : SseEvent
}

@Serializable
private data class ChatRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    @SerialName("stream_options") val streamOptions: StreamOptionsDto = StreamOptionsDto(),
    @SerialName("max_tokens") val maxTokens: Int? = null,
    // DeepSeek 系：thinking.type = enabled/disabled。只在开启时才发送，
    // 关闭时不带该字段——部分厂商不认识它会报 400，另一些会因此切进推理通道。
    @SerialName("thinking") val thinking: ThinkingDto? = null,
    // OpenAI 系：reasoning_effort
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    // DeepSeek 联网搜索：服务端工具声明（仅 deepseek_web_search）
    val tools: List<ToolDto>? = null,
    // 模式绑定的采样参数（模式体系注入；null = 不发，用服务端默认）
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
)

@Serializable
private data class ToolDto(val type: String)

@Serializable
private data class ThinkingDto(val type: String)

@Serializable
private data class StreamOptionsDto(
    @SerialName("include_usage") val includeUsage: Boolean = true,
)

@Serializable
private data class MessageDto(val role: String, val content: JsonElement)

@Serializable
private data class StreamChunkDto(
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
)

@Serializable
private data class ChoiceDto(
    val delta: DeltaDto = DeltaDto(),
    // DeepSeek 联网搜索事件（与 delta 并列的扩展字段），结构不定，手动解析
    @SerialName("server_tool_event") val serverToolEvent: JsonElement? = null,
)

@Serializable
private data class DeltaDto(
    @SerialName("content") val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

@Serializable
private data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Long = 0,
    @SerialName("completion_tokens") val completionTokens: Long = 0,
)

/** 非流式响应（服务端忽略 stream 参数时的兜底格式）。 */
@Serializable
private data class NonStreamResponseDto(
    val choices: List<NonStreamChoiceDto> = emptyList(),
)

@Serializable
private data class NonStreamChoiceDto(
    val message: NonStreamMessageDto = NonStreamMessageDto(),
)

@Serializable
private data class NonStreamMessageDto(val content: String? = null)

/**
 * 手写 OkHttp SSE 客户端，调用 OpenAI 兼容的 /v1/chat/completions。
 * 无状态：所有配置通过参数传入；以 SseEvent 流发射文本片段与最终 usage。
 */
class OpenAiSseClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    // encodeDefaults 必须为 true：否则 stream / stream_options 这类"带默认值的字段"会被
    // 序列化时省略，服务端收到普通请求并返回整段 JSON（无 data: 行），流式解析拿不到任何内容。
    // explicitNulls = false：避免把 thinking/max_tokens 的 null 也发给服务端（部分厂商会报 400）。
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    },
) {
    fun stream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        thinkingEnabled: Boolean = false,
        maxTokens: Int? = null,
        searchEnabled: Boolean = false,
        temperature: Float? = null,
        topP: Float? = null,
    ): Flow<SseEvent> = callbackFlow {
        // 各厂商"深度思考"参数不同：DeepSeek 用 thinking.type，OpenAI 系用 reasoning_effort。
        // 注意：DeepSeek 默认就会思考（不传也会返回 reasoning_content），
        // 因此必须显式发 disabled 才能真正关闭；其他厂商不发该字段，避免不认参数报 400。
        val isDeepSeek = baseUrl.contains("deepseek", ignoreCase = true)
        val payload = ChatRequestDto(
            model = model,
            messages = messages.map { it.toWireDto() },
            // 显式传，不依赖默认值（默认值在 encodeDefaults=false 时会被省略）
            stream = true,
            streamOptions = StreamOptionsDto(includeUsage = true),
            maxTokens = maxTokens,
            thinking = if (isDeepSeek) {
                ThinkingDto(if (thinkingEnabled) "enabled" else "disabled")
            } else {
                null
            },
            reasoningEffort = if (thinkingEnabled && !isDeepSeek) "high" else null,
            // 联网搜索仅 DeepSeek 支持（deepseek_web_search 为服务端工具，搜索在服务端完成）
            tools = if (searchEnabled && isDeepSeek) listOf(ToolDto("deepseek_web_search")) else null,
            // 模式绑定的采样参数（Float -> Double 序列化）
            temperature = temperature?.toDouble(),
            topP = topP?.toDouble(),
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
                    println("[SSE] ${resp.code} $endpoint model=$model thinking=$thinkingEnabled maxTokens=$maxTokens temp=$temperature topP=$topP")
                    if (!resp.isSuccessful) {
                        val errBody = runCatching { resp.body?.string() }.getOrNull()
                        println("[SSE] error body=${errBody?.take(500)}")
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
                        var sawDataLine = false
                        var emittedText = false
                        // 兜底：若服务端忽略了 stream 参数、直接返回整段 JSON，则缓存原文用于解析
                        val rawFallback = StringBuilder()

                        loop@ while (true) {
                            val line = source.readUtf8Line() ?: break@loop
                            if (!line.startsWith(DATA_PREFIX)) {
                                if (!sawDataLine && rawFallback.length < 64_000) {
                                    rawFallback.append(line).append('\n')
                                }
                                continue@loop
                            }
                            sawDataLine = true
                            val data = line.removePrefix(DATA_PREFIX).trim()
                            if (data == DONE_TOKEN) break@loop
                            if (data.isEmpty()) continue@loop

                            // 单条 chunk 解析失败不应中断整条流（不同厂商会追加自有字段）
                            val chunk = try {
                                json.decodeFromString<StreamChunkDto>(data)
                            } catch (e: Exception) {
                                println("[SSE] decode failed: ${e.message} | data=${data.take(300)}")
                                continue@loop
                            }

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
                            // DeepSeek 联网搜索事件（server_tool_event，与 delta 并列）
                            chunk.choices.firstOrNull()?.serverToolEvent?.let { emitSearchEvents(listOf(it)) }
                            val delta = chunk.choices.firstOrNull()?.delta ?: continue@loop
                            delta.content?.takeIf { it.isNotEmpty() }?.let { content ->
                                emittedText = true
                                if (!trySend(SseEvent.Text(content)).isSuccess) break@loop
                            }
                            // 推理内容单独走 Reasoning 事件，供上层决定是否展示
                            delta.reasoningContent?.takeIf { it.isNotEmpty() }?.let { reasoning ->
                                if (!trySend(SseEvent.Reasoning(reasoning)).isSuccess) break@loop
                            }
                        }

                        // 完全没收到 SSE 数据行时，尝试按非流式响应解析
                        if (!sawDataLine && !emittedText && rawFallback.isNotBlank()) {
                            val fallbackText = runCatching {
                                json.decodeFromString<NonStreamResponseDto>(rawFallback.toString())
                                    .choices.firstOrNull()?.message?.content
                            }.getOrNull()
                            if (!fallbackText.isNullOrEmpty()) {
                                trySend(SseEvent.Text(fallbackText))
                            } else {
                                println("[SSE] non-stream fallback failed, raw=${rawFallback.take(400)}")
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

    /**
     * 解析 DeepSeek 联网搜索事件（choice.server_tool_event，与 delta 并列）：
     * - call_started：搜索类调用开始 → SearchStarted
     * - call_completed：搜索类调用完成 → SearchCompleted（网页结果条数从 sources 提取）
     * read_link（读用户贴的链接）不算搜索，忽略；解析失败静默跳过，不影响正文流。
     */
    private fun kotlinx.coroutines.channels.ProducerScope<SseEvent>.emitSearchEvents(
        events: List<JsonElement>,
    ) {
        for (element in events) {
            val obj = element as? JsonObject ?: continue
            val type = (obj["type"] as? JsonPrimitive)?.content ?: continue
            val call = obj["call"] as? JsonObject ?: continue
            val kind = (call["kind"] as? JsonPrimitive)?.content ?: continue
            val isSearchKind = kind == "search" || kind == "deep_search" || kind == "image_search"
            when (type) {
                "call_started" -> if (isSearchKind) trySend(SseEvent.SearchStarted)
                "call_completed" -> {
                    if (!isSearchKind) continue
                    // "已搜索 N 个网页"：N = 本次调用返回的 url 类型结果条数
                    val sources = call["sources"] as? JsonArray ?: JsonArray(emptyList())
                    val pageCount = sources.count { src ->
                        (src as? JsonObject)?.get("type")?.let { (it as? JsonPrimitive)?.content } == "url"
                    }
                    trySend(SseEvent.SearchCompleted(pageCount))
                }
            }
        }
    }

    private fun MessageRole.wireValue(): String = when (this) {
        MessageRole.SYSTEM -> "system"
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
    }

    /**
     * 把领域消息转成 wire 格式。纯文本用字符串 content；
     * 带图片（多模态）时用数组 content：[{type:text,text:...},{type:image_url,image_url:{url:data:image/...;base64,...}}]。
     */
    private fun ChatMessage.toWireDto(): MessageDto {
        val role = role.wireValue()
        if (images.isEmpty()) {
            return MessageDto(role, JsonPrimitive(content))
        }
        val parts = buildJsonArray {
            if (content.isNotBlank()) {
                add(
                    buildJsonObject {
                        put("type", "text")
                        put("text", content)
                    },
                )
            }
            images.forEach { base64 ->
                add(
                    buildJsonObject {
                        put("type", "image_url")
                        put(
                            "image_url",
                            buildJsonObject {
                                put("url", "data:image/jpeg;base64,$base64")
                            },
                        )
                    },
                )
            }
        }
        return MessageDto(role, parts)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val DATA_PREFIX = "data:"
        const val DONE_TOKEN = "[DONE]"
    }
}
