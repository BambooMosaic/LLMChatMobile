package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一套可保存的 API 配置。 */
@Entity(tableName = "api_configs")
data class ApiConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val provider: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    /** 1 = 支持深度思考 */
    val supportReasoning: Int = 0,
    /** 1 = 默认配置（全局只有一条为 1，由 Dao 的 CASE 语句维护） */
    val isDefault: Int = 0,
)
