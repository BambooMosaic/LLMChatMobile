package com.bamboomosaic.llmchat.feature.chat

/**
 * 27 轮：内置提示词模板库（与「模式」完全隔离的轻量体系）。
 * 模板 = 一段用户层系统提示词的起点文本；选中后直接填进「补充上下文」输入框，
 * 用户可在其后追加内容或再编辑——不创建模式、不导入导出，仅作快速辅助。
 * 定位：轻量短句工具向（摘要 / 润色 / 翻译 / 扩写 / 精简等），不含复杂角色设定。
 */
object PromptTemplateLibrary {

    data class Template(val name: String, val content: String)

    data class Category(val name: String, val templates: List<Template>)

    val categories: List<Category> = listOf(
        Category(
            name = "写作润色",
            templates = listOf(
                Template(
                    name = "通用润色",
                    content = "请润色下面的文字，使其更流畅自然，保持原意不变，直接给出润色后的结果：\n\n",
                ),
                Template(
                    name = "扩写",
                    content = "请把下面的内容扩写得更详尽，补充合理的细节和过渡，但不要虚构事实：\n\n",
                ),
                Template(
                    name = "精简",
                    content = "请把下面的内容精简到一半以内的篇幅，保留全部关键信息：\n\n",
                ),
                Template(
                    name = "正式语气",
                    content = "请把下面的内容改写成正式、礼貌的书面语气，适合工作场合：\n\n",
                ),
                Template(
                    name = "轻松语气",
                    content = "请把下面的内容改写成轻松、口语化的语气，像朋友聊天一样：\n\n",
                ),
            ),
        ),
        Category(
            name = "翻译互译",
            templates = listOf(
                Template(
                    name = "中译英",
                    content = "请把下面的中文翻译成英文，保留原意与语气，直接给出译文：\n\n",
                ),
                Template(
                    name = "英译中",
                    content = "请把下面的英文翻译成中文，译文要符合中文表达习惯，直接给出译文：\n\n",
                ),
                Template(
                    name = "双语对照",
                    content = "请把下面的内容分别用中文和英文各写一遍，做成双语对照：\n\n",
                ),
                Template(
                    name = "译文润色",
                    content = "请检查下面的翻译，指出不准确的地方，并给出更自然的译法：\n\n",
                ),
            ),
        ),
        Category(
            name = "总结摘要",
            templates = listOf(
                Template(
                    name = "长文摘要",
                    content = "请把下面的内容总结成一段简明摘要，突出核心结论：\n\n",
                ),
                Template(
                    name = "要点提取",
                    content = "请从下面的内容中提炼出要点，用编号列表逐条列出：\n\n",
                ),
                Template(
                    name = "一句话总结",
                    content = "请用一句话概括下面内容的核心信息：\n\n",
                ),
                Template(
                    name = "会议纪要",
                    content = "请把下面的会议记录整理成纪要，分「结论 / 待办 / 讨论」三部分列出：\n\n",
                ),
            ),
        ),
        Category(
            name = "代码助手",
            templates = listOf(
                Template(
                    name = "解释代码",
                    content = "请逐段解释下面代码的作用，最后总结整体逻辑：\n\n",
                ),
                Template(
                    name = "补注释",
                    content = "请给下面的代码补充必要的注释，解释关键步骤：\n\n",
                ),
                Template(
                    name = "代码审查",
                    content = "请审查下面的代码，指出潜在问题和风险，并给出改进建议：\n\n",
                ),
                Template(
                    name = "重构建议",
                    content = "请对下面的代码给出重构建议，先说思路再给示例代码：\n\n",
                ),
            ),
        ),
        Category(
            name = "学习讲解",
            templates = listOf(
                Template(
                    name = "名词解释",
                    content = "请用通俗易懂的语言解释下面这个概念，并举一个生活化的例子：\n\n",
                ),
                Template(
                    name = "深入浅出",
                    content = "请把下面的内容讲得让完全没接触过的人也能听懂，多用比喻：\n\n",
                ),
                Template(
                    name = "出题自测",
                    content = "请根据下面的内容出 5 道自测题（附答案），帮我检验理解程度：\n\n",
                ),
                Template(
                    name = "对比分析",
                    content = "请对比分析下面的几个对象，用表格列出异同点和各自的适用场景：\n\n",
                ),
            ),
        ),
    )
}
