package com.bamboomosaic.llmchat.core.data

import com.bamboomosaic.llmchat.core.data.db.TokenUsageDao
import com.bamboomosaic.llmchat.core.data.db.TokenUsageRecordEntity
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.core.network.OpenAiSseClient
import com.bamboomosaic.llmchat.core.network.SseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val sseClient: OpenAiSseClient,
    private val tokenUsageDao: TokenUsageDao,
    private val configRepository: ConfigRepository,
) : ChatRepository {

    override fun sendChatStream(
        sessionId: String,
        messages: List<ChatMessage>,
        model: String,
        thinkingEnabled: Boolean,
        maxTokens: Int?,
        onReasoning: (String) -> Unit,
        searchEnabled: Boolean,
        onSearchPages: (Int) -> Unit,
        temperature: Float?,
        topP: Float?,
    ): Flow<String> = flow {
        // 从「当前选中的 API 配置」读取 URL/Key，不再读单条固定 DataStore 配置
        val config = configRepository.getCurrentConfig()
        val baseUrl = config?.baseUrl.orEmpty()
        val apiKey = config?.apiKey.orEmpty()
        // model 优先用当前配置里的，调用方传入的仅作兜底
        val effectiveModel = config?.model?.takeIf { it.isNotBlank() } ?: model
        emitAll(
            sseClient.stream(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = effectiveModel,
                messages = messages,
                thinkingEnabled = thinkingEnabled,
                maxTokens = maxTokens,
                searchEnabled = searchEnabled,
                temperature = temperature,
                topP = topP,
            ).transform { event ->
                when (event) {
                    is SseEvent.Text -> emit(event.chunk)
                    is SseEvent.SearchStarted -> onSearchPages(0)
                    is SseEvent.SearchCompleted -> onSearchPages(event.pageCount)
                    is SseEvent.Usage -> {
                        // data 层内部拦截 usage 写入 Room，不暴露给 UI 接口
                        tokenUsageDao.insert(
                            TokenUsageRecordEntity(
                                sessionId = sessionId,
                                model = effectiveModel,
                                promptTokens = event.promptTokens,
                                completionTokens = event.completionTokens,
                                timestamp = System.currentTimeMillis(),
                                cacheHitTokens = event.cacheHitTokens,
                                reasoningTokens = event.reasoningTokens,
                            ),
                        )
                    }
                    // 思考内容通过回调单独送出，不混进正文流
                    is SseEvent.Reasoning -> onReasoning(event.chunk)
                }
            },
        )
    }

    override suspend fun validateConfig(config: ApiConfig): Result<Unit> = runCatching {
        // 用表单里的配置直接发 hello，极短输出；只取第一个事件确认连通，不写任何库
        sseClient.stream(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model,
            messages = listOf(ChatMessage(MessageRole.USER, "hello")),
            thinkingEnabled = false,
            maxTokens = 8,
        ).firstOrNull()
        Unit
    }

    override suspend fun searchProxy(
        config: ApiConfig,
        question: String,
        sessionId: String,
        onPages: (Int) -> Unit,
    ): String {
        // 模型优先用该 DeepSeek 配置自己的模型；非 deepseek 系模型名兜底为 deepseek-flash
        val model = config.model.takeIf { it.startsWith("deepseek", ignoreCase = true) }
            ?: SEARCH_PROXY_MODEL
        val answer = StringBuilder()
        sseClient.stream(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = model,
            messages = listOf(
                ChatMessage(MessageRole.SYSTEM, SEARCH_SYSTEM_PROMPT),
                ChatMessage(MessageRole.USER, question),
            ),
            searchEnabled = true,
        ).collect { event ->
            when (event) {
                is SseEvent.Text -> answer.append(event.chunk)
                is SseEvent.SearchStarted -> onPages(0)
                is SseEvent.SearchCompleted -> onPages(event.pageCount)
                is SseEvent.Usage -> tokenUsageDao.insert(
                    TokenUsageRecordEntity(
                        sessionId = sessionId,
                        model = model,
                        promptTokens = event.promptTokens,
                        completionTokens = event.completionTokens,
                        timestamp = System.currentTimeMillis(),
                        cacheHitTokens = event.cacheHitTokens,
                        reasoningTokens = event.reasoningTokens,
                        // 搜索代理轮次打标，供上下文面板分项统计联网搜索 token
                        isSearch = true,
                    ),
                )
                is SseEvent.Reasoning -> Unit
            }
        }
        return answer.toString()
    }

    override suspend fun latestApkDownloadUrl(): String? = kotlinx.coroutines.withContext(
        kotlinx.coroutines.Dispatchers.IO,
    ) {
        runCatching {
            // GitHub Releases Latest API：只返回正式版（prerelease 不参与），无需认证
            // 用 JDK HttpURLConnection：core:data 不直接依赖 OkHttp，避免穿透网络模块边界
            val conn = java.net.URL(LATEST_RELEASE_API)
                .openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.inputStream.use { input ->
                val root = org.json.JSONObject(input.bufferedReader().readText())
                val assets = root.optJSONArray("assets") ?: return@runCatching null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith("_android_release.apk")) {
                        return@runCatching asset.optString("browser_download_url")
                            .takeIf { it.isNotBlank() }
                    }
                }
                null
            }
        }.getOrNull()
    }

    private companion object {
        /** 搜索代理固定指令：要求基于联网搜索结果作答并标注来源。 */
        const val SEARCH_SYSTEM_PROMPT =
            "你是联网搜索助手。请先联网搜索，再基于搜索结果回答用户的问题：" +
                "优先给出现状、数据与结论，并标注信息来源（网站名称或链接）；" +
                "如果搜索结果不足以回答，请如实说明。回答保持简洁。"

        /** 搜索代理兜底模型（任务约定固定使用 DeepSeek-Flash）。 */
        const val SEARCH_PROXY_MODEL = "deepseek-flash"

        /** 最新发布版查询端点（Latest 不含 prerelease）。 */
        const val LATEST_RELEASE_API =
            "https://api.github.com/repos/BambooMosaic/LLMChatMobile/releases/latest"
    }
}
