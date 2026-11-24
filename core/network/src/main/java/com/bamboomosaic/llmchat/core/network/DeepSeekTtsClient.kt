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
 * 接口：POST https://api.deepseek.com/v1/tools/tts（Bearer 鉴权，音频在响应体流式返回）。
 * 音色：stella=海星（女，默认）/ tide=暗潮（男）；filter_markdown=true 由服务端去掉 Markdown 格式。
 * 限制：text 最多 10,000 计费字符（汉字算 2）、请求体 64 KiB——调用方负责截断。
 * 27 轮修正：要 PCM 裸流（24kHz 16bit mono）而非 WAV——流式 WAV 的长度头不标准会导致
 * MediaPlayer prepare 失败（status=0x1），PCM 配 AudioTrack 直接播放最可靠。
 */
class DeepSeekTtsClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS) // 长文本合成 + 下载可能超过一分钟
        .build()

    /** 合成并返回完整 PCM 字节（24kHz 16bit 单声道；服务端 filter_markdown 默认开启）。 */
    fun synthesize(apiKey: String, text: String, voiceId: String): Result<ByteArray> = runCatching {
        println("[LLMChatTTS] request tts: text=${text.length}c voice=$voiceId key=${apiKey.take(6)}...")
        val payload = JsonObject(
            mapOf(
                "text" to JsonPrimitive(text),
                "voice_id" to JsonPrimitive(if (voiceId == "tide") "tide" else "stella"),
                "format" to JsonPrimitive("pcm"),
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
                println("[LLMChatTTS] tts http ${response.code}: $raw")
                throw IOException(ttsErrorHint(response.code, raw))
            }
            val bytes = response.body.bytes()
            check(bytes.isNotEmpty()) { "TTS 返回空音频" }
            println("[LLMChatTTS] tts ok pcm: ${bytes.size} bytes")
            bytes
        }
    }

    private fun ttsErrorHint(code: Int, raw: String): String = when (code) {
        400 -> "没有可朗读的文本或内容未通过审核"
        429 -> "朗读请求并发超限，请稍后再试"
        503 -> "TTS 服务繁忙，请稍后再试"
        504 -> "合成超时，请稍后再试"
        401 -> "API Key 无效"
        402 -> "DeepSeek 账户余额不足，请充值后重试"
        else -> "HTTP $code ${raw.take(120)}"
    }

    private companion object {
        const val TTS_URL = "https://api.deepseek.com/v1/tools/tts"
    }
}
