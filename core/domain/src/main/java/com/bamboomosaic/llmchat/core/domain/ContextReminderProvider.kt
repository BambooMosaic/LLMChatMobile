package com.bamboomosaic.llmchat.core.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 环境上下文 Provider：组装「日期-时分,星期,地区,客户端署名,语言」五项字符串。
 * 每次请求实时生成（注入开关开启的模式，system 末尾），不落库、不进历史。
 * 地区自动识别：系统地区，失败兜底「无」。
 */
interface ContextReminderProvider {
    /**
     * 组装环境上下文。[userNickname] 非空时追加用户称呼项；
     * DS 网页版模式由调用方传空串跳过（该模式提示词固定单句，不注入称呼）。
     */
    fun buildReminder(language: AppLanguage, userNickname: String = ""): String
}

class ContextReminderProviderImpl() : ContextReminderProvider {

    override fun buildReminder(language: AppLanguage, userNickname: String): String {
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
        val base = "$dateTime,$weekday,$region,由BambooMosaic制作的LLMChatMobile客户端,$langText"
        // 19 轮：用户称呼非空时追加为第六项（调用方已按模式排除 DS 网页版）
        return if (userNickname.isNotBlank()) {
            val nickLabel = when (language) {
                AppLanguage.TRADITIONAL_CHINESE -> "用戶稱呼"
                AppLanguage.ENGLISH -> "user's preferred name"
                else -> "用户称呼"
            }
            "$base,$nickLabel:$userNickname"
        } else {
            base
        }
    }
}
