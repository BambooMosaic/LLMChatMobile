package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * 发送对话请求，以流式返回助手回复的文本片段。
     * [sessionId] 由 UI 层生成并传入，用于 token 用量归集。
     * [messages] 为完整历史（含 system），[model] 为模型名。
     * [onReasoning] 推理模型（如 deepseek-flash）的思考内容回调，与正文流分开传递。
     * [onSearchPages] 联网搜索回调：每次搜索类工具调用完成时回调本次返回的网页条数（增量）。
     */
    fun sendChatStream(
        sessionId: String,
        messages: List<ChatMessage>,
        model: String,
        thinkingEnabled: Boolean = false,
        maxTokens: Int? = null,
        onReasoning: (String) -> Unit = {},
        searchEnabled: Boolean = false,
        onSearchPages: (Int) -> Unit = {},
        temperature: Float? = null,
        topP: Float? = null,
    ): Flow<String>

    /**
     * 连通性校验：用给定配置发一条 hello（极短输出），不写会话/token 记录。
     * 成功返回 Ok，失败返回具体错误（密钥错误/网络异常等由上层转成友好文案）。
     */
    suspend fun validateConfig(config: ApiConfig): Result<Unit>
}
