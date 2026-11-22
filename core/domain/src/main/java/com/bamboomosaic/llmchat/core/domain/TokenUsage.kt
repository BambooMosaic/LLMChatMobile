package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

/** 单次请求写入的原始 token 用量。 */
data class TokenUsageRecord(
    val sessionId: String,
    val model: String,
    val promptTokens: Long,
    val completionTokens: Long,
    val timestamp: Long,
    /** 20 轮：prompt 中命中缓存的 token 数（非 DeepSeek 系模型为 0）。 */
    val cacheHitTokens: Long = 0,
)

/** 按 model 聚合后的统计（来自 token_stats_models 表）。 */
data class ModelTokenStats(
    val model: String,
    val totalPromptTokens: Long,
    val totalCompletionTokens: Long,
    val requestCount: Long,
)

/** 某一天（本地时区 yyyy-MM-dd）的 token 消耗，用于柱状图（三色分段：缓存命中/输入/输出）。 */
data class DailyTokenStats(
    val date: String,
    val tokens: Long,
    val cacheHitTokens: Long = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
)

/** 某 3 小时时段（本地时区起始小时 0/3/6/.../21）的 token 消耗，用于柱状图。 */
data class HourlyTokenStats(
    val hour: Int,
    val tokens: Long,
    val cacheHitTokens: Long = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
)

/** 输入/输出/总计/缓存命中 token 汇总。 */
data class TokenTotals(
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val cacheHitTokens: Long = 0,
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

    /** 按记录 id 取用量（多版本切换时按版本显示消耗）。 */
    suspend fun getUsageById(id: Long): TokenUsageRecord?

    /** 按 model 聚合的统计，供扩展 UI 使用。 */
    fun observeModelStats(): Flow<List<ModelTokenStats>>

    /** 从 [since]（epoch millis）起按本地日期聚合的每日 token 消耗，供柱状图使用。 */
    fun observeDailyTotals(since: Long): Flow<List<DailyTokenStats>>

    /** 从 [since]（epoch millis）起按本地 3 小时时段聚合的 token 消耗，供近 1 天柱状图使用。 */
    fun observeHourlyTotals(since: Long): Flow<List<HourlyTokenStats>>
}
