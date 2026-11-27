package com.bamboomosaic.llmchat.core.data.context

import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ContextWindow
import com.bamboomosaic.llmchat.core.domain.ContextWindowRepository
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 滑动窗口管理器：纯本地估算与裁剪，不调用 LLM。
 * 最大上下文来自设置（DataStore）；从最旧消息开始移除，保留 system 消息。
 */
class SlidingWindowManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ContextWindowRepository {

    override fun observeWindow(): Flow<ContextWindow> =
        settingsRepository.observeSettings().map { ContextWindow(maxTokens = it.maxContextTokens) }

    override fun estimateTokens(messages: List<ChatMessage>): Int {
        val chars = messages.sumOf { it.content.length }
        return kotlin.math.ceil(chars / CHARS_PER_TOKEN).toInt()
    }

    override fun compress(messages: List<ChatMessage>, window: ContextWindow): List<ChatMessage> {
        if (estimateTokens(messages) <= window.safeLine) return messages

        val system = messages.firstOrNull { it.role == MessageRole.SYSTEM }
        val rest = messages.filter { it.role != MessageRole.SYSTEM }.toMutableList()

        while (rest.isNotEmpty() && estimateTokens(buildList { system?.let(::add); addAll(rest) }) > window.safeLine) {
            rest.removeAt(0)
        }

        return buildList {
            system?.let(::add)
            addAll(rest)
        }
    }

    private companion object {
        const val CHARS_PER_TOKEN = 4.0
    }
}
