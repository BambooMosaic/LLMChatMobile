package com.escape.ui.core.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * 发送对话请求，以流式返回助手回复的文本片段。
     * [sessionId] 由 UI 层生成并传入，用于 token 用量归集。
     * [messages] 为完整历史（含 system），[model] 为模型名。
     */
    fun sendChatStream(
        sessionId: String,
        messages: List<ChatMessage>,
        model: String,
    ): Flow<String>
}
