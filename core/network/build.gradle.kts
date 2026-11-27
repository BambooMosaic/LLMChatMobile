import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 34 轮（桌面端阶段一收尾）：core/network KMP 化——android + desktop 双目标。
// OkHttp SSE / TTS 客户端均为纯 JVM 代码（无任何 Android API），全部放 jvmShared；
// Mobile 消费方（app / core:data / feature:chat）零改动，仍走 android 变体；
// 桌面端（LLMChatDesktop 复合构建）走 desktop JVM 变体。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
        // 与 core/domain 相同的手工层级（默认模板已在 gradle.properties 禁用）：
        // jvmShared 承载 OkHttp 等 Java API 代码，android/desktop 两个 JVM 系目标共享
        val jvmShared by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmShared)
        getByName("desktopMain").dependsOn(jvmShared)

        jvmShared.dependencies {
            implementation(project(":core:domain"))
            implementation(libs.squareup.okhttp)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.bamboomosaic.llmchat.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
