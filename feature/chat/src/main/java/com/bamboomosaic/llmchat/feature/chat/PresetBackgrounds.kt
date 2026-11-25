package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

/**
 * 31 轮：内置流光聊天背景预设——对角渐变纯代码绘制，不打包图片资源（APK 不增重、任意分辨率不糊）。
 * 渲染时统一叠加 72% 主题色遮罩（见 ChatScreen 背景层），保证深浅主题下文字可读。
 * id 空串 = 无预设（回退自定义图片或默认背景）。
 */
object PresetBackgrounds {

    data class Preset(val id: String, val colors: List<Color>)

    val all = listOf(
        // 紫樱：紫→粉（用户喜欢的流光测试图配色）
        Preset("lilac", listOf(Color(0xFF8E6EE8), Color(0xFFC77DBE), Color(0xFFFF9BB0))),
        // 极光：深靛→青绿
        Preset("aurora", listOf(Color(0xFF283593), Color(0xFF00897B), Color(0xFF4DB6AC))),
        // 晚霞：橙→玫红→紫
        Preset("sunset", listOf(Color(0xFFFF8A50), Color(0xFFFF5C8A), Color(0xFF8E6EE8))),
        // 晨雾：天蓝→淡紫
        Preset("mist", listOf(Color(0xFF6FA8DC), Color(0xFF9FA8DA), Color(0xFFCE93D8))),
    )

    fun brushOf(id: String): Brush? =
        all.firstOrNull { it.id == id }?.let { preset ->
            Brush.linearGradient(
                preset.colors,
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            )
        }
}
