package com.bamboomosaic.llmchat.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * LLMChatDesktop 桌面壳入口。
 * 桌面端第 2 轮：会话/消息/设置/API 配置持久化（存储层 KMP 共享）+ 会话列表 + 功能对话框。
 * UI 见 DesktopApp.kt / DesktopDialogs.kt；装配见 DesktopContainer.kt。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LLMChat Desktop",
        state = rememberWindowState(width = 1100.dp, height = 760.dp),
    ) {
        DesktopApp(DesktopChatViewModel(
            settingsRepository = DesktopContainer.settingsRepository,
            configRepository = DesktopContainer.configRepository,
            conversationRepository = DesktopContainer.conversationRepository,
            chatRepository = DesktopContainer.chatRepository,
            contextWindowRepository = DesktopContainer.contextWindowRepository,
            contextReminderProvider = DesktopContainer.contextReminderProvider,
        ))
    }
}
