// LLMChatDesktop：独立 Gradle 工程根（26 轮决策：不并入 LLMChatMobile 的 include，
// 避免 KMP/Compose 插件影响 Android 构建；共享核心之后按阶段以 includeBuild / 复合构建接入）。
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "LLMChatDesktop"
