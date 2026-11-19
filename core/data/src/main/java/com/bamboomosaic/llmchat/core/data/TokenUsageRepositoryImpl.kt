package com.bamboomosaic.llmchat.core.data

import com.bamboomosaic.llmchat.core.data.db.DailyTokenRow
import com.bamboomosaic.llmchat.core.data.db.HourlyTokenRow
import com.bamboomosaic.llmchat.core.data.db.TokenTotalsRow
import com.bamboomosaic.llmchat.core.data.db.TokenUsageDao
import com.bamboomosaic.llmchat.core.domain.DailyTokenStats
import com.bamboomosaic.llmchat.core.domain.HourlyTokenStats
import com.bamboomosaic.llmchat.core.domain.ModelTokenStats
import com.bamboomosaic.llmchat.core.domain.TokenTotals
import com.bamboomosaic.llmchat.core.domain.TokenUsageRecord
import com.bamboomosaic.llmchat.core.domain.TokenUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TokenUsageRepositoryImpl @Inject constructor(
    private val dao: TokenUsageDao,
) : TokenUsageRepository {

    override fun observeSessionTotal(sessionId: String): Flow<Long> =
        dao.observeSessionTotal(sessionId)

    override fun observeSessionTotals(sessionId: String): Flow<TokenTotals> =
        dao.observeSessionTotals(sessionId).map { it.toDomain() }

    override fun observeGlobalTotals(): Flow<TokenTotals> =
        dao.observeGlobalTotals().map { it.toDomain() }

    override suspend fun getLastUsage(sessionId: String): TokenUsageRecord? =
        dao.getLastRecord(sessionId)?.let {
            TokenUsageRecord(
                sessionId = it.sessionId,
                model = it.model,
                promptTokens = it.promptTokens,
                completionTokens = it.completionTokens,
                timestamp = it.timestamp,
            )
        }

    override fun observeModelStats(): Flow<List<ModelTokenStats>> =
        dao.observeModelStats().map { list ->
            list.map { view ->
                ModelTokenStats(
                    model = view.model,
                    totalPromptTokens = view.totalPromptTokens,
                    totalCompletionTokens = view.totalCompletionTokens,
                    requestCount = view.requestCount,
                )
            }
        }

    override fun observeDailyTotals(since: Long): Flow<List<DailyTokenStats>> =
        dao.observeDailyTotals(since).map { list ->
            list.map { it.toDomain() }
        }

    override fun observeHourlyTotals(since: Long): Flow<List<HourlyTokenStats>> =
        dao.observeHourlyTotals(since).map { list ->
            list.map { it.toDomain() }
        }

    private fun DailyTokenRow.toDomain() = DailyTokenStats(date = day, tokens = tokens)

    private fun HourlyTokenRow.toDomain() = HourlyTokenStats(hour = hour, tokens = tokens)

    private fun TokenTotalsRow.toDomain() = TokenTotals(
        promptTokens = promptTokens,
        completionTokens = completionTokens,
    )
}
