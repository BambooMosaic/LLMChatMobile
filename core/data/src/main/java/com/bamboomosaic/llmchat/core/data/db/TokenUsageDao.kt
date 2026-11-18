package com.bamboomosaic.llmchat.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** SUM 聚合结果（输入/输出分开），仅用于查询映射。 */
data class TokenTotalsRow(
    @ColumnInfo(name = "promptTokens") val promptTokens: Long,
    @ColumnInfo(name = "completionTokens") val completionTokens: Long,
)

@Dao
interface TokenUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TokenUsageRecordEntity)

    /** 某会话累计 token（prompt + completion），用 SQL SUM 聚合，不手动累加。 */
    @Query(
        """
        SELECT COALESCE(SUM(promptTokens + completionTokens), 0)
        FROM token_usage_records
        WHERE sessionId = :sessionId
        """,
    )
    fun observeSessionTotal(sessionId: String): Flow<Long>

    /** 某会话输入/输出明细。 */
    @Query(
        """
        SELECT COALESCE(SUM(promptTokens), 0) AS promptTokens,
               COALESCE(SUM(completionTokens), 0) AS completionTokens
        FROM token_usage_records
        WHERE sessionId = :sessionId
        """,
    )
    fun observeSessionTotals(sessionId: String): Flow<TokenTotalsRow>

    /** 全应用输入/输出累计（跨会话）。 */
    @Query(
        """
        SELECT COALESCE(SUM(promptTokens), 0) AS promptTokens,
               COALESCE(SUM(completionTokens), 0) AS completionTokens
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
}
