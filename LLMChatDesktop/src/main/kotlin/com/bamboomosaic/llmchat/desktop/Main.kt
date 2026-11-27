package com.bamboomosaic.llmchat.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.ContextReminderProviderImpl
import com.bamboomosaic.llmchat.core.domain.Providers

/**
 * LLMChatDesktop 桌面壳入口。
 * 34 轮（阶段一）：共享域核心接入——窗口里的数据全部来自 core:domain KMP 模块：
 * - ContextReminderProviderImpl 环境快照：java.time 实时计算，验证 JVM 运行时链路
 * - Providers 供应商预设：验证纯 Kotlin 数据模型跨端复用
 * 后续阶段：core:network（OkHttp SSE，JVM 直接可用）→ Room/DataStore KMP 化 → 聊天 UI 复用。
 */
fun main() = singleWindowApplication(
    title = "LLMChat Desktop",
) {
    val reminder = remember {
        ContextReminderProviderImpl().buildReminder(AppLanguage.SIMPLIFIED_CHINESE)
    }
    val providers = remember {
        Providers.all.joinToString(" / ") { it.displayName }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("LLMChat Desktop", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "共享域核心已接入（阶段一）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "环境快照（core:domain 实时计算）：$reminder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "供应商预设（core:domain）：$providers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
