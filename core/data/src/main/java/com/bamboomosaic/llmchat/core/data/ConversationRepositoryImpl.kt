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
        appendVersionedReply(conversationId, message, versionKey = message.versionKey ?: 0L)
    }

    override suspend fun appendVersionedReply(
        conversationId: String,
        message: ChatMessage,
        versionKey: Long,
    ): ChatMessage {
        val now = System.currentTimeMillis()
        // 版本号 = 同组内最大 version + 1（新组从 1 开始）
        val nextVersion = if (versionKey > 0L) {
            (dao.getSiblingVersionsByGroup(conversationId, versionKey).maxOfOrNull { it.version } ?: 0) + 1
        } else {
            1
        }
        // 助手回复未显式指定 usageId 时自动关联该会话最近一条用量记录
        val effectiveUsageId = message.usageId
            ?: if (message.role == MessageRole.ASSISTANT) dao.getLastUsageId(conversationId) else null
        val entityId = dao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = message.role.name,
                content = message.content,
                timestamp = now,
                reasoning = message.reasoning?.takeIf { it.isNotBlank() },
                images = message.images.takeIf { it.isNotEmpty() }?.joinToString(IMAGE_SEPARATOR),
                searchPages = message.searchPages,
                version = if (message.role == MessageRole.USER) 1 else nextVersion,
                versionKey = if (message.role == MessageRole.USER) null else versionKey.takeIf { it > 0 },
                usageId = effectiveUsageId,
            ),
        )
        dao.touch(conversationId, now)
        return message.copy(id = entityId, version = if (message.role == MessageRole.USER) 1 else nextVersion)
    }

    override suspend fun getSiblingVersions(messageId: Long): List<ChatMessage> =
        dao.getSiblingVersions(messageId).map { it.toDomain() }

    override suspend fun getMessageById(messageId: Long): ChatMessage? =
        dao.getMessageById(messageId)?.toDomain()

    override suspend fun getConversationById(conversationId: String): Conversation? =
        dao.getConversation(conversationId)?.toDomain()

    override suspend fun countVersions(conversationId: String, versionKey: Long): Int =
        dao.countVersions(conversationId, versionKey)

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        dao.getMessages(conversationId).map { it.toDomain() }

    override suspend fun updateTitleIfPlaceholder(conversationId: String, title: String) {
        val current = dao.getConversation(conversationId) ?: return
        if (current.title == PLACEHOLDER_TITLE || current.title == "新对话" || current.title.isBlank()) {
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

    override suspend fun ensureConversationWithMode(
        conversationId: String,
        title: String,
        modeId: String,
        userPrompt: String?,
        temperature: Float?,
        topP: Float?,
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.getConversation(conversationId)
        if (existing != null) return
        dao.insertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                createdAt = now,
                updatedAt = now,
                modeId = modeId,
                userPrompt = userPrompt?.takeIf { it.isNotBlank() },
                effectiveTemperature = temperature,
                effectiveTopP = topP,
            ),
        )
    }

    override suspend fun updateConversationMode(
        conversationId: String,
        modeId: String,
        userPrompt: String?,
        temperature: Float?,
        topP: Float?,
    ) {
        // 会话不存在（新建后未发消息）时先创建占位记录，避免静默丢失修改
        val entity = dao.getConversation(conversationId)
            ?: ConversationEntity(
                id = conversationId,
                title = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        dao.updateConversation(
            entity.copy(
                modeId = modeId,
                userPrompt = userPrompt?.takeIf { it.isNotBlank() },
                effectiveTemperature = temperature,
                effectiveTopP = topP,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archived = archived == 1,
        systemPrompt = systemPrompt,
        modeId = modeId,
        userPrompt = userPrompt,
        effectiveTemperature = effectiveTemperature,
        effectiveTopP = effectiveTopP,
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.USER),
        content = content,
        reasoning = reasoning,
        images = images?.split(IMAGE_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList(),
        searchPages = searchPages,
        id = id,
        version = version,
        versionKey = versionKey,
        usageId = usageId,
    )

    companion object {
        const val PLACEHOLDER_TITLE = "新对话"
        const val IMAGE_SEPARATOR = "\u0001"
    }
}
