package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlinx.coroutines.delay

/**
 * Markdown 渲染：完整支持标题/列表/加粗/表格等语法。
 * 代码块自带头部（复制按钮），点击写入剪贴板并短暂显示"已复制"。
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    val components = markdownComponents(
        codeFence = { model -> CodeBlockWithCopy(model, fence = true) },
        codeBlock = { model -> CodeBlockWithCopy(model, fence = false) },
    )
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

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(model.content))
                    copied = true
                }) {
                    Text(
                        text = if (copied) s.copied else s.copy,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            ) {
                if (fence) {
                    MarkdownHighlightedCodeFence(
                        content = model.content,
                        node = model.node,
                    )
                } else {
                    MarkdownHighlightedCodeBlock(
                        content = model.content,
                        node = model.node,
                    )
                }
            }
        }
    }
}
