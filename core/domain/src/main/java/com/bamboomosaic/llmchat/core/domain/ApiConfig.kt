package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

/** 一套可保存的 API 配置（供应商 + 端点 + 密钥 + 模型）。 */
data class ApiConfig(
    val id: Long = 0,
    val name: String,
    val provider: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    /** 该模型是否支持"深度思考"；不支持时聊天页开关置灰。 */
    val supportReasoning: Boolean = false,
    /** 是否为默认配置（列表高亮 + 无选中记录时的兜底）。 */
    val isDefault: Boolean = false,
    /** 该配置独立的最大上下文长度；null = 用全局默认（API 高级选项）。 */
    val maxContextTokens: Int? = null,
    /** 该配置独立的最大输出长度；null = 用全局默认（API 高级选项）。 */
    val maxOutputTokens: Int? = null,
)

/** 多套 API 配置的持久化仓储，ViewModel 只注入此接口。 */
interface ConfigRepository {

    fun observeConfigs(): Flow<List<ApiConfig>>

    /** 当前生效配置：优先按 DataStore 里选中的 ID，其次默认配置，最后第一条。 */
    fun observeCurrentConfig(): Flow<ApiConfig?>

    suspend fun getCurrentConfig(): ApiConfig?

    /** 新增或更新；返回记录 id。 */
    suspend fun save(config: ApiConfig): Long

    /** 删除；若删的是默认配置，自动把第一条设为默认。至少保留一条。 */
    suspend fun delete(id: Long)

    suspend fun setDefault(id: Long)

    /** 切换当前使用的配置（只影响后续请求）。 */
    suspend fun select(id: Long)

    /**
     * 首次启动时把 DataStore 里的单条旧配置迁移成第一条默认配置。
     * 表里已有数据则什么都不做。
     */
    suspend fun ensureSeeded()
}
