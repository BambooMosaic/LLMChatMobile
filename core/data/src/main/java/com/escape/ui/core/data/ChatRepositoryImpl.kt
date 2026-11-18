package com.escape.ui.core.data

import com.escape.ui.core.data.db.TokenUsageDao
import com.escape.ui.core.data.db.TokenUsageRecordEntity
import com.escape.ui.core.domain.ChatMessage
import com.escape.ui.core.domain.ChatRepository
import com.escape.ui.core.domain.SettingsRepository
import com.escape.ui.core.network.OpenAiSseClient
import com.escape.ui.core.network.SseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val sseClient: OpenAiSseClient,
    private val tokenUsageDao: TokenUsageDao,
    private val settingsRepository: SettingsRepository,
) : ChatRepository {

    override fun sendChatStream(
        sessionId: String,
        messages: List<ChatMessage>,
        model: String,
    ): Flow<String> = flow {
        // 每次请求都从 DataStore 读取最新配置，不在代码里写死 Key
        val settings = settingsRepository.observeSettings().first()
        emitAll(
            sseClient.stream(
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = model,
                messages = messages,
            ).transform { event ->
                when (event) {
                    is SseEvent.Text -> emit(event.chunk)
                    is SseEvent.Usage -> {
                        // data 层内部拦截 usage 写入 Room，不暴露给 UI 接口
                        tokenUsageDao.insert(
                            TokenUsageRecordEntity(
                                sessionId = sessionId,
                                model = model,
                                promptTokens = event.promptTokens,
                                completionTokens = event.completionTokens,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
            },
        )
    }
}
