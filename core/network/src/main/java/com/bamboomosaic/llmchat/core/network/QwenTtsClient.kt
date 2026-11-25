package com.bamboomosaic.llmchat.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 29 轮：通义千问语音合成（qwen3-tts-flash）客户端。
 * POST https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
 * （Bearer 鉴权，与通义千问聊天共用同一个 DashScope key）。
 * 响应是 JSON：output.audio.url 指向 OSS 上的 WAV 文件（24 小时有效）——需要二次 GET 下载音频字节。
 * 限制：text 最多 600 字符（截断由调用方负责）；音色如 Cherry/Serena/Ethan（预置音色，大小写敏感）。
 */
class QwenTtsClient : TtsClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // 合成 + 下载两次网络往返
        .build()

    override fun synthesize(apiKey: String, text: String, voiceId: String): Result<TtsAudio> =
        runCatching<TtsAudio> {
            println("[LLMChatTTS] qwen request: text=${text.length}c voice=$voiceId key=${apiKey.take(6)}...")
            val payload = JsonObject(
                mapOf(
                    "model" to JsonPrimitive("qwen3-tts-flash"),
                    "input" to JsonObject(
                        mapOf(
                            "text" to JsonPrimitive(text),
                            "voice" to JsonPrimitive(voiceId.ifBlank { "Cherry" }),
                        ),
                    ),
                ),
            ).toString()
            val request = Request.Builder()
                .url(TTS_URL)
                .header("Authorization", "Bearer $apiKey")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            val audioUrl = client.newCall(request).execute().use { response: Response ->
                val raw = runCatching { response.body.string() }.getOrDefault("")
                if (!response.isSuccessful) {
                    println("[LLMChatTTS] qwen tts http ${response.code}: $raw")
                    throw IOException(errorHint(response.code, raw))
                }
                val output = runCatching { Json.parseToJsonElement(raw).jsonObject["output"]?.jsonObject }
                    .getOrNull()
                    ?: throw IOException("通义千问 TTS 响应格式异常")
                output["audio"]?.jsonObject?.get("url")?.jsonPrimitive?.content
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IOException("通义千问 TTS 响应缺少音频地址")
            }
            // 二次 GET 下载 WAV（签名 URL，无需鉴权；失败自动重试一次）
            val bytes = download(audioUrl) ?: download(audioUrl)
                ?: throw IOException("音频下载失败，请稍后再试")
            check(bytes.isNotEmpty()) { "TTS 返回空音频" }
            println("[LLMChatTTS] qwen tts ok wav: ${bytes.size} bytes")
            TtsAudio.Wav(bytes)
        }

    private fun download(url: String): ByteArray? = runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body.bytes()
        }
    }.getOrNull()

    /** DashScope 错误体：{"code":"...","message":"..."}。 */
    private fun errorHint(code: Int, raw: String): String = when (code) {
        401 -> "通义千问 API Key 无效"
        429 -> "朗读请求并发超限，请稍后再试"
        else -> {
            val message = runCatching {
                Json.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.content
            }.getOrNull()
            message?.takeIf { it.isNotBlank() } ?: "HTTP $code ${raw.take(120)}"
        }
    }

    private companion object {
        const val TTS_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
    }
}
