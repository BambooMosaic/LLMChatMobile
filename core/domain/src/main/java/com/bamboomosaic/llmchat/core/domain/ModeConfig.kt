package com.bamboomosaic.llmchat.core.domain

/**
 * 对话模式配置（双层系统提示词架构的模式层）。
 * 内置模式硬编码、用户不可修改；自定义模式（custom）的参数与提示词由用户控制。
 */
data class ModeConfig(
    /** 唯一标识，如 chat、work、code。 */
    val modeId: String,
    /** 模式显示名称。 */
    val modeName: String,
    /** 模式简介，选择时展示。 */
    val description: String,
    /** 绑定的温度参数。 */
    val temperature: Float,
    /** 绑定的 top_p 参数。 */
    val topP: Float,
    /** 模式层内置系统提示词。 */
    val builtinPrompt: String,
    /** 用户层输入框占位符（随模式动态变化）。 */
    val userPromptPlaceholder: String,
    /** 是否为内置模式；自定义模式为 false。 */
    val isBuiltin: Boolean = true,
) {
    companion object {
        const val CHAT = "chat"
        const val WORK = "work"
        const val CODE = "code"
        const val ROLEPLAY = "roleplay"
        const val CREATIVE = "creative"
        const val CUSTOM = "custom"
        const val LEGACY = "legacy"
    }
}

/** 6 套内置预设模式（硬编码，普通用户仅可选择不可修改）。 */
object ChatModes {

    /** 自定义模式的默认参数（用户可在弹窗里改）。 */
    const val CUSTOM_DEFAULT_TEMPERATURE = 0.6f
    const val CUSTOM_DEFAULT_TOP_P = 0.75f

    /** 仅保留基础安全底线（自定义模式 / 历史会话兜底的"模式层"）。 */
    const val BASE_SAFETY_PROMPT =
        "你是乐于助人的 AI 助手。请遵守基本的安全与合规底线，不输出违法、有害内容。"

    val all: List<ModeConfig> = listOf(
        ModeConfig(
            modeId = ModeConfig.CHAT,
            modeName = "聊天模式",
            description = "日常问答、翻译、简单出主意，轻松对话",
            temperature = 0.6f,
            topP = 0.75f,
            builtinPrompt = "回答简洁自然，按需分段；支持多领域问答、翻译、简单规划；" +
                "语言口语化，不强行堆砌格式；不主动超长篇输出。",
            userPromptPlaceholder = "可选：补充你的偏好，如回复语言、称呼等",
        ),
        ModeConfig(
            modeId = ModeConfig.WORK,
            modeName = "工作模式",
            description = "帮你整理文档、写方案、总结内容，条理清晰",
            temperature = 0.4f,
            topP = 0.65f,
            builtinPrompt = "输出结构化、逻辑严谨；优先分点罗列、条理清晰；" +
                "贴合职场正式表达；禁止无依据脑补；自动提炼核心结论。",
            userPromptPlaceholder = "可选：补充工作场景，如你的角色、汇报对象等",
        ),
        ModeConfig(
            modeId = ModeConfig.CODE,
            modeName = "代码开发模式",
            description = "写代码、查 bug、解释程序，输出可直接用代码",
            temperature = 0.2f,
            topP = 0.4f,
            builtinPrompt = "代码必须包裹带语言标识的 Markdown 代码块；优先保证语法正确、可直接运行；" +
                "附带必要注释；报错排查优先定位根因；不使用生僻写法。",
            userPromptPlaceholder = "可选：补充技术栈、代码规范等",
        ),
        ModeConfig(
            modeId = ModeConfig.ROLEPLAY,
            modeName = "角色扮演模式",
            description = "和虚拟角色对话，扮演剧情、人设互动",
            temperature = 0.9f,
            topP = 0.85f,
            builtinPrompt = "动作、神态、环境描写统一用（）包裹；严格维持人设一致性；" +
                "对话口语化；禁止跳出角色进行解释说明；安全约束优先级最高。",
            userPromptPlaceholder = "请粘贴角色卡或人设说明（必填）",
        ),
        ModeConfig(
            modeId = ModeConfig.CREATIVE,
            modeName = "创意写作模式",
            description = "写小说、文案、诗歌、剧本，自由发挥创作",
            temperature = 1.0f,
            topP = 0.92f,
            builtinPrompt = "鼓励修辞与氛围描写；支持多文风发散；剧情逻辑自洽；" +
                "长文自动分段；适配小说、散文、文案、歌词等创作场景。",
            userPromptPlaceholder = "可选：补充题材、文风、篇幅要求等",
        ),
        ModeConfig(
            modeId = ModeConfig.CUSTOM,
            modeName = "自定义模式",
            description = "全部参数、提示词自己掌控，自由度最高",
            temperature = CUSTOM_DEFAULT_TEMPERATURE,
            topP = CUSTOM_DEFAULT_TOP_P,
            builtinPrompt = BASE_SAFETY_PROMPT,
            userPromptPlaceholder = "请输入你的自定义提示词（角色卡、规则等）",
            isBuiltin = false,
        ),
        ModeConfig(
            modeId = ModeConfig.LEGACY,
            modeName = "Legacy 模式",
            description = "开发者调试用，不使用双层提示词隔离",
            temperature = CUSTOM_DEFAULT_TEMPERATURE,
            topP = CUSTOM_DEFAULT_TOP_P,
            builtinPrompt = "",
            userPromptPlaceholder = "输入完整系统提示词（原样发送，不拼接任何模板）",
            isBuiltin = true,
        ),
    )

    fun find(modeId: String): ModeConfig =
        all.firstOrNull { it.modeId == modeId } ?: customFallback()

    /** 兜底：未知 id（历史数据异常等）回落到自定义模式。 */
    private fun customFallback(): ModeConfig = all.last()
}
