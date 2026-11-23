# LLMChatDesktop（规划中，未启动开发）

LLMChatMobile 的桌面端子项目占位。基于 **Kotlin Multiplatform + Compose Multiplatform**，目标是与 Android 端共享核心代码，一套代码出 Windows / macOS / Linux 桌面安装包。

> 按 Roadmap 约定：**1.0 版本之后启动**，当前此目录只是占位与规划，不参与构建。

## 为什么不是重写

| 模块 | 复用情况 |
|---|---|
| `core:domain`（模式体系 / 省 Token 档位 / 领域模型） | ✅ 纯 Kotlin，零改动 |
| `core:network`（手写 OkHttp SSE 流式） | ✅ OkHttp 本就是 JVM 库，零改动 |
| `core:data`（Room / DataStore） | ⚠️ 迁移到 Room-KMP + DataStore-KMP |
| `feature:chat`（Compose UI） | ⚠️ 大部分复用，平台件换 expect/actual |
| 依赖注入（Hilt） | ❌ 桌面不支持，换 Koin 或手写构造注入 |

最难写的部分（SSE 流式解析、双层系统提示词、档位上限逻辑、Markdown 表格、模式导入导出）全部在复用区——**移植，不是重写**。

## 三阶段计划

1. **阶段一（无行为变化）**：`core:domain` + `core:network` 抽成 KMP 共享模块，Android 端照常消费
2. **阶段二**：`core:data` 迁 Room-KMP / DataStore-KMP；Hilt → Koin
3. **阶段三**：Compose Multiplatform 桌面 target，复用聊天 UI；文件选择器 / Toast / 下载目录等平台件 expect/actual

## 打包

Compose Desktop 的 `compose.desktop.application` 插件 + jpackage：Windows 出 `.exe` / `.msi`，macOS 出 `.dmg`，Linux 出 `.deb`。

## 状态

- [ ] 阶段一
- [ ] 阶段二
- [ ] 阶段三（首个可运行桌面包）
