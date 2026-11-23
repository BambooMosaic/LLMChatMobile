package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

/** 一个对话会话（侧边栏列表项）。 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** 已归档的会话不出现在侧边栏主列表，只能在「恢复对话」里找回。 */
    val archived: Boolean = false,
    /** 会话独立系统提示词；null 表示使用全局默认。 */
    val systemPrompt: String? = null,
    /** 当前会话使用的模式 ID（双层提示词架构的模式层）。 */
    val modeId: String = ModeConfig.CUSTOM,
    /** 用户自定义的第二层提示词内容。 */
    val userPrompt: String? = null,
    /** 实际生效的温度值（冗余存储，便于统计）。 */
    val effectiveTemperature: Float? = null,
    /** 实际生效的 top_p 值。 */
    val effectiveTopP: Float? = null,
    /** 24 轮：置顶会话（侧边栏固定组展示，按置顶优先排序）。 */
    val pinned: Boolean = false,
    /** 24 轮：标题是否仍为自动生成（用户手动改名后为 false，不再自动覆盖）。 */
    val titleAuto: Boolean = true,
)

/** 会话与消息的持久化仓储，ViewModel 只注入此接口。 */
interface ConversationRepository {

    /** 未归档的会话列表，按最近更新时间倒序。 */
    fun observeConversations(): Flow<List<Conversation>>

    /** 25 轮：已有消息的会话 id 集合（用于隐藏「编辑提示词」入口）。 */
    fun observeConversationIdsWithMessages(): Flow<Set<String>>

    /** 已归档的会话列表，按最近更新时间倒序。 */
    fun observeArchivedConversations(): Flow<List<Conversation>>

    /** 会话不存在则创建，返回会话 id。标题默认取首条用户消息。 */
    suspend fun ensureConversation(conversationId: String, title: String)

    /** 追加一条消息到指定会话。 */
    suspend fun appendMessage(conversationId: String, message: ChatMessage)

    /** 读取某会话的全部历史消息。 */
    suspend fun getMessages(conversationId: String): List<ChatMessage>

    /** 如果标题仍是占位标题，用首条消息更新。 */
    suspend fun updateTitleIfPlaceholder(conversationId: String, title: String)

    /** 手动重命名会话标题。 */
    suspend fun renameConversation(conversationId: String, title: String)

    /** 24 轮：置顶 / 取消置顶。 */
    suspend fun setPinned(conversationId: String, pinned: Boolean)

    /** 24 轮：写入自动生成的标题（同时关闭自动标记，避免重复生成）。 */
    suspend fun setAutoTitle(conversationId: String, title: String)

    /** 归档 / 取消归档。 */
    suspend fun setArchived(conversationId: String, archived: Boolean)

    /** 永久删除会话及其全部消息（不可恢复）。 */
    suspend fun deleteConversation(conversationId: String)

    /** 设置/清除会话独立系统提示词（传 null 或空串回退全局默认）。 */
    suspend fun setSystemPrompt(conversationId: String, prompt: String?)

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /**
     * 追加多版本助手回复：version = 同组内最大 version + 1。
     * [versionKey] 为触发该回复的用户消息 id。
     */
    suspend fun appendVersionedReply(
        conversationId: String,
        message: ChatMessage,
        versionKey: Long,
    ): ChatMessage

    /** 取某条助手消息的全部兄弟版本（含自身），按版本升序。 */
    suspend fun getSiblingVersions(messageId: Long): List<ChatMessage>

    /** 按消息 id 取单条消息。 */
    suspend fun getMessageById(messageId: Long): ChatMessage?

    /** 按 id 读会话（一次性，绕过 Flow 缓存延迟）。 */
    suspend fun getConversationById(conversationId: String): Conversation?

    /** 同版本组的版本总数（显示 1/N 用）。 */
    suspend fun countVersions(conversationId: String, versionKey: Long): Int

    /**
     * 创建带模式配置的会话（已存在则忽略）。新对话创建弹窗用。
     * [userPrompt] 为用户层提示词；自定义模式的参数也可自定义。
     */
    suspend fun ensureConversationWithMode(
        conversationId: String,
        title: String,
        modeId: String,
        userPrompt: String?,
        temperature: Float?,
        topP: Float?,
    )

    /** 更新会话的模式配置（仅限未发送过消息的会话）。 */
    suspend fun updateConversationMode(
        conversationId: String,
        modeId: String,
        userPrompt: String?,
        temperature: Float?,
        topP: Float?,
    )

    /** 21 轮：手动压缩——删除会话中除最近 keep 条外的全部消息（不可逆，配合摘要使用）。 */
    suspend fun deleteMessagesExceptLast(conversationId: String, keep: Int)
}
