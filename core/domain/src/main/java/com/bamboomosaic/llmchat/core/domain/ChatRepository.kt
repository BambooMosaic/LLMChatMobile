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

    /**
     * 19 轮新增：DeepSeek-Flash 搜索代理。
     * 用指定（DeepSeek）配置执行一次联网搜索问答，返回回答全文（含引用）；
     * 网页条数经 [onPages] 增量回调；本轮 usage 写入统计表（sessionId 归集）。
     * 失败抛异常，由上层降级处理（不阻断对话）。
     */
    suspend fun searchProxy(
        config: ApiConfig,
        question: String,
        sessionId: String,
        onPages: (Int) -> Unit = {},
    ): String

    /**
     * 20 轮：查询最新发布（不含 prerelease）的安卓 APK 下载直链。
     * 无网络或解析失败返回 null，由上层回退浏览器打开 Releases 页。
     */
    suspend fun latestApkDownloadUrl(): String?
}
