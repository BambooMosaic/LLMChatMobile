package com.bamboomosaic.llmchat.core.data

import com.bamboomosaic.llmchat.core.data.db.ApiConfigDao
import com.bamboomosaic.llmchat.core.data.db.ApiConfigEntity
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConfigRepositoryImpl @Inject constructor(
    private val dao: ApiConfigDao,
    private val settingsRepository: SettingsRepository,
) : ConfigRepository {

    override fun observeConfigs(): Flow<List<ApiConfig>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCurrentConfig(): Flow<ApiConfig?> = combine(
        dao.observeAll(),
        settingsRepository.observeCurrentConfigId(),
    ) { list, selectedId ->
        pickCurrent(list, selectedId)?.toDomain()
    }

    override suspend fun getCurrentConfig(): ApiConfig? {
        val list = dao.observeAll().first()
        val selectedId = settingsRepository.observeCurrentConfigId().first()
        return pickCurrent(list, selectedId)?.toDomain()
    }

    override suspend fun save(config: ApiConfig): Long {
        val entity = config.toEntity()
        return if (config.id == 0L) {
            // 第一条配置自动成为默认
            val isFirst = dao.count() == 0
            val newId = dao.insert(entity.copy(isDefault = if (isFirst) 1 else entity.isDefault))
            if (isFirst) settingsRepository.setCurrentConfigId(newId)
            newId
        } else {
            dao.update(entity)
            if (entity.isDefault == 1) settingsRepository.setCurrentConfigId(config.id)
            config.id
        }
    }

    override suspend fun delete(id: Long) {
        val target = dao.getById(id) ?: return
        // 至少保留一条
        if (dao.count() <= 1) return
        dao.delete(id)
        if (target.isDefault == 1) {
            dao.getFirst()?.let { dao.setDefault(it.id) }
        }
        // 删掉的是当前选中的配置时，回落到默认配置
        if (settingsRepository.observeCurrentConfigId().first() == id) {
            val fallback = dao.getDefault() ?: dao.getFirst()
            settingsRepository.setCurrentConfigId(fallback?.id ?: 0L)
        }
    }

    override suspend fun setDefault(id: Long) {
        dao.setDefault(id)
        settingsRepository.setCurrentConfigId(id)
    }

    override suspend fun select(id: Long) {
        settingsRepository.setCurrentConfigId(id)
    }

    override suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        // 把升级前 DataStore 里的单条配置迁移为第一条默认配置
        val legacy = settingsRepository.observeSettings().first()
        val providerId = legacy.providerId.ifBlank { "deepseek" }
        val preset = Providers.find(providerId)
        val name = if (providerId == Providers.CUSTOM) {
            legacy.model.ifBlank { "默认配置" }
        } else {
            preset.displayName
        }
        val supportsReasoning = preset.supportReasoning ||
            legacy.model.contains("reasoner", ignoreCase = true) ||
            legacy.model.contains("thinking", ignoreCase = true)
        val newId = dao.insert(
            ApiConfigEntity(
                name = name,
                provider = providerId,
                baseUrl = legacy.baseUrl,
                apiKey = legacy.apiKey,
                model = legacy.model,
                supportReasoning = if (supportsReasoning) 1 else 0,
                isDefault = 1,
            ),
        )
        settingsRepository.setCurrentConfigId(newId)
    }

    /** 选中 id → 默认配置 → 第一条，三级兜底。 */
    private fun pickCurrent(list: List<ApiConfigEntity>, selectedId: Long): ApiConfigEntity? =
        list.firstOrNull { it.id == selectedId }
            ?: list.firstOrNull { it.isDefault == 1 }
            ?: list.firstOrNull()

    private fun ApiConfigEntity.toDomain() = ApiConfig(
        id = id,
        name = name,
        provider = provider,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        supportReasoning = supportReasoning == 1,
        isDefault = isDefault == 1,
        maxContextTokens = maxContextTokens,
        maxOutputTokens = maxOutputTokens,
    )

    private fun ApiConfig.toEntity() = ApiConfigEntity(
        id = id,
        name = name,
        provider = provider,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        supportReasoning = if (supportReasoning) 1 else 0,
        isDefault = if (isDefault) 1 else 0,
        maxContextTokens = maxContextTokens,
        maxOutputTokens = maxOutputTokens,
    )
}
