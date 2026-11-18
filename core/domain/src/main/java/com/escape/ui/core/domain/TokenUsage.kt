package com.escape.ui.core.domain

import kotlinx.coroutines.flow.Flow

/** 单次请求写入的原始 token 用量。 */
data class TokenUsageRecord(
    val sessionId: String,
    val model: String,
    val promptTokens: Long,
    val completionTokens: Long,
    val timestamp: Long,
)

/** 按 model 聚合后的统计（来自 token_stats_models 表）。 */
data class ModelTokenStats(
    val model: String,
    val totalPromptTokens: Long,
    val totalCompletionTokens: Long,
    val requestCount: Long,
)

/** 只读 token 统计，供 ViewModel 注入（不暴露 Room 实现）。 */
interface TokenUsageRepository {
    /** 某个会话内所有 model 的累计 token（prompt + completion）。 */
    fun observeSessionTotal(sessionId: String): Flow<Long>

    /** 按 model 聚合的统计，供扩展 UI 使用。 */
    fun observeModelStats(): Flow<List<ModelTokenStats>>
}
