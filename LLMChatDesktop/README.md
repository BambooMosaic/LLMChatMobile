# LLMChatDesktop（共享域核心已接入）

LLMChatMobile 的桌面端子项目。基于 **Kotlin/JVM + Compose Multiplatform（仅 Desktop 目标）**，目标是与 Android 端共享核心代码，一套代码出 Windows / macOS / Linux 桌面安装包。

> 26 轮决策：**独立 Gradle 工程根**，不并入 LLMChatMobile 的 include——避免 KMP/Compose 插件影响 Android 构建。当前为阶段一（core:domain 已共享，复合构建接入），共享核心按下面阶段逐步接入。

## 运行

```bash
cd LLMChatDesktop
gradlew.bat run        # Windows；macOS / Linux 用 ./gradlew run
```

## 为什么不是重写

| 模块 | 复用情况 |
|---|---|
| `core:domain`（模式体系 / 省 Token 档位 / 领域模型） | ✅ 纯 Kotlin，零改动 |
| `core:network`（手写 OkHttp SSE 流式） | ✅ OkHttp 本就是 JVM 库，零改动 |
| `core:data`(Room / DataStore) | ⚠️ 迁移到 Room-KMP + DataStore-KMP（好消息：Android 端用的 Room 2.8.5 / DataStore 1.3 本身已是 KMP 兼容版本线） |
| `feature:chat`（Compose UI） | ⚠️ 大部分复用，平台件换 expect/actual |
| 依赖注入（Hilt） | ❌ 桌面不支持，换 Koin 或手写构造注入 |

最难写的部分（SSE 流式解析、双层系统提示词、档位上限逻辑、Markdown 表格、模式导入导出）全部在复用区——**移植，不是重写**。

## 阶段计划

0. **阶段〇（已完成）**：独立桌面壳——Kotlin 2.2.20 + Compose Multiplatform 1.12.2，单窗口占位 UI，`gradlew run` 可运行
1. **阶段一（无行为变化）**：`core:domain` + `core:network` 抽成 KMP 共享模块，Android 端照常消费
2. **阶段二**：`core:data` 迁 Room-KMP / DataStore-KMP；Hilt → Koin
3. **阶段三**：复用聊天 UI，文件选择器 / Toast / 下载目录等平台件 expect/actual

## 打包

启用 `compose.desktop.application` 插件的 `nativeDistributions` + jpackage（构建文件里已留注释模板）：Windows 出 `.exe` / `.msi`，macOS 出 `.dmg`，Linux 出 `.deb`。UI 成型后再启用。

## 状态

- [x] 阶段〇：独立桌面壳（可编译可运行窗口）
- [x] 阶段一（34 轮）：core:domain KMP 化（android+desktop 双目标）+ 复合构建接入，窗口已渲染共享域实时数据
- [ ] 阶段一收尾：core:network（OkHttp SSE，JVM 直接可用）接入桌面端
- [ ] 阶段二：存储层 KMP 化 + Koin
- [ ] 阶段三：聊天 UI 复用（首个可用的桌面端）
- [ ] 打包（jpackage 出安装包）
