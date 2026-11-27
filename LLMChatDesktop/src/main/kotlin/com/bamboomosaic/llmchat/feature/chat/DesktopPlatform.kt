package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * 桌面端平台工具（全量移植轮）：
 * - DesktopToast：替代 android.widget.Toast——全局底部提示条，2.5 秒自动消失
 * - DesktopHost.openUrl：替代 Intent ACTION_VIEW——系统默认浏览器
 * - DesktopFilePicker：替代 SAF ActivityResultContracts——AWT FileDialog（保存/打开/选图）
 * - TtsPlayer：替代 android.media.AudioTrack——javax.sound.sampled 播 24kHz 16bit mono PCM
 */

/** 全局提示条状态（任意线程可调 show，Compose 侧收集渲染）。 */
object DesktopToast {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val scope = CoroutineScope(Dispatchers.Default)

    fun show(text: String) {
        _message.value = text
        scope.launch {
            delay(2500)
            if (_message.value == text) _message.value = null
        }
    }

    /** 挂在根窗口底部：有消息时显示提示条。 */
    @Composable
    fun Host() {
        val msg = _message.collectAsState().value
        if (msg != null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                            androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private val colorScheme by lazy { Color.Unspecified }

/** 系统级能力：打开链接 / 重启。 */
object DesktopHost {

    fun openUrl(url: String) {
        runCatching {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
            } else {
                // 无 Desktop 支持（少见）：退化为命令行
                val os = System.getProperty("os.name").lowercase()
                when {
                    os.contains("win") -> ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
                    os.contains("mac") -> ProcessBuilder("open", url).start()
                    else -> ProcessBuilder("xdg-open", url).start()
                }
            }
        }.onFailure { DesktopToast.show("无法打开链接：$url") }
    }

    /** 导入数据后的「重启应用」：数据已落库，退出进程由用户手动重开。 */
    fun restartApp() {
        kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(600) }
        kotlin.system.exitProcess(0)
    }
}

/** AWT FileDialog 封装（阻塞式，全部放 IO 线程）。 */
object DesktopFilePicker {

    /** 选择保存路径（导出用），返回文件路径或 null。 */
    suspend fun pickSavePath(title: String, defaultName: String): String? = withContext(Dispatchers.IO) {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.SAVE)
        dialog.file = defaultName
        dialog.isVisible = true
        val dir = dialog.directory ?: return@withContext null
        val file = dialog.file ?: return@withContext null
        File(dir, file).absolutePath
    }

    /** 选择打开文件（导入用），返回文件路径或 null。 */
    suspend fun pickOpenPath(title: String): String? = withContext(Dispatchers.IO) {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
        dialog.isVisible = true
        val dir = dialog.directory ?: return@withContext null
        val file = dialog.file ?: return@withContext null
        File(dir, file).absolutePath
    }

    /** 选择图片（发送图片用），返回文件路径或 null。 */
    suspend fun pickImagePath(title: String): String? = withContext(Dispatchers.IO) {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
        dialog.setFilenameFilter { _, name ->
            name.lowercase().endsWith(".jpg") || name.lowercase().endsWith(".jpeg") ||
                name.lowercase().endsWith(".png") || name.lowercase().endsWith(".webp")
        }
        dialog.isVisible = true
        val dir = dialog.directory ?: return@withContext null
        val file = dialog.file ?: return@withContext null
        File(dir, file).absolutePath
    }
}

/**
 * 语音播放（桌面端）：javax.sound.sampled SourceDataLine。
 * - playPcm：24kHz 16bit mono little-endian（与手机端 AudioTrack 参数一致，DeepSeek TTS）
 * - playWav：标准 WAV 字节（Qwen/GLM TTS）
 * stop() 使阻塞中的播放循环提前退出（竞态安全：以 line 身份比对判断）。
 */
object TtsPlayer {

    private var line: SourceDataLine? = null

    /** 播放完整 PCM（阻塞至播完，调用方放 IO 线程）。 */
    fun playPcm(pcm: ByteArray) {
        val format = AudioFormat(24000f, 16, 1, true, false)
        val l = AudioSystem.getSourceDataLine(format)
        l.open(format, 64 * 1024)
        l.start()
        line = l
        var offset = 0
        while (offset < pcm.size) {
            if (line !== l) return // 已被 stop() 中断
            val len = minOf(4096, pcm.size - offset)
            l.write(pcm, offset, len)
            offset += len
        }
        l.drain()
        l.close()
        if (line === l) line = null
    }

    /** 播放标准 WAV 字节（阻塞至播完，调用方放 IO 线程）。 */
    fun playWav(wavBytes: ByteArray) {
        val audio = AudioSystem.getAudioInputStream(java.io.ByteArrayInputStream(wavBytes))
        val format = audio.format
        val l = AudioSystem.getSourceDataLine(format)
        l.open(format)
        l.start()
        line = l
        val buf = ByteArray(4096)
        while (line === l) {
            val n = audio.read(buf)
            if (n < 0) break
            l.write(buf, 0, n)
        }
        l.drain()
        l.close()
        audio.close()
        if (line === l) line = null
    }

    /** 停止当前播放（下一次 write 返回后循环退出）。 */
    fun stop() {
        line?.let {
            runCatching { it.stop() }
            runCatching { it.close() }
        }
        line = null
    }
}

/** 桌面端窗口宽度（dp）——替代 Android 的 LocalConfiguration.screenWidthDp。 */
@Composable
fun rememberScreenWidthDp(): Int =
    LocalWindowInfo.current.containerDpSize.width.value.roundToInt()
