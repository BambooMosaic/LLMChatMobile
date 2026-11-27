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

// 34 轮（阶段一）：复合构建接入 Mobile 共享域核心。
// includeBuild 整个 LLMChatMobile 工程，依赖替换把
// com.bamboomosaic.llmchat:core-domain → LLMChatMobile 的 :core:domain（KMP，取 desktop JVM 变体）。
// 后续阶段接入 core/network 等共享模块时，同样在此追加替换规则。
includeBuild("..") {
    dependencySubstitution {
        substitute(module("com.bamboomosaic.llmchat:core-domain"))
            .using(project(":core:domain"))
    }
}
