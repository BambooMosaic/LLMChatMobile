package com.bamboomosaic.llmchat.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** SUM 聚合结果（输入/输出/缓存命中/联网搜索/深度思考分开），仅用于查询映射。 */
data class TokenTotalsRow(
    @ColumnInfo(name = "promptTokens") val promptTokens: Long,
    @ColumnInfo(name = "completionTokens") val completionTokens: Long,
    @ColumnInfo(name = "cacheHitTokens") val cacheHitTokens: Long,
    @ColumnInfo(name = "searchTokens") val searchTokens: Long = 0,
    @ColumnInfo(name = "reasoningTokens") val reasoningTokens: Long = 0,
)

/** 按本地 3 小时时段聚合的 token 消耗（近 1 天柱状图数据源，三色分段）。 */
data class HourlyTokenRow(
    @ColumnInfo(name = "hour") val hour: Int,
    @ColumnInfo(name = "tokens") val tokens: Long,
    @ColumnInfo(name = "cacheHitTokens") val cacheHitTokens: Long,
    @ColumnInfo(name = "inputTokens") val inputTokens: Long,
    @ColumnInfo(name = "outputTokens") val outputTokens: Long,
)

/** 按本地日期聚合的每日 token 消耗（柱状图数据源，三色分段）。 */
data class DailyTokenRow(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "tokens") val tokens: Long,
    @ColumnInfo(name = "cacheHitTokens") val cacheHitTokens: Long,
    @ColumnInfo(name = "inputTokens") val inputTokens: Long,
    @ColumnInfo(name = "outputTokens") val outputTokens: Long,
)

@Dao
interface TokenUsageDao {
    /** 按主键取用量记录（多版本切换显示对应消耗）。 */
    @Query("SELECT * FROM token_usage_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): TokenUsageRecordEntity?

    /** 某会话最近一条用量记录 id（消息 usageId 关联用）。 */
    @Query("SELECT id FROM token_usage_records WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 1")
    suspend fun getLastUsageId(sessionId: String): Long?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TokenUsageRecordEntity)

    // ===== 29 轮：全量备份 =====

    @Query("SELECT * FROM token_usage_records")
    suspend fun getAllRecords(): List<TokenUsageRecordEntity>

    @Query("DELETE FROM token_usage_records")
    suspend fun deleteAllRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecords(records: List<TokenUsageRecordEntity>)

    /** 某会话累计 token（prompt + completion），用 SQL SUM 聚合，不手动累加。 */
    @Query(
        """
        SELECT COALESCE(SUM(promptTokens + completionTokens), 0)
        FROM token_usage_records
        WHERE sessionId = :sessionId
        """,
    )
    fun observeSessionTotal(sessionId: String): Flow<Long>

    /** 某会话输入/输出/缓存命中/联网搜索/深度思考明细。 */
    @Query(
        """
        SELECT COALESCE(SUM(promptTokens), 0) AS promptTokens,
               COALESCE(SUM(completionTokens), 0) AS completionTokens,
               COALESCE(SUM(cacheHitTokens), 0) AS cacheHitTokens,
               COALESCE(SUM(CASE WHEN isSearch = 1 THEN promptTokens + completionTokens ELSE 0 END), 0) AS searchTokens,
               COALESCE(SUM(reasoningTokens), 0) AS reasoningTokens
        FROM token_usage_records
        WHERE sessionId = :sessionId
        """,
    )
    fun observeSessionTotals(sessionId: String): Flow<TokenTotalsRow>

    /** 全应用输入/输出/缓存命中/联网搜索/深度思考累计（跨会话）。 */
    @Query(
        """
        SELECT COALESCE(SUM(promptTokens), 0) AS promptTokens,
               COALESCE(SUM(completionTokens), 0) AS completionTokens,
               COALESCE(SUM(cacheHitTokens), 0) AS cacheHitTokens,
               COALESCE(SUM(CASE WHEN isSearch = 1 THEN promptTokens + completionTokens ELSE 0 END), 0) AS searchTokens,
               COALESCE(SUM(reasoningTokens), 0) AS reasoningTokens
        FROM token_usage_records
        """,
    )
    fun observeGlobalTotals(): Flow<TokenTotalsRow>

    /** 该会话最近一条用量记录（用于"本轮消耗"展示）。 */
    @Query(
        """
        SELECT * FROM token_usage_records
        WHERE sessionId = :sessionId
        ORDER BY timestamp DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getLastRecord(sessionId: String): TokenUsageRecordEntity?

    /** 从 token_stats_models 视图读取按 model 的聚合统计。 */
    @Query("SELECT * FROM token_stats_models ORDER BY model")
    fun observeModelStats(): Flow<List<ModelStatsView>>

    /** 从 [since]（epoch millis）起，按本地日期聚合每日 token 消耗（三色分段）。 */
    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch', 'localtime') AS day,
               SUM(promptTokens + completionTokens) AS tokens,
               SUM(cacheHitTokens) AS cacheHitTokens,
               SUM(promptTokens - cacheHitTokens) AS inputTokens,
               SUM(completionTokens) AS outputTokens
        FROM token_usage_records
        WHERE timestamp >= :since
        GROUP BY day
        ORDER BY day ASC
        """,
    )
    fun observeDailyTotals(since: Long): Flow<List<DailyTokenRow>>

    /** 从 [since] 起按本地 3 小时时段聚合（hour = 0/3/6/.../21，三色分段）。 */
    @Query(
        """
        SELECT (CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) / 3) * 3 AS hour,
               SUM(promptTokens + completionTokens) AS tokens,
               SUM(cacheHitTokens) AS cacheHitTokens,
               SUM(promptTokens - cacheHitTokens) AS inputTokens,
               SUM(completionTokens) AS outputTokens
        FROM token_usage_records
        WHERE timestamp >= :since
        GROUP BY hour
        ORDER BY hour ASC
        """,
    )
    fun observeHourlyTotals(since: Long): Flow<List<HourlyTokenRow>>
}
