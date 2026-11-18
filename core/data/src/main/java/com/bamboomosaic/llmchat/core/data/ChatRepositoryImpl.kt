package com.bamboomosaic.llmchat.core.data

import com.bamboomosaic.llmchat.core.data.db.TokenUsageDao
import com.bamboomosaic.llmchat.core.data.db.TokenUsageRecordEntity
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.network.OpenAiSseClient
import com.bamboomosaic.llmchat.core.network.SseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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
            ).transform { event ->
                when (event) {
                    is SseEvent.Text -> emit(event.chunk)
                    is SseEvent.Usage -> {
                        // data 层内部拦截 usage 写入 Room，不暴露给 UI 接口
                        tokenUsageDao.insert(
                            TokenUsageRecordEntity(
                                sessionId = sessionId,
                                model = effectiveModel,
                                promptTokens = event.promptTokens,
                                completionTokens = event.completionTokens,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }
                    // 思考内容通过回调单独送出，不混进正文流
                    is SseEvent.Reasoning -> onReasoning(event.chunk)
                }
            },
        )
    }
}
