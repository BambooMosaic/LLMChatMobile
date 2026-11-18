package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    /** 侧边栏主列表：只看未归档。 */
    @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    /** 「恢复对话」列表：只看已归档。 */
    @Query("SELECT * FROM conversations WHERE archived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversation(entity: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: String): ConversationEntity?

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateTitle(conversationId: String, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun touch(conversationId: String, updatedAt: Long)

    @Query("UPDATE conversations SET archived = :archived WHERE id = :conversationId")
    suspend fun setArchived(conversationId: String, archived: Int)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    @Insert
    suspend fun insertMessage(entity: MessageEntity)
}
