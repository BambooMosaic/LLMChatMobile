package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.MarkdownTokenTypes

/**
 * Markdown 渲染：
 * - 全局自动换行（标题/列表/引用/正文/代码块内文本），只有表格保留横向滚动；
 * - 代码块：spotlight（dev.snipme:highlights）语法高亮 + 头部复制按钮；
 *   自渲染 AnnotatedString 实现折行（替换库内置的横向滚动版本）；
 * - 表格：网格线 + 表头底色 + 横向滚动。
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    enableHorizontalScroll: Boolean = true,
) {
    val components = markdownComponents(
        codeFence = { model -> CodeBlockWithCopy(model, fence = true) },
        // codeBlock（缩进代码块）不注册：AI 回复中带缩进的编号列表等普通文本
        // 会被误渲染为巨型代码块（超高 item 导致列表滚动二次测量跳变）。
        table = { model -> BorderedTable(model) },
    )
    // 注：0.33+ 的库版本默认异步解析（item 首次组合显示 Loading 占位，解析完成后内容
    // 撑高 → LazyColumn 高度剧变 → 滚动补偿 → 上跳抖动），故固定使用 0.32.0（同步解析，
    // 首次组合即完整渲染，item 高度一次到位，滚动稳定）。
    Markdown(
        content = content,
        colors = markdownColor(),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.titleLarge,
            h2 = MaterialTheme.typography.titleMedium,
            h3 = MaterialTheme.typography.titleSmall,
            h4 = MaterialTheme.typography.titleSmall,
            h5 = MaterialTheme.typography.bodyLarge,
            h6 = MaterialTheme.typography.bodyLarge,
            text = MaterialTheme.typography.bodyMedium,
            paragraph = MaterialTheme.typography.bodyMedium,
            code = MaterialTheme.typography.bodySmall,
            inlineCode = MaterialTheme.typography.bodyMedium,
        ),
        components = components,
        modifier = modifier.fillMaxWidth(),
    )
}

/** 语言名 → highlights 语言（库支持的 18 种；JSON/SQL 等不支持的语言返回 null，回退自动推断）。 */
private fun guessLanguage(name: String?): SyntaxLanguage? {
    val n = name?.trim()?.lowercase() ?: return null
    return when (n) {
        "kotlin", "kt" -> SyntaxLanguage.KOTLIN
        "java" -> SyntaxLanguage.JAVA
        "python", "py" -> SyntaxLanguage.PYTHON
        "javascript", "js" -> SyntaxLanguage.JAVASCRIPT
        "typescript", "ts" -> SyntaxLanguage.TYPESCRIPT
        "shell", "sh", "bash", "zsh" -> SyntaxLanguage.SHELL
        "go", "golang" -> SyntaxLanguage.GO
        "rust", "rs" -> SyntaxLanguage.RUST
        "c" -> SyntaxLanguage.C
        "cpp", "c++" -> SyntaxLanguage.CPP
        "csharp", "cs", "c#" -> SyntaxLanguage.CSHARP
        "ruby", "rb" -> SyntaxLanguage.RUBY
        "php" -> SyntaxLanguage.PHP
        "swift" -> SyntaxLanguage.SWIFT
        "dart" -> SyntaxLanguage.DART
        else -> null
    }
}

/** 从 fence 的语言信息行提取语言名（AST：CODE_FENCE_INFO 总在子节点首位）。 */
private fun fenceLanguage(model: MarkdownComponentModel, fence: Boolean): String? {
    if (!fence) return null
    return try {
        val info = model.node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG } ?: return null
        model.content.substring(info.startOffset, info.endOffset).trim().takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

/** 提取代码纯文本：fence 取 CODE_FENCE_CONTENT 子节点；indent block 取整节点文本。 */
private fun extractCode(model: MarkdownComponentModel, fence: Boolean): String = try {
    if (fence) {
        model.node.children
            .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
            .joinToString("\n") { model.content.substring(it.startOffset, it.endOffset) }
            .ifBlank { model.content }
    } else {
        model.content
    }
} catch (e: Exception) {
    model.content
}

/** 代码文字样式：小号等宽，深浅主题分别用对应文字色。 */
@Composable
private fun codeTextStyle(): TextStyle = TextStyle(
    fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
    fontSize = MaterialTheme.typography.bodySmall.fontSize,
    color = MaterialTheme.colorScheme.onSurface,
)

/**
 * 高亮配色（按主题）：关键字紫、字符串绿、数字橙、注释灰。
 * 用 SyntaxThemes.default(darkMode) 为底，copy 覆盖成应用统一配色。
 */
@Composable
private fun codeHighlightsBuilder(language: String? = null): Pair<Highlights.Builder, SyntaxLanguage?> {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val base = SyntaxThemes.default(darkMode = dark)
    val theme = base.copy(
        keyword = if (dark) 0xFFC792EA.toInt() else 0xFF7B1FA2.toInt(),
        string = if (dark) 0xFFA5D6A7.toInt() else 0xFF2E7D32.toInt(),
        literal = if (dark) 0xFFF78C6C.toInt() else 0xFFE65100.toInt(),
        comment = if (dark) 0xFF7A7A7A.toInt() else 0xFF8A8A8A.toInt(),
        multilineComment = if (dark) 0xFF7A7A7A.toInt() else 0xFF8A8A8A.toInt(),
        metadata = if (dark) 0xFF82AAFF.toInt() else 0xFF1565C0.toInt(),
        // default 主题的标点是透明的（括号/冒号/等号会"消失"），必须显式给色
        punctuation = if (dark) 0xFFB0BEC5.toInt() else 0xFF455A64.toInt(),
        mark = if (dark) 0xFFFFCB6B.toInt() else 0xFFB58900.toInt(),
    )
    val lang = guessLanguage(language)
    val builder = remember(theme, lang) {
        Highlights.Builder(theme = theme).let { b -> if (lang != null) b.language(lang) else b }
    }
    return builder to lang
}

/** 简易亮度估算（0-1），用于判断深浅主题。 */
private fun Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)

/** 代码块：头部（复制按钮）+ 高亮正文（自动折行，不横向滚动）。 */
@Composable
private fun CodeBlockWithCopy(model: MarkdownComponentModel, fence: Boolean) {
    val s = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    // 用 AST 精确提取代码文本：fence 的子节点 CODE_FENCE_CONTENT 是纯代码；
    // indent code block（fence=false）直接取整块文本
    val codeText = remember(model.node, model.content) { extractCode(model, fence) }
    val (builder, _) = codeHighlightsBuilder(fenceLanguage(model, fence))
    val highlights = remember(codeText, builder) {
        builder.code(codeText).build()
    }

    // 16 轮重制：圆角 10dp 细边框；顶部栏深灰/浅灰底；代码区底色与全局背景一致；语法高亮保留
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerBorder = if (isDark) Color(0xFF3C3C3C) else Color(0xFFDDDDDD)
    val headerBackground = if (isDark) Color(0xFF2B2B2B) else Color(0xFFEFEFEF)
    val codeBackground = MaterialTheme.colorScheme.background

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = codeBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, containerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column {
            // 顶部栏：语言标签 + 复制按钮（深灰/浅灰底），单行紧凑高度
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(headerBackground)
                    .padding(start = 10.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fenceLanguage(model, fence)?.ifBlank { "code" } ?: "code",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (copied) s.copied else s.copy,
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
                    color = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(codeText))
                        copied = true
                    },
                )
            }
            // 高亮正文：底色与全局背景一致，普通 Text 直接折行
            Text(
                text = buildHighlightedCode(codeText, highlights),
                style = codeTextStyle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 4.dp),
            )
        }
    }
}

/** 用 highlights 结果构造 AnnotatedString（着色 + 加粗），文本自然折行。 */
private fun buildHighlightedCode(code: String, highlights: Highlights): AnnotatedString =
    buildAnnotatedString {
        append(code)
        highlights.getHighlights()
            .filterIsInstance<ColorHighlight>()
            .forEach {
                if (it.location.end <= code.length) {
                    addStyle(
                        SpanStyle(color = Color(it.rgb)),
                        start = it.location.start,
                        end = it.location.end,
                    )
                }
            }
        highlights.getHighlights()
            .filterIsInstance<BoldHighlight>()
            .forEach {
                if (it.location.end <= code.length) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start = it.location.start, end = it.location.end)
                }
            }
    }

/**
 * 20 轮：适合聊天的表格——无外框无竖线，行间横线，表头分隔加粗。
 * 列宽按各列内容的自然宽度计算（52~320dp）；自然总宽 ≤ 消息宽度时撑满消息宽度，
 * 超出时保持自然宽度并横向滚动（列宽不被挤压）。
 */
@Composable
private fun BorderedTable(model: MarkdownComponentModel) {
    val rows = remember(model.node, model.content) { parseTableRows(model) }
    if (rows.isEmpty()) return
    val columnCount = rows.maxOf { it.size }
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val cellStyle = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    androidx.compose.foundation.layout.BoxWithConstraints {
        val availablePx = with(density) { maxWidth.toPx() }
        val minPx = with(density) { 52.dp.toPx() }
        val maxPx = with(density) { 320.dp.toPx() }
        // 每列自然宽度 = 列内最长单行的像素宽（52~320dp 截断）
        val naturals = remember(rows, availablePx) {
            (0 until columnCount).map { col ->
                rows.maxOf { row ->
                    row.getOrElse(col) { "" }.split('\n')
                        .maxOfOrNull { line ->
                            if (line.isBlank()) 0f
                            else textMeasurer.measure(line, cellStyle, maxLines = 1)
                                .size.width.toFloat()
                        } ?: 0f
                }.coerceIn(minPx, maxPx)
            }
        }
        val totalNatural = naturals.sum()
        // 自然总宽超出消息宽度 → 横向滚动模式（列宽保持自然值，不再挤压）
        val scrollable = totalNatural > availablePx

        Column(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .then(
                    if (scrollable) Modifier.width(with(density) { totalNatural.toDp() })
                    else Modifier.fillMaxWidth(),
                ),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(modifier = if (scrollable) Modifier else Modifier.fillMaxWidth()) {
                    (0 until columnCount).forEach { col ->
                        Text(
                            text = row.getOrElse(col) { "" },
                            style = cellStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier =
                                if (scrollable) {
                                    Modifier
                                        .width(with(density) { naturals[col].toDp() })
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                } else {
                                    Modifier
                                        .weight(naturals[col])
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                },
                        )
                    }
                }
                if (rowIndex < rows.lastIndex) {
                    HorizontalDivider(
                        color = if (rowIndex == 0) borderColor else borderColor.copy(alpha = 0.5f),
                        thickness = if (rowIndex == 0) 1.dp else 0.5.dp,
                    )
                }
            }
        }
    }
}

/** 从 AST 解析表格行列：GFM 结构 TABLE > ROW > CELL（跳过 HEADER 分隔行）。 */
private fun parseTableRows(model: MarkdownComponentModel): List<List<String>> {
    return try {
        model.node.children
            .filter { it.type == GFMElementTypes.ROW }
            .map { rowNode ->
                rowNode.children
                    .filter { it.type == GFMTokenTypes.CELL }
                    .map { cell ->
                        // 单元格内的 <br>/<br/> 是换行而非分列；还原为真实换行
                        model.content.substring(cell.startOffset, cell.endOffset)
                            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                            .trim()
                    }
            }
            .filter { it.isNotEmpty() }
    } catch (e: Exception) {
        emptyList()
    }
}
