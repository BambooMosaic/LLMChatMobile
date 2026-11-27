package com.bamboomosaic.llmchat.core.data.db

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

/**
 * 按 model 聚合的统计视图（对应 token_stats_models）。
 * 用 SQL SUM + GROUP BY 实时聚合，不手动累加；新增统计维度只需改 SQL，不改表结构。
 */
@DatabaseView(
    viewName = "token_stats_models",
    value = """
        SELECT
            model AS model,
            SUM(promptTokens) AS totalPromptTokens,
            SUM(completionTokens) AS totalCompletionTokens,
            COUNT(*) AS requestCount
        FROM token_usage_records
        GROUP BY model
    """,
)
data class ModelStatsView(
    @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "totalPromptTokens") val totalPromptTokens: Long,
    @ColumnInfo(name = "totalCompletionTokens") val totalCompletionTokens: Long,
    @ColumnInfo(name = "requestCount") val requestCount: Long,
)
