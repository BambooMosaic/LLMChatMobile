package com.bamboomosaic.llmchat

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import com.bamboomosaic.llmchat.feature.chat.OnboardingScreen
import com.bamboomosaic.llmchat.feature.chat.SplashScreen
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import com.bamboomosaic.llmchat.feature.chat.i18n.stringsFor
import com.bamboomosaic.llmchat.feature.chat.theme.LLMChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = kotlinx.coroutines.MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.observeSettings().collectAsState(
                initial = AppSettings(
                    systemPrompt = AppSettings.defaultSystemPrompt(AppLanguage.SIMPLIFIED_CHINESE),
                    model = AppSettings.DEFAULT_MODEL,
                    baseUrl = AppSettings.DEFAULT_BASE_URL,
                    apiKey = AppSettings.DEFAULT_API_KEY,
                    // 占位值与默认主题一致（深色），避免首帧闪白
                    colorPreset = "dark_default",
                )
            )

            // 22 轮：明暗由配色预设的 light_/dark_ 前缀决定（浅色/深色模式开关已取消）
            val isDark = settings.colorPreset.startsWith("dark")

            // 状态栏图标颜色跟随明暗模式
            LaunchedEffect(isDark) {
                window.decorView.systemUiVisibility = if (isDark) {
                    0
                } else {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }

            // 27 轮：平板模式启动窗口标题为 LLMChatPro（仅窗口标题；应用内所有文案仍为 LLMChatMobile，手机模式不变）
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            LaunchedEffect(configuration.screenWidthDp) {
                title = if (configuration.screenWidthDp >= 600) "LLMChatPro" else "LLMChatMobile"
            }

            // 开屏动画（内存状态，每次冷启动都播放约 1 秒）
            var showSplash by remember { mutableStateOf(true) }

            // 首次启动标记（DataStore），引导完成后置 false
            val isFirstLaunch by settingsRepository.observeFirstLaunch().collectAsState(initial = true)
            // 27 轮修正：大版本变化 → 轻量更新公告窗口（不再重走首次引导）
            val majorVersionChanged by settingsRepository.observeMajorVersionChanged().collectAsState(initial = false)
            val splashVersionName = remember {
                runCatching {
                    packageManager.getPackageInfo(packageName, 0).versionName
                }.getOrNull() ?: ""
            }

            CompositionLocalProvider(
                LocalAppStrings provides stringsFor(settings.language),
                com.bamboomosaic.llmchat.feature.chat.LocalGlassLevel provides settings.glassLevel,
            ) {
                LLMChatTheme(darkTheme = isDark, colorPreset = settings.colorPreset) {
                    when {
                        showSplash -> SplashScreen(onFinished = { showSplash = false })
                        isFirstLaunch -> OnboardingScreen(
                            themeMode = settings.themeMode,
                            language = settings.language,
                            onFinish = { /* 标记已在 ViewModel 内写入，这里等待 Flow 刷新 */ },
                        )
                        else -> {
                            val navController = rememberNavController()
                            AppNavHost(navController = navController)
                            // 27 轮修正：大版本更新公告覆盖在主界面之上
                            if (majorVersionChanged) {
                                com.bamboomosaic.llmchat.feature.chat.UpdateNoticeDialog(
                                    versionName = splashVersionName,
                                    onDismiss = {
                                        // 27 轮：关闭公告记录当前大版本段（DataStore 编辑需协程）
                                        scope.launch {
                                            settingsRepository.markMajorVersionNoticeSeen()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
