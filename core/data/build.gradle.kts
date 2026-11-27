import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 桌面端第 2 轮：core/data KMP 化——android + desktop 双目标。
// Room（2.8.5 KMP）+ DataStore（preferences KMP 工件）逻辑全进 commonMain；
// 平台件拆分：Android Keystore 密钥加密 / Hilt 模块 / 备份 / 系统深色判定 → androidMain，
// 桌面端（Room BundledSQLiteDriver + 文件型 DataStore + 明文密钥 TODO）→ desktopMain。
// Mobile 行为零变化：Android 侧 Room 仍用 context + framework SQLite 构建同名库文件，
// DataStore 仍是同名 preferences 文件（androidMain 提供委托），迁移路径与改造前完全一致。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

group = "com.bamboomosaic.llmchat"
version = "0.1.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // kotlin.time.Clock（替代 System.currentTimeMillis 的 common 写法）仍需 opt-in
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(project(":core:network"))
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            // @Inject 注解（javax.inject）common 层使用；桌面端手写注入不跑 Hilt 编译器
            implementation(libs.javax.inject)
        }
        getByName("androidMain").dependencies {
            implementation(libs.hilt.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.room.ktx)
        }
        getByName("desktopMain").dependencies {
            // Room 桌面端 SQLite 驱动（自带各平台 native，无需系统安装）
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

android {
    namespace = "com.bamboomosaic.llmchat.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Room 编译器双端各跑一份；Hilt 编译器仅 Android（桌面端手写注入，无 Hilt 图）
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
    add("kspAndroid", libs.hilt.android.compiler)
}
