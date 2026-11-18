package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    /** 推理模型的思考内容，仅助手消息可能有值，用于历史会话回看。 */
    val reasoning: String? = null,
    /** 多模态图片 base64，用 \u0001 分隔多张；仅用户消息可能有值。 */
    val images: String? = null,
)
