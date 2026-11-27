import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 34 轮（桌面端阶段一）：core/domain KMP 化——android + desktop 双目标。
// 纯 Kotlin 逻辑放 commonMain；用到 java.time/Locale 的文件放 jvmShared
// （commonMain 看不到 java.*，jvmShared 是 android/desktop 两个 JVM 系目标共享的中间源集）。
// Mobile 各模块依旧 implementation(project(":core:domain"))，走 android 变体，零改动；
// 桌面端（LLMChatDesktop 复合构建）走 desktop JVM 变体，替换规则见 LLMChatDesktop/settings.gradle.kts。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
}

group = "com.bamboomosaic.llmchat"
version = "0.1.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        // jvmShared：android/desktop 共享的中间源集——Java API（java.time 等）在这里可用
        val jvmShared by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmShared)
        getByName("desktopMain").dependsOn(jvmShared)

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.bamboomosaic.llmchat.core.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
