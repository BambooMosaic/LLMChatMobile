// LLMChatDesktop 桌面壳（阶段一：共享域核心已接入）：Kotlin/JVM + Compose Multiplatform 仅挂 Desktop 目标。
// 版本与 Android 端对齐：Kotlin 2.2.20（见根工程 gradle/libs.versions.toml）；
// CMP 1.12.2 为当前稳定线（2026-10，JetBrains 政策：最新 CMP 兼容最新 Kotlin）。
plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.12.2"
    application
}

group = "com.bamboomosaic.llmchat"
version = "0.1.0"

kotlin {
    // 与 Android 端同款 JDK，避免再下载 toolchain
    jvmToolchain(21)
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    // 当前操作系统的 Skia 原生运行时（skiko-awt-runtime-windows-x64），缺失会在窗口创建时
    // 抛 LibraryLoadException: Cannot find skiko-windows-x64.dll
    implementation(compose.desktop.currentOs)

    // 34 轮：共享域核心 + 共享网络层——复合构建替换为 Mobile 工程对应模块的 desktop JVM 变体
    implementation("com.bamboomosaic.llmchat:core-domain:0.1.0")
    implementation("com.bamboomosaic.llmchat:core-network:0.1.0")
    // 桌面端第 2 轮：共享存储层（Room + DataStore，KMP）
    implementation("com.bamboomosaic.llmchat:core-data:0.1.0")
    // core:data 以 implementation 暴露 API，桌面端直接用到其类型时需自行带上传递依赖
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.datastore:datastore-preferences-core:1.3.0")
}

application {
    mainClass.set("com.bamboomosaic.llmchat.desktop.MainKt")
}

// 之后轮次的打包入口（jpackage：Windows .exe/.msi、Linux .deb、macOS .dmg）：
// compose.desktop {
//     application {
//         mainClass = "com.bamboomosaic.llmchat.desktop.MainKt"
//         nativeDistributions { targetFormats(TargetFormat.Msi, TargetFormat.Exe) }
//     }
// }
// 说明：当前先用 Gradle application 插件提供 `run`/`installDist`，打包插件待 UI 成型后启用。
