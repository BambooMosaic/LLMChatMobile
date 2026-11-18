package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** 1 = 已归档；0 = 正常。用 Int 而非 Boolean，便于后续直接用 SQL 统计。 */
    val archived: Int = 0,
)
