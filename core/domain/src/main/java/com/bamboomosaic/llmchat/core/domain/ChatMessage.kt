package com.bamboomosaic.llmchat.core.domain

enum class MessageRole { SYSTEM, USER, ASSISTANT }

data class ChatMessage(
    val role: MessageRole,
    val content: String,
)
