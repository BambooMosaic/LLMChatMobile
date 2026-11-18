package com.escape.ui.core.domain

/** 上下文窗口配置：最大 token 数与压缩触发阈值（占比）。 */
data class ContextWindow(
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val compressThreshold: Float = DEFAULT_COMPRESS_THRESHOLD,
) {
    /** 触发压缩的 token 线。 */
    val safeLine: Int get() = (maxTokens * compressThreshold).toInt()

    companion object {
        const val DEFAULT_MAX_TOKENS = 80000
        const val DEFAULT_COMPRESS_THRESHOLD = 0.9f
    }
}
