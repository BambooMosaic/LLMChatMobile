pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LLMChatMobile"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":feature:chat")

// 23 轮：桌面端子项目占位（见 LLMChatDesktop/README.md）。
// 26 轮决策：桌面端作为独立 Gradle 工程根单独构建（cd LLMChatDesktop && gradlew run），
// 不并入本工程 include——避免 KMP/Compose 插件影响 Android 构建；共享核心按阶段以复合构建接入。
// include(":LLMChatDesktop")
