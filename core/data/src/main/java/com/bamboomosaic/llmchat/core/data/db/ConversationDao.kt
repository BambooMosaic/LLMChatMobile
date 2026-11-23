package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    /** 侧边栏主列表：只看未归档。 */
    // 24 轮：置顶会话固定组优先，组内按最近更新倒序
    @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    /** 「恢复对话」列表：只看已归档。 */
    @Query("SELECT * FROM conversations WHERE archived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversation(entity: ConversationEntity)

    @Update
    suspend fun updateConversation(entity: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: String): ConversationEntity?

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateTitle(conversationId: String, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun touch(conversationId: String, updatedAt: Long)

    @Query("UPDATE conversations SET archived = :archived WHERE id = :conversationId")
    suspend fun setArchived(conversationId: String, archived: Int)

    /** 24 轮：置顶 / 取消置顶。 */
    @Query("UPDATE conversations SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Int)

    /** 24 轮：写标题并关闭自动标题标记（手动改名与自动生成都走这里）。 */
    @Query("UPDATE conversations SET title = :title, titleAuto = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTitle(id: String, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET systemPrompt = :prompt WHERE id = :conversationId")
    suspend fun setSystemPrompt(conversationId: String, prompt: String?)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessages(conversationId: String)

    /** 21 轮：手动压缩——删除会话中除最近 keep 条外的全部消息（不可逆）。 */
    @Query(
        """
        DELETE FROM messages WHERE conversationId = :conversationId AND id NOT IN (
            SELECT id FROM messages WHERE conversationId = :conversationId
            ORDER BY id DESC LIMIT :keep
        )
        """,
    )
    suspend fun deleteMessagesExceptLast(conversationId: String, keep: Int)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    /** 25 轮：已有消息的会话 id 集合（侧边栏「编辑提示词」入口的显隐依据）。 */
    @Query("SELECT DISTINCT conversationId FROM messages")
    fun observeConversationIdsWithMessages(): Flow<List<String>>

    @Insert
    suspend fun insertMessage(entity: MessageEntity): Long

    /** 指定助手消息的同组版本（同会话、同 versionKey、role=ASSISTANT），按版本升序。 */
    @Query(
        """
        SELECT m2.* FROM messages m2
        WHERE m2.conversationId = (SELECT conversationId FROM messages WHERE id = :messageId)
          AND m2.role = 'ASSISTANT'
          AND m2.versionKey = (SELECT versionKey FROM messages WHERE id = :messageId)
        ORDER BY m2.version ASC, m2.id ASC
        """,
    )
    suspend fun getSiblingVersions(messageId: Long): List<MessageEntity>

    /** 按版本组键取同组全部助手消息（算下一版本号用）。 */
    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId AND versionKey = :versionKey AND role = 'ASSISTANT'
        """,
    )
    suspend fun getSiblingVersionsByGroup(conversationId: String, versionKey: Long): List<MessageEntity>

    /** 同组版本数（显示 1/N 用）。 */
    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE conversationId = :conversationId AND versionKey = :versionKey AND role = 'ASSISTANT'
        """,
    )
    suspend fun countVersions(conversationId: String, versionKey: Long): Int

    /** 某会话最近一条 token 用量记录 id（助手消息 usageId 自动关联用）。 */
    @Query(
        "SELECT id FROM token_usage_records WHERE sessionId = :conversationId " +
            "ORDER BY id DESC LIMIT 1",
    )
    suspend fun getLastUsageId(conversationId: String): Long?

    /** 按 versionKey 取单条消息（多版本切换时定位）。 */
    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): MessageEntity?
}
