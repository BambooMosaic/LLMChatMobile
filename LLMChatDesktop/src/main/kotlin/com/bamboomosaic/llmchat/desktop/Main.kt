package com.bamboomosaic.llmchat.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import com.bamboomosaic.llmchat.feature.chat.ChatScreen
import com.bamboomosaic.llmchat.feature.chat.ChatViewModel
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import com.bamboomosaic.llmchat.feature.chat.DesktopToast
import com.bamboomosaic.llmchat.feature.chat.OnboardingScreen
import com.bamboomosaic.llmchat.feature.chat.OnboardingViewModel
import com.bamboomosaic.llmchat.feature.chat.SplashScreen
import com.bamboomosaic.llmchat.feature.chat.UpdateNoticeDialog
import com.bamboomosaic.llmchat.feature.chat.i18n.stringsFor
import com.bamboomosaic.llmchat.feature.chat.theme.LLMChatTheme
import kotlinx.coroutines.launch

/**
 * LLMChatDesktop 桌面端入口（全量移植轮）：
 * 与手机端 MainActivity 同构——Splash → Onboarding（首启）→ ChatScreen；
 * 全量版 UI（ChatScreen/SettingsDialog/模式编辑器/Token 统计/导入导出/TTS）来自
 * feature:chat 的复制改造版（本工程 feature.chat 包），手机端源码零改动。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LLMChat Desktop",
        state = rememberWindowState(width = 1150.dp, height = 800.dp),
    ) {
        DesktopAppRoot()
    }
}

@Composable
private fun DesktopAppRoot() {
    val settings by DesktopContainer.settingsRepository.observeSettings()
        .collectAsState(initial = AppSettings(systemPrompt = "", model = "", baseUrl = "", apiKey = ""))
    var showSplash by remember { mutableStateOf(true) }
    val isFirstLaunch by DesktopContainer.settingsRepository.observeFirstLaunch()
        .collectAsState(initial = true)
    val majorVersionChanged by DesktopContainer.settingsRepository.observeMajorVersionChanged()
        .collectAsState(initial = false)

    val chatViewModel = remember {
        ChatViewModel(
            chatRepository = DesktopContainer.chatRepository,
            contextWindowRepository = DesktopContainer.contextWindowRepository,
            settingsRepository = DesktopContainer.settingsRepository,
            contextReminderProvider = DesktopContainer.contextReminderProvider,
            conversationRepository = DesktopContainer.conversationRepository,
            tokenUsageRepository = DesktopContainer.tokenUsageRepository,
            configRepository = DesktopContainer.configRepository,
            dataDir = DesktopContainer.dataDir,
            ttsClient = DesktopContainer.deepSeekTtsClient,
            qwenTtsClient = DesktopContainer.qwenTtsClient,
            glmTtsClient = DesktopContainer.glmTtsClient,
            backupManager = DesktopContainer.backupManager,
        )
    }
    val onboardingViewModel = remember {
        OnboardingViewModel(
            chatRepository = DesktopContainer.chatRepository,
            configRepository = DesktopContainer.configRepository,
            settingsRepository = DesktopContainer.settingsRepository,
        )
    }

    // 桌面端主题：深色/浅色跟随设置，SYSTEM 暂按深色处理（TODO 跟随系统）
    val isDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        else -> true
    }

    com.bamboomosaic.llmchat.feature.chat.i18n.ProvideAppStrings(
        language = settings.language,
        glassLevel = settings.glassLevel,
    ) {
        LLMChatTheme(darkTheme = isDark, colorPreset = settings.colorPreset) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    showSplash -> SplashScreen(onFinished = { showSplash = false })
                    isFirstLaunch -> OnboardingScreen(
                        themeMode = settings.themeMode,
                        language = settings.language,
                        onFinish = { /* 标记已在 ViewModel 内写入，这里等待 Flow 刷新 */ },
                        viewModel = onboardingViewModel,
                    )
                    else -> ChatScreen(viewModel = chatViewModel)
                }
                DesktopToast.Host()
                if (majorVersionChanged) {
                    UpdateNoticeDialog(
                        versionName = com.bamboomosaic.llmchat.core.data.settings.appVersionName(),
                        onDismiss = {
                            DesktopContainer.appScope.launch {
                                DesktopContainer.settingsRepository.markMajorVersionNoticeSeen()
                            }
                        },
                    )
                }
            }
        }
    }
    // 开屏播放约 1 秒（与手机端 SplashScreen 内部动画时长一致）
    LaunchedEffect(Unit) { }
}
