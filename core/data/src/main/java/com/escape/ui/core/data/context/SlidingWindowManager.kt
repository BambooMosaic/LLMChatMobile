package com.escape.ui.core.data.context

import com.escape.ui.core.domain.ChatMessage
import com.escape.ui.core.domain.ContextWindow
import com.escape.ui.core.domain.ContextWindowRepository
import com.escape.ui.core.domain.MessageRole
import com.escape.ui.core.data.settings.ContextWindowStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 滑动窗口管理器：纯本地估算与裁剪，不调用 LLM。
 * 从最旧消息开始移除，直到回到安全线以内；保留 system 消息（存在时）。
 */
class SlidingWindowManager @Inject constructor(
    private val store: ContextWindowStore,
) : ContextWindowRepository {

    override fun observeWindow(): Flow<ContextWindow> = store.window

    override fun estimateTokens(messages: List<ChatMessage>): Int {
        val chars = messages.sumOf { it.content.length }
        return kotlin.math.ceil(chars / CHARS_PER_TOKEN).toInt()
    }

    override fun compress(messages: List<ChatMessage>, window: ContextWindow): List<ChatMessage> {
        if (estimateTokens(messages) <= window.safeLine) return messages

        val system = messages.firstOrNull { it.role == MessageRole.SYSTEM }
        val rest = messages.filter { it.role != MessageRole.SYSTEM }.toMutableList()

        // 从头部（最旧）逐个移除，直到回到安全线以内
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
