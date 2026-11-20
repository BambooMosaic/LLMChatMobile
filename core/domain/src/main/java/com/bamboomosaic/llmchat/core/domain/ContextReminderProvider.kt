package com.bamboomosaic.llmchat.core.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 环境上下文 Provider：组装「日期,星期,地区,客户端,语言」五项字符串。
 * 每次请求实时生成（ds_web 模式注入 system 末尾），不落库、不进历史。
 */
interface ContextReminderProvider {
    /** @param language 当前应用语言；@param regionOverride 用户设置的地区覆盖（空 = 跟随设备）。 */
    fun buildReminder(language: AppLanguage, regionOverride: String): String
}

class ContextReminderProviderImpl() : ContextReminderProvider {

    override fun buildReminder(language: AppLanguage, regionOverride: String): String {
        val locale = when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        // 本地时区日期（禁止 UTC，避免跨日错位）
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val weekday = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        // 地区回退链：设置覆盖 → 设备地区 → 默认"北京"
        val region = regionOverride.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().displayCountry.takeIf { it.isNotBlank() }
            ?: "北京"
        val langText = when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> "中文"
            AppLanguage.TRADITIONAL_CHINESE -> "繁體中文"
            AppLanguage.ENGLISH -> "English"
        }
        return "$date,$weekday,$region,LLMChatMobile客户端,$langText"
    }
}
