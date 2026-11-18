package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

/** 一个对话会话（侧边栏列表项）。 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 会话与消息的持久化仓储，ViewModel 只注入此接口。 */
interface ConversationRepository {

    /** 会话列表，按最近更新时间倒序。 */
    fun observeConversations(): Flow<List<Conversation>>

    /** 会话不存在则创建，返回会话 id。标题默认取首条用户消息。 */
    suspend fun ensureConversation(conversationId: String, title: String)

    /** 追加一条消息到指定会话。 */
    suspend fun appendMessage(conversationId: String, message: ChatMessage)

    /** 读取某会话的全部历史消息。 */
    suspend fun getMessages(conversationId: String): List<ChatMessage>

    /** 如果标题仍是占位标题，用首条消息更新。 */
    suspend fun updateTitleIfPlaceholder(conversationId: String, title: String)

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
}
