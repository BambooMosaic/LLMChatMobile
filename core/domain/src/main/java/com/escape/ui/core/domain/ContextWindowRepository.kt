package com.escape.ui.core.domain

import kotlinx.coroutines.flow.Flow

/** 上下文压缩：纯本地估算与裁剪，不调用任何 LLM。 */
interface ContextWindowRepository {
    /** 读取当前窗口配置。 */
    fun observeWindow(): Flow<ContextWindow>

    /**
     * 在发送请求前把历史压缩到安全线以内。
     * 从列表头部移除最旧消息，直到估算 token 回到 [window.safeLine] 以内。
     */
    fun compress(messages: List<ChatMessage>, window: ContextWindow): List<ChatMessage>

    /** 估算一组消息的 token 数（纯本地启发式）。 */
    fun estimateTokens(messages: List<ChatMessage>): Int
}
