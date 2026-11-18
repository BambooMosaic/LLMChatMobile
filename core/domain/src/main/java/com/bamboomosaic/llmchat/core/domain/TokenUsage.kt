package com.bamboomosaic.llmchat.core.domain

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

/** 输入/输出/总计三项 token 汇总。 */
data class TokenTotals(
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
) {
    val total: Long get() = promptTokens + completionTokens
}

/** 只读 token 统计，供 ViewModel 注入（不暴露 Room 实现）。 */
interface TokenUsageRepository {
    /** 某个会话内所有 model 的累计 token（prompt + completion）。 */
    fun observeSessionTotal(sessionId: String): Flow<Long>

    /** 某个会话的输入/输出明细。 */
    fun observeSessionTotals(sessionId: String): Flow<TokenTotals>

    /** 全应用累计（跨会话）的输入/输出明细。 */
    fun observeGlobalTotals(): Flow<TokenTotals>

    /** 该会话最近一次请求的用量，用于展示"本轮消耗"。 */
    suspend fun getLastUsage(sessionId: String): TokenUsageRecord?

    /** 按 model 聚合的统计，供扩展 UI 使用。 */
    fun observeModelStats(): Flow<List<ModelTokenStats>>
}
