package com.bamboomosaic.llmchat.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * LLMChatDesktop 桌面壳入口。
 * 34 轮（阶段一收尾）：对话链路打通——UI 见 DesktopChat.kt。
 * 已共享：core:domain（域模型/模式提示词/环境快照）+ core:network（OkHttp SSE 流式）。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LLMChat Desktop",
        state = rememberWindowState(width = 980.dp, height = 720.dp),
    ) {
        DesktopChatApp()
    }
}
