package com.escape.ui.core.data

import com.escape.ui.core.data.db.ConversationDao
import com.escape.ui.core.data.db.ConversationEntity
import com.escape.ui.core.data.db.MessageEntity
import com.escape.ui.core.domain.ChatMessage
import com.escape.ui.core.domain.Conversation
import com.escape.ui.core.domain.ConversationRepository
import com.escape.ui.core.domain.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationDao,
) : ConversationRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        dao.observeConversations().map { list ->
            list.map { Conversation(it.id, it.title, it.createdAt, it.updatedAt) }
        }

    override suspend fun ensureConversation(conversationId: String, title: String) {
        val now = System.currentTimeMillis()
        dao.insertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun appendMessage(conversationId: String, message: ChatMessage) {
        val now = System.currentTimeMillis()
        dao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = message.role.name,
                content = message.content,
                timestamp = now,
            ),
        )
        dao.touch(conversationId, now)
    }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        dao.getMessages(conversationId).map { it.toDomain() }

    override suspend fun updateTitleIfPlaceholder(conversationId: String, title: String) {
        val current = dao.getConversation(conversationId) ?: return
        if (current.title == PLACEHOLDER_TITLE || current.title.isBlank()) {
            dao.updateTitle(conversationId, title, System.currentTimeMillis())
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    private fun MessageEntity.toDomain() = ChatMessage(
        role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.USER),
        content = content,
    )

    companion object {
        const val PLACEHOLDER_TITLE = "新对话"
    }
}
