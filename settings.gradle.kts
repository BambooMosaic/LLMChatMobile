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

// 23 轮：桌面端子项目占位（1.0 后启动，见 LLMChatDesktop/README.md）。
// 启用前需要先接入 Kotlin Multiplatform 插件与 Compose Desktop，未启用时不参与构建：
// include(":LLMChatDesktop")
