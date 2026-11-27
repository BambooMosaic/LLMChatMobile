package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** @Serializable：29 轮全量备份直接序列化实体。 */
@Serializable
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** 1 = 已归档；0 = 正常。用 Int 而非 Boolean，便于后续直接用 SQL 统计。 */
    val archived: Int = 0,
    /** 会话独立系统提示词；null/空串表示使用全局默认。（旧字段，v9 起由 userPrompt 取代） */
    val systemPrompt: String? = null,
    /** 当前会话使用的模式 ID。 */
    val modeId: String = "custom",
    /** 用户自定义的第二层提示词内容。 */
    val userPrompt: String? = null,
    /** 实际生效的温度值。 */
    val effectiveTemperature: Float? = null,
    /** 实际生效的 top_p 值。 */
    val effectiveTopP: Float? = null,
    /** 24 轮：1 = 置顶会话（侧边栏固定组）。 */
    val pinned: Int = 0,
    /** 24 轮：1 = 标题仍为自动生成；用户手动改名后置 0。 */
    val titleAuto: Int = 1,
)
