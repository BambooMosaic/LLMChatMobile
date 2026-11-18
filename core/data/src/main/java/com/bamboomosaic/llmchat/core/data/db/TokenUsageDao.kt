package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    /** 从 token_stats_models 视图读取按 model 的聚合统计。 */
    @Query("SELECT * FROM token_stats_models ORDER BY model")
    fun observeModelStats(): Flow<List<ModelStatsView>>
}
