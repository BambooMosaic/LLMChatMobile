package com.bamboomosaic.llmchat.core.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 27 轮：DeepSeek 语音合成（TTS）客户端。
 * 接口：POST https://api.deepseek.com/v1/tools/tts（Bearer 鉴权，音频 wav 在响应体流式返回）。
 * 音色：stella=海星（女，默认）/ tide=暗潮（男）；filter_markdown=true 由服务端去掉 Markdown 格式。
 * 限制：text 最多 10,000 计费字符（汉字算 2）、请求体 64 KiB——调用方负责截断。
 */
class DeepSeekTtsClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS) // 长文本合成 + 下载可能超过一分钟
        .build()

    /** 合成并返回完整 WAV 字节（服务端 filter_markdown 默认开启，Markdown 格式会被去掉）。 */
    fun synthesize(apiKey: String, text: String, voiceId: String): Result<ByteArray> = runCatching {
        val payload = JsonObject(
            mapOf(
                "text" to JsonPrimitive(text),
                "voice_id" to JsonPrimitive(if (voiceId == "tide") "tide" else "stella"),
                "format" to JsonPrimitive("wav"),
                "filter_markdown" to JsonPrimitive(true),
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
                throw IOException(ttsErrorHint(response.code, raw))
            }
            val bytes = response.body.bytes()
            check(bytes.isNotEmpty()) { "TTS 返回空音频" }
            bytes
        }
    }

    private fun ttsErrorHint(code: Int, raw: String): String = when (code) {
        400 -> "没有可朗读的文本或内容未通过审核"
        429 -> "朗读请求并发超限，请稍后再试"
        503 -> "TTS 服务繁忙，请稍后再试"
        504 -> "合成超时，请稍后再试"
        401 -> "API Key 无效"
        else -> "HTTP $code ${raw.take(120)}"
    }

    private companion object {
        const val TTS_URL = "https://api.deepseek.com/v1/tools/tts"
    }
}
