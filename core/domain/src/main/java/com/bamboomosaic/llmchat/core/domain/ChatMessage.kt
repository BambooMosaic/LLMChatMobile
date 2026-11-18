package com.bamboomosaic.llmchat.core.domain

enum class MessageRole { SYSTEM, USER, ASSISTANT }

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    /** 推理模型的思考内容（reasoning_content）。只用于展示/持久化，不会回传给 API。 */
    val reasoning: String? = null,
)
