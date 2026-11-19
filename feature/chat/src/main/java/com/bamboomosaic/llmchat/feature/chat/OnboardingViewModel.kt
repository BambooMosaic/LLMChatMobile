package com.bamboomosaic.llmchat.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 引导流程状态：验证中 / 成功 / 失败。 */
sealed interface ValidateState {
    data object Idle : ValidateState
    data object Validating : ValidateState
    data object Success : ValidateState
    data class Error(val message: String) : ValidateState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val configRepository: ConfigRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _validateState = MutableStateFlow<ValidateState>(ValidateState.Idle)
    val validateState: StateFlow<ValidateState> = _validateState.asStateFlow()

    private val _configured = MutableStateFlow(false)

    /** 每次验证完成（成功或失败）自增，驱动 Toast 重复弹出。 */
    private val _toastTick = MutableStateFlow(0)
    val toastTick: StateFlow<Int> = _toastTick.asStateFlow()

    /** 当前是否已验证通过（决定「下一步」是否可点）。 */
    val isConfigured: StateFlow<Boolean> = _configured.asStateFlow()

    fun resetValidation() {
        _validateState.value = ValidateState.Idle
    }

    /** 校验配置：成功则保存为默认配置并解锁下一步。 */
    fun validateAndSave(config: ApiConfig) {
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            _validateState.value = ValidateState.Error("请填写完整配置")
            _toastTick.value++
            return
        }
        viewModelScope.launch {
            _validateState.value = ValidateState.Validating
            val result = runCatching { chatRepository.validateConfig(config) }
                .getOrElse { Result.failure(it) }
            if (result.isSuccess) {
                val id = configRepository.save(config.copy(isDefault = true))
                configRepository.setDefault(id)
                configRepository.select(id)
                _configured.value = true
                _validateState.value = ValidateState.Success
                _toastTick.value++
            } else {
                val e = result.exceptionOrNull()
                _validateState.value = ValidateState.Error(
                    e?.message?.takeIf { it.isNotBlank() } ?: "请求失败，请检查配置"
                )
                _toastTick.value++
            }
        }
    }

    /** 标记引导完成（与首次启动标记共用）。 */
    fun finish() {
        viewModelScope.launch { settingsRepository.setFirstLaunchDone() }
    }

    /** 构造 DeepSeek 快捷配置。 */
    fun deepSeekConfig(apiKey: String): ApiConfig {
        val preset = Providers.find("deepseek")
        return ApiConfig(
            name = preset.displayName,
            provider = "deepseek",
            baseUrl = preset.baseUrl,
            apiKey = apiKey.trim(),
            model = "deepseek-flash",
            supportReasoning = true,
        )
    }
}
