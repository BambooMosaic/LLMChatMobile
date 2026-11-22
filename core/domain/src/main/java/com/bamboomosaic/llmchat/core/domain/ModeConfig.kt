package com.bamboomosaic.llmchat.core.domain

/**
 * 对话模式配置（双层系统提示词架构的模式层）。
 * 内置模式硬编码、用户不可修改；自定义模式（custom）的参数与提示词由用户控制。
 * 文案（名称/说明/内置提示词/占位符）支持三语，跟随当前应用语言。
 */
data class ModeConfig(
    /** 唯一标识，如 chat、work、code。 */
    val modeId: String,
    /** 绑定的温度参数。 */
    val temperature: Float,
    /** 绑定的 top_p 参数。 */
    val topP: Float,
    /** 是否为内置模式；自定义模式为 false。 */
    val isBuiltin: Boolean = true,
    /** false 时隐藏用户层提示词输入（ds_web 禁止用户层）。 */
    val allowUserPrompt: Boolean = true,
    /** 模式绑定/推荐模型（新建会话选中时自动优先选含此模型的配置）。 */
    val bindModel: String? = null,
    /** 是否在 system 末尾注入环境上下文（时间/地区/客户端/语言）。 */
    val injectContext: Boolean = false,
    /** 三语文案包：模式名称 / 简介 / 内置提示词 / 用户层占位符。 */
    val strings: ModeStrings,
) {
    /** 按当前应用语言取显示文案（委托注册表的 find 做三语映射）。 */
    fun localized(language: AppLanguage): ModeStrings =
        ChatModes.find(modeId, language).strings

    companion object {
        const val CHAT = "chat"
        const val ASK = "ask"
        const val WORK = "work"
        const val CODE = "code"
        const val ROLEPLAY = "roleplay"
        const val CREATIVE = "creative"
        const val CUSTOM = "custom"
        const val LEGACY = "legacy"
        const val SAVING = "saving"
        const val DS_WEB = "ds_web"
    }
}

/** 模式的三语文案。 */
data class ModeStrings(
    /** 模式显示名称。 */
    val modeName: String,
    /** 模式简介，选择时展示。 */
    val description: String,
    /** 模式层内置系统提示词。 */
    val builtinPrompt: String,
    /** 用户层输入框占位符（随模式动态变化）。 */
    val userPromptPlaceholder: String,
)

/** 内置模式注册表（硬编码，普通用户仅可选择不可修改）。 */
object ChatModes {

    /** 自定义模式的默认参数（用户可在弹窗里改）。 */
    const val CUSTOM_DEFAULT_TEMPERATURE = 0.6f
    const val CUSTOM_DEFAULT_TOP_P = 0.75f

    /** 仅保留基础安全底线（自定义模式 / 历史会话兜底的"模式层"，三语）。 */
    fun baseSafetyPrompt(language: AppLanguage): String = when (language) {
        AppLanguage.TRADITIONAL_CHINESE ->
            "你是樂於助人的 AI 助手。請遵守基本的安全與合規底線，不輸出違法、有害內容。"
        AppLanguage.ENGLISH ->
            "You are a helpful AI assistant. Follow basic safety and compliance " +
                "guidelines; do not produce illegal or harmful content."
        else -> "你是乐于助人的 AI 助手。请遵守基本的安全与合规底线，不输出违法、有害内容。"
    }

    /** 全部内置模式（8 条，文案三语完整）。 */
    val all: List<ModeConfig> = listOf(
        ModeConfig(
            modeId = ModeConfig.CHAT,
            temperature = 0.6f,
            topP = 0.75f,
            strings = ModeStrings(
                modeName = "聊天模式",
                description = "日常问答、翻译、简单出主意，轻松对话",
                builtinPrompt = "回答简洁自然，按需分段；支持多领域问答、翻译、简单规划；" +
                    "语言口语化，不强行堆砌格式；不主动超长篇输出。",
                userPromptPlaceholder = "可选：补充你的偏好，如回复语言、称呼等",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.ASK,
            temperature = 0.6f,
            topP = 0.8f,
            strings = ModeStrings(
                modeName = "提问模式",
                description = "向AI提出问题/对比内容，AI给你解答并说明理由",
                builtinPrompt = "用户会给出提问主题。请先给出明确结论或答案，再清晰说明理由与依据；" +
                    "理由按重要性排列；表达专业、不回避问题。",
                userPromptPlaceholder = "输入提问主题（必填）",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.WORK,
            temperature = 0.4f,
            topP = 0.65f,
            strings = ModeStrings(
                modeName = "工作模式",
                description = "帮你整理文档、写方案、总结内容，条理清晰",
                builtinPrompt = "输出结构化、逻辑严谨；优先分点罗列、条理清晰；" +
                    "贴合职场正式表达；禁止无依据脑补；自动提炼核心结论。",
                userPromptPlaceholder = "可选：补充工作场景，如你的角色、汇报对象等",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.CODE,
            temperature = 0.2f,
            topP = 0.4f,
            strings = ModeStrings(
                modeName = "代码开发模式",
                description = "写代码、查 bug、解释程序，输出可直接用代码",
                builtinPrompt = "代码必须包裹带语言标识的 Markdown 代码块；优先保证语法正确、可直接运行；" +
                    "附带必要注释；报错排查优先定位根因；不使用生僻写法。",
                userPromptPlaceholder = "可选：补充技术栈、代码规范等",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.ROLEPLAY,
            temperature = 0.9f,
            topP = 0.85f,
            strings = ModeStrings(
                modeName = "角色扮演模式",
                description = "和虚拟角色对话，扮演剧情、人设互动",
                // 18 轮：括号描写规则已迁移至用户层（SystemPromptBuilder 按开关追加），模式层不再硬编码
                builtinPrompt = "严格维持人设一致性；" +
                    "对话口语化；禁止跳出角色进行解释说明；安全约束优先级最高。",
                userPromptPlaceholder = "请粘贴角色卡或人设说明（必填）",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.CREATIVE,
            temperature = 1.0f,
            topP = 0.92f,
            strings = ModeStrings(
                modeName = "创意写作模式",
                description = "写小说、文案、诗歌、剧本，自由发挥创作",
                builtinPrompt = "鼓励修辞与氛围描写；支持多文风发散；剧情逻辑自洽；" +
                    "长文自动分段；适配小说、散文、文案、歌词等创作场景。",
                userPromptPlaceholder = "可选：补充题材、文风、篇幅要求等",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.CUSTOM,
            temperature = CUSTOM_DEFAULT_TEMPERATURE,
            topP = CUSTOM_DEFAULT_TOP_P,
            isBuiltin = false,
            strings = ModeStrings(
                modeName = "自定义模式",
                description = "全部参数、提示词自己掌控，自由度最高",
                builtinPrompt = "你是乐于助人的 AI 助手。请遵守基本的安全与合规底线，不输出违法、有害内容。",
                userPromptPlaceholder = "请输入你的自定义提示词（角色卡、规则等）",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.SAVING,
            temperature = CUSTOM_DEFAULT_TEMPERATURE,
            topP = CUSTOM_DEFAULT_TOP_P,
            strings = ModeStrings(
                modeName = "省钱模式",
                description = "不浪费任何token",
                builtinPrompt = "",
                userPromptPlaceholder = "补充信息（可选，最多256字）",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.DS_WEB,
            temperature = 0.3f,
            topP = 0.75f,
            allowUserPrompt = false,
            bindModel = "deepseek-flash",
            injectContext = true,
            strings = ModeStrings(
                modeName = "DS 网页版",
                description = "模拟 DeepSeek 官方网页版对话体验",
                builtinPrompt = "该助手为 DeepSeek，由深度求索公司创造。",
                userPromptPlaceholder = "",
            ),
        ),
        ModeConfig(
            modeId = ModeConfig.LEGACY,
            temperature = CUSTOM_DEFAULT_TEMPERATURE,
            topP = CUSTOM_DEFAULT_TOP_P,
            strings = ModeStrings(
                modeName = "Legacy 模式",
                description = "开发者调试用，不使用双层提示词隔离",
                builtinPrompt = "",
                userPromptPlaceholder = "输入完整系统提示词（原样发送，不拼接任何模板）",
            ),
        ),
    )

    /** 原始（简中）文案——非 UI 场景（如日志兜底）使用。 */
    fun raw(modeId: String): ModeConfig =
        all.firstOrNull { it.modeId == modeId } ?: all.last()

    /** 按语言取本地化模式配置（UI 与提示词拼接统一入口）。 */
    fun find(modeId: String, language: AppLanguage): ModeConfig {
        val base = all.firstOrNull { it.modeId == modeId } ?: all.last()
        val zh = base.strings
        val localized = when (language) {
            AppLanguage.TRADITIONAL_CHINESE -> localizedZhTw(base.modeId)
                ?: ModeStrings(zh.modeName, zh.description, zh.builtinPrompt, zh.userPromptPlaceholder)
            AppLanguage.ENGLISH -> localizedEn(base.modeId)
                ?: ModeStrings(zh.modeName, zh.description, zh.builtinPrompt, zh.userPromptPlaceholder)
            else -> zh
        }
        return base.copy(strings = localized)
    }

    /** 繁体中文文案表。 */
    private fun localizedZhTw(modeId: String): ModeStrings? = when (modeId) {
        ModeConfig.CHAT -> ModeStrings(
            modeName = "聊天模式",
            description = "日常問答、翻譯、簡單出主意，輕鬆對話",
            builtinPrompt = "回答簡潔自然，按需分段；支援多領域問答、翻譯、簡單規劃；" +
                "語言口語化，不強行堆砌格式；不主動超長篇輸出。",
            userPromptPlaceholder = "可選：補充你的偏好，如回覆語言、稱呼等",
        )
        ModeConfig.WORK -> ModeStrings(
            modeName = "工作模式",
            description = "幫你整理文件、寫方案、總結內容，條理清晰",
            builtinPrompt = "輸出結構化、邏輯嚴謹；優先分點羅列、條理清晰；" +
                "貼合職場正式表達；禁止無依據腦補；自動提煉核心結論。",
            userPromptPlaceholder = "可選：補充工作場景，如你的角色、匯報對象等",
        )
        ModeConfig.CODE -> ModeStrings(
            modeName = "代碼開發模式",
            description = "寫代碼、查 bug、解釋程式，輸出可直接用代碼",
            builtinPrompt = "代碼必須包裹帶語言標識的 Markdown 代碼塊；優先保證語法正確、可直接運行；" +
                "附帶必要註釋；報錯排查優先定位根因；不使用生僻寫法。",
            userPromptPlaceholder = "可選：補充技術棧、代碼規範等",
        )
        ModeConfig.ROLEPLAY -> ModeStrings(
            modeName = "角色扮演模式",
            description = "和虛擬角色對話，扮演劇情、人設互動",
            builtinPrompt = "嚴格維持人設一致性；" +
                "對話口語化；禁止跳出角色進行解釋說明；安全約束優先級最高。",
            userPromptPlaceholder = "請貼上角色卡或人設說明（必填）",
        )
        ModeConfig.CREATIVE -> ModeStrings(
            modeName = "創意寫作模式",
            description = "寫小說、文案、詩歌、劇本，自由發揮創作",
            builtinPrompt = "鼓勵修辭與氛圍描寫；支援多文風發散；劇情邏輯自洽；" +
                "長文自動分段；適配小說、散文、文案、歌詞等創作場景。",
            userPromptPlaceholder = "可選：補充題材、文風、篇幅要求等",
        )
        ModeConfig.CUSTOM -> ModeStrings(
            modeName = "自訂模式",
            description = "全部參數、提示詞自己掌控，自由度最高",
            builtinPrompt = "你是樂於助人的 AI 助手。請遵守基本的安全與合規底線，不輸出違法、有害內容。",
            userPromptPlaceholder = "請輸入你的自訂提示詞（角色卡、規則等）",
        )
        ModeConfig.ASK -> ModeStrings(
            modeName = "提問模式",
            description = "向AI提出問題/對比內容，AI給你解答並說明理由",
            builtinPrompt = "用戶會給出提問主題。請先給出明確結論或答案，再清晰說明理由與依據；" +
                "理由按重要性排列；表達專業、不迴避問題。",
            userPromptPlaceholder = "輸入提問主題（必填）",
        )
        ModeConfig.DS_WEB -> ModeStrings(
            modeName = "DS 網頁版",
            description = "模擬 DeepSeek 官方網頁版對話體驗",
            builtinPrompt = "該助手為 DeepSeek，由深度求索公司創造。",
            userPromptPlaceholder = "",
        )
        ModeConfig.SAVING -> ModeStrings(
            modeName = "省錢模式",
            description = "不浪費任何token",
            builtinPrompt = "",
            userPromptPlaceholder = "補充信息（可選，最多256字）",
        )
        ModeConfig.LEGACY -> ModeStrings(
            modeName = "Legacy 模式",
            description = "開發者除錯用，不使用雙層提示詞隔離",
            builtinPrompt = "",
            userPromptPlaceholder = "輸入完整系統提示詞（原樣發送，不拼接任何模板）",
        )
        else -> null
    }

    /** 英文文案表。 */
    private fun localizedEn(modeId: String): ModeStrings? = when (modeId) {
        ModeConfig.CHAT -> ModeStrings(
            modeName = "Chat",
            description = "Everyday Q&A, translation and quick ideas — easy conversation",
            builtinPrompt = "Answer concisely and naturally; break into paragraphs as needed. " +
                "Support multi-domain Q&A, translation and simple planning. Keep the tone " +
                "conversational; avoid forced formatting; do not pad responses.",
            userPromptPlaceholder = "Optional: add preferences such as reply language or how to address you",
        )
        ModeConfig.WORK -> ModeStrings(
            modeName = "Work",
            description = "Organize documents, draft plans and summarize clearly",
            builtinPrompt = "Produce structured, rigorous output; prefer clear bullet points; " +
                "match formal workplace tone; never fabricate; surface key conclusions.",
            userPromptPlaceholder = "Optional: add context such as your role or audience",
        )
        ModeConfig.CODE -> ModeStrings(
            modeName = "Code",
            description = "Write code, debug and explain programs with runnable output",
            builtinPrompt = "Wrap code in Markdown blocks with language tags; ensure syntax is " +
                "correct and runnable; add necessary comments; debug by root cause; " +
                "avoid obscure constructs.",
            userPromptPlaceholder = "Optional: add your stack or coding standards",
        )
        ModeConfig.ROLEPLAY -> ModeStrings(
            modeName = "Roleplay",
            description = "Talk with virtual characters — story and persona interaction",
            builtinPrompt = "Stay strictly in character; keep dialogue conversational; never break " +
                "character to explain; safety constraints take priority.",
            userPromptPlaceholder = "Paste a character card or persona description (required)",
        )
        ModeConfig.CREATIVE -> ModeStrings(
            modeName = "Creative",
            description = "Fiction, copywriting, poetry and scripts — free-form creation",
            builtinPrompt = "Encourage rhetoric and atmosphere; support diverse styles; keep " +
                "plot logic consistent; auto-segment long pieces; fit novels, essays, " +
                "copywriting and lyrics.",
            userPromptPlaceholder = "Optional: add genre, style or length requirements",
        )
        ModeConfig.CUSTOM -> ModeStrings(
            modeName = "Custom",
            description = "Full control over parameters and prompts — maximum freedom",
            builtinPrompt = "You are a helpful AI assistant. Follow basic safety and compliance " +
                "guidelines; do not produce illegal or harmful content.",
            userPromptPlaceholder = "Enter your custom prompt (character cards, rules, etc.)",
        )
        ModeConfig.ASK -> ModeStrings(
            modeName = "Ask",
            description = "Ask questions or compare options — get answers with reasoning",
            builtinPrompt = "The user provides a topic. Give a clear, direct conclusion or answer first, " +
                "then explain the reasoning and evidence clearly; order reasons by importance; " +
                "stay professional and address the question head-on.",
            userPromptPlaceholder = "Enter your question topic (required)",
        )
        ModeConfig.DS_WEB -> ModeStrings(
            modeName = "DS Web",
            description = "Replicates the DeepSeek official web chat experience",
            builtinPrompt = "该助手为 DeepSeek，由深度求索公司创造。",
            userPromptPlaceholder = "",
        )
        ModeConfig.SAVING -> ModeStrings(
            modeName = "Economy",
            description = "Never waste a single token",
            builtinPrompt = "",
            userPromptPlaceholder = "Extra info (optional, max 256 chars)",
        )
        ModeConfig.LEGACY -> ModeStrings(
            modeName = "Legacy",
            description = "For developer debugging; skips dual-layer prompt isolation",
            builtinPrompt = "",
            userPromptPlaceholder = "Enter a full system prompt (sent as-is, no layering)",
        )
        else -> null
    }
}
