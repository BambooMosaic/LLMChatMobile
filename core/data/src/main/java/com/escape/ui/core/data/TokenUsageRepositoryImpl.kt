package com.escape.ui.core.data

import com.escape.ui.core.data.db.TokenUsageDao
import com.escape.ui.core.domain.ModelTokenStats
import com.escape.ui.core.domain.TokenUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TokenUsageRepositoryImpl @Inject constructor(
    private val dao: TokenUsageDao,
) : TokenUsageRepository {

    override fun observeSessionTotal(sessionId: String): Flow<Long> =
        dao.observeSessionTotal(sessionId)

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
}
