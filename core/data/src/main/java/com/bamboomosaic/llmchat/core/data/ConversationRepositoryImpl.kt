package com.bamboomosaic.llmchat.core.data

import com.bamboomosaic.llmchat.core.data.db.ConversationDao
import com.bamboomosaic.llmchat.core.data.db.ConversationEntity
import com.bamboomosaic.llmchat.core.data.db.MessageEntity
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.ConversationRepository
import com.bamboomosaic.llmchat.core.domain.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationDao,
) : ConversationRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        dao.observeConversations().map { list -> list.map { it.toDomain() } }

    override fun observeArchivedConversations(): Flow<List<Conversation>> =
        dao.observeArchivedConversations().map { list -> list.map { it.toDomain() } }

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
                reasoning = message.reasoning?.takeIf { it.isNotBlank() },
                images = message.images.takeIf { it.isNotEmpty() }?.joinToString(IMAGE_SEPARATOR),
                searchPages = message.searchPages,
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

    override suspend fun renameConversation(conversationId: String, title: String) {
        dao.updateTitle(conversationId, title.trim(), System.currentTimeMillis())
    }

    override suspend fun setArchived(conversationId: String, archived: Boolean) {
        dao.setArchived(conversationId, if (archived) 1 else 0)
    }

    override suspend fun deleteConversation(conversationId: String) {
        // 先删消息再删会话，避免残留孤儿消息
        dao.deleteMessages(conversationId)
        dao.deleteConversation(conversationId)
    }

    override suspend fun setSystemPrompt(conversationId: String, prompt: String?) {
        dao.setSystemPrompt(conversationId, prompt?.takeIf { it.isNotBlank() })
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archived = archived == 1,
        systemPrompt = systemPrompt,
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.USER),
        content = content,
        reasoning = reasoning,
        images = images?.split(IMAGE_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList(),
        searchPages = searchPages,
    )

    companion object {
        const val PLACEHOLDER_TITLE = "新对话"
        const val IMAGE_SEPARATOR = "\u0001"
    }
}
