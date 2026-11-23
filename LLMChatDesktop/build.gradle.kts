// LLMChatDesktop 桌面壳（阶段〇）：Kotlin/JVM + Compose Multiplatform 仅挂 Desktop 目标。
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
