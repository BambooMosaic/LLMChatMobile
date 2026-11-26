package com.bamboomosaic.llmchat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashLogHandler()
    }

    /**
     * 31 轮：本地崩溃日志——未捕获异常时把堆栈写入应用私有目录（保留最近 5 个），
     * 设置 → 其他设置 → 导出崩溃日志可带走排障（配合发布归档的 mapping.txt 可还原混淆堆栈）。
     * 处理完链回系统默认 handler，崩溃对话框行为不变。
     */
    private fun installCrashLogHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = File(filesDir, "crash").apply { if (!exists()) mkdirs() }
                // 只保留最近 5 个
                dir.listFiles()?.sortedByDescending { it.name }?.drop(4)?.forEach { it.delete() }
                val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                File(dir, "crash_" + stamp + ".txt").writeText(
                    buildString {
                        append("time: ").append(
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
                        )
                        append("\nversion: ")
                        runCatching {
                            val info = packageManager.getPackageInfo(packageName, 0)
                            append(info.versionName).append(" (").append(info.versionCode).append(")")
                        }
                        append("\nthread: ").append(thread.name)
                        append("\n\n")
                        append(android.util.Log.getStackTraceString(throwable))
                    },
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
