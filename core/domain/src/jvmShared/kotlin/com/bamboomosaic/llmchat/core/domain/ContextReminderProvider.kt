package com.bamboomosaic.llmchat.core.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// 34 轮：客户端署名按平台区分——expect 声明在本文件（jvmShared），
// actual 在 androidMain（LLMChatMobile，保持历史署名不变）/ desktopMain（LLMChatDesktop）
expect val clientDisplayName: String

/**
 * 环境上下文 Provider：组装「日期-时分,星期,地区,客户端署名,语言」五项字符串。
 * 每次请求实时生成（32 轮起注入当前轮用户消息开头，不再进 system），不落库、不进历史。
 * 地区自动识别：系统地区，失败兜底「无」。
 */
interface ContextReminderProvider {
    /**
     * 组装环境快照（仅易变信息：日期时分 / 星期 / 地区 / 客户端署名 / 语言）。
     * 32 轮对照官方注入方案：用户称呼是静态偏好，不再混入环境快照，
     * 改由调用方归入 system 静态提示词区。
     */
    fun buildReminder(language: AppLanguage): String
}

class ContextReminderProviderImpl() : ContextReminderProvider {

    override fun buildReminder(language: AppLanguage): String {
        val locale = when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        // 本地时区日期 + 精确时分
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm"))
        val weekday = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        // 地区自动识别：系统地区，失败兜底「无」
        val region = Locale.getDefault().displayCountry.takeIf { it.isNotBlank() } ?: "无"
        val langText = when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> "中文"
            AppLanguage.TRADITIONAL_CHINESE -> "繁體中文"
            AppLanguage.ENGLISH -> "English"
        }
        return "$dateTime,$weekday,$region,由BambooMosaic制作的${clientDisplayName}客户端,$langText"
    }
}
