package com.bamboomosaic.llmchat.core.network

/**
 * 29 轮：TTS 合成结果统一模型——三家引擎（DeepSeek/Qwen/GLM）协议差异收敛到此处。
 */
sealed interface TtsAudio {
    /** DeepSeek：24kHz 16bit mono PCM 裸流（AudioTrack 直接播放）。 */
    data class Pcm(val bytes: ByteArray) : TtsAudio

    /** Qwen / GLM：标准 WAV 文件字节（写临时文件交 MediaPlayer 播放）。 */
    data class Wav(val bytes: ByteArray) : TtsAudio
}

/** 29 轮：TTS 引擎统一接口，调用方按引擎选实现，播放侧按 TtsAudio 分支。 */
interface TtsClient {
    fun synthesize(apiKey: String, text: String, voiceId: String): Result<TtsAudio>
}
