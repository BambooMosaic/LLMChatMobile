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
 * 29 轮：智谱 GLM 语音合成（glm-tts）客户端。
 * POST https://open.bigmodel.cn/api/paas/v4/audio/speech
 * （直接 Bearer key，与 GLM 聊天共用；无需 JWT）。
 * 请求 response_format=wav 时响应体即二进制 WAV 文件。
 * 限制：input 最多 1024 字符（截断由调用方负责）；音色 tongtong/chuichui/xiaochen/jam 等。
 */
class GlmTtsClient : TtsClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // 长文本合成可能超过一分钟
        .build()

    override fun synthesize(apiKey: String, text: String, voiceId: String): Result<TtsAudio> =
        runCatching<TtsAudio> {
            println("[LLMChatTTS] glm request: text=${text.length}c voice=$voiceId key=${apiKey.take(6)}...")
            val payload = JsonObject(
                mapOf(
                    "model" to JsonPrimitive("glm-tts"),
                    "input" to JsonPrimitive(text),
                    "voice" to JsonPrimitive(voiceId.ifBlank { "tongtong" }),
                    "response_format" to JsonPrimitive("wav"),
                    "speed" to JsonPrimitive(1.0),
                    "volume" to JsonPrimitive(1.0),
                ),
            ).toString()
            val request = Request.Builder()
                .url(TTS_URL)
                .header("Authorization", "Bearer $apiKey")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response: Response ->
                if (!response.isSuccessful) {
                    val raw = runCatching { response.body.string() }.getOrDefault("")
                    println("[LLMChatTTS] glm tts http ${response.code}: $raw")
                    throw IOException(errorHint(response.code, raw))
                }
                val bytes = response.body.bytes()
                check(bytes.isNotEmpty()) { "TTS 返回空音频" }
                println("[LLMChatTTS] glm tts ok wav: ${bytes.size} bytes")
                TtsAudio.Wav(bytes)
            }
        }

    /** BigModel 错误体：{"error":{"code":"...","message":"..."}}。 */
    private fun errorHint(code: Int, raw: String): String = when (code) {
        401 -> "智谱 API Key 无效"
        429 -> "朗读请求并发超限，请稍后再试"
        else -> {
            val message = runCatching {
                Json.parseToJsonElement(raw).jsonObject["error"]
                    ?.jsonObject?.get("message")?.jsonPrimitive?.content
            }.getOrNull()
            message?.takeIf { it.isNotBlank() } ?: "HTTP $code ${raw.take(120)}"
        }
    }

    private companion object {
        const val TTS_URL = "https://open.bigmodel.cn/api/paas/v4/audio/speech"
    }
}
