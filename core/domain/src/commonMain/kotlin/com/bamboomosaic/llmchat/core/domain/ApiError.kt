package com.bamboomosaic.llmchat.core.domain

/**
 * API 请求错误的结构化分类（第 17 轮）。
 * 从库层异常 message 解析出错误类别，UI 按类别展示对应的解决引导。
 */
data class ApiError(
    val kind: Kind,
    val raw: String?,
) {
    enum class Kind { AUTH, INSUFFICIENT_BALANCE, RATE_LIMIT, TIMEOUT, NETWORK, UNKNOWN }

    companion object {
        /** 从异常 message 解析错误类别。 */
        fun from(message: String?): ApiError {
            val raw = message ?: "Unknown error"
            return when {
                raw.contains("HTTP 401") -> ApiError(Kind.AUTH, raw)
                raw.contains("HTTP 402") -> ApiError(Kind.INSUFFICIENT_BALANCE, raw)
                raw.contains("HTTP 429") -> ApiError(Kind.RATE_LIMIT, raw)
                raw.contains("timeout", ignoreCase = true) ||
                    raw.contains("timed out", ignoreCase = true) -> ApiError(Kind.TIMEOUT, raw)
                raw.contains("Unable to resolve host", ignoreCase = true) ||
                    raw.contains("Failed to connect", ignoreCase = true) ||
                    raw.contains("Network is unreachable", ignoreCase = true) ||
                    raw.contains("Connection reset", ignoreCase = true) -> ApiError(Kind.NETWORK, raw)
                else -> ApiError(Kind.UNKNOWN, raw)
            }
        }
    }
}
