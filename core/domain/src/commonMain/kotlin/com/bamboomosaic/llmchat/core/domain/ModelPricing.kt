package com.bamboomosaic.llmchat.core.domain

/**
 * 23 轮：模型单价表与金额折算（单位：人民币元 / 每百万 token）。
 * 价格为公开报价，厂商调价后直接改这张表即可；无匹配条目的模型不展示金额。
 * 金额为近似估算：同一会话混用多个模型时按当前模型单价折算全部用量。
 */
object ModelPricing {

    /** 单价条目：缓存命中输入 / 未命中输入 / 输出（元 / 1M token）。 */
    data class Price(val cacheHitInput: Double, val missInput: Double, val output: Double)

    /** 前缀匹配表：key 为模型名前缀（小写），取第一个命中的条目。 */
    private val prefixPrices: List<Pair<String, Price>> = listOf(
        // DeepSeek V3.2 系（chat / reasoner / flash 同价）
        "deepseek" to Price(cacheHitInput = 0.2, missInput = 2.0, output = 3.0),
        // GLM-4-Flash 免费
        "glm-4-flash" to Price(cacheHitInput = 0.0, missInput = 0.0, output = 0.0),
        // GLM-4.5 / GLM-4 系
        "glm" to Price(cacheHitInput = 0.5, missInput = 2.0, output = 8.0),
    )

    /** 按模型名取单价；无匹配返回 null。 */
    fun priceOf(model: String): Price? {
        val name = model.trim().lowercase()
        return prefixPrices.firstOrNull { name.startsWith(it.first) }?.second
    }

    /**
     * 折算金额（元）。[promptTokens] 含缓存命中部分，内部自动拆分；
     * 无该模型单价时返回 null（UI 隐藏金额）。
     */
    fun estimateCNY(model: String, promptTokens: Long, completionTokens: Long, cacheHitTokens: Long): Double? {
        val price = priceOf(model) ?: return null
        val hit = minOf(promptTokens, cacheHitTokens).coerceAtLeast(0)
        val miss = (promptTokens - hit).coerceAtLeast(0)
        return miss / 1_000_000.0 * price.missInput +
            hit / 1_000_000.0 * price.cacheHitInput +
            completionTokens / 1_000_000.0 * price.output
    }

    /** 金额展示格式：≥0.01 保留两位小数，更小保留四位（免费/极小值不至于显示 0.00）。 */
    fun formatCNY(value: Double): String =
        if (value >= 0.01 || value == 0.0) "¥" + String.format("%.2f", value)
        else "¥" + String.format("%.4f", value)
}
