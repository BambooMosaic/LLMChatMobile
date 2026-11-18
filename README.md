# LLMChatMobile

一个 Android 原生的 LLM 聊天客户端，支持 OpenAI 兼容接口（DeepSeek / OpenAI / 智谱 GLM / 通义千问 / Ollama / 自定义）。

当前处于 **alpha 阶段**，功能可用，接口和数据结构仍可能调整。

## 功能

- **流式对话**：手写 OkHttp SSE 解析，逐字输出
- **Markdown 渲染**：标题、列表、引用、表格、代码块语法高亮，代码块可一键复制
- **深度思考**：推理模型（如 `deepseek-flash`）的思考过程以小号半透明字体展示在正文之前，可折叠
- **多会话管理**：侧边栏会话列表，长按可重命名或归档；归档会话可在设置中恢复
- **Token 统计**：每轮消耗、当前会话累计、全局累计三级展示，数据落 Room
- **上下文压缩**：纯本地滑动窗口裁剪，自动压缩开关 + 最大上下文 / 最大输出可配置
- **多语言**：简体中文 / 繁体中文 / English，切换即时生效
- **深浅主题**：深色（深灰底 + 中灰分层）/ 浅色（纯白），即时切换
- **多供应商预设**：选中供应商后自动填充 Base URL 与模型列表

## 技术栈

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3 |
| 架构 | 多模块 + MVVM + Hilt |
| 网络 | OkHttp + kotlinx.serialization（手写 SSE） |
| 存储 | Room（会话 / 消息 / token 用量）+ DataStore（配置） |
| 异步 | Kotlin Coroutines + Flow |

## 模块结构

```
:app              应用入口，唯一的实现组装处
:core:domain      领域模型与仓储接口（纯 Kotlin，无 Android 依赖）
:core:data        Room / DataStore 实现，Hilt 绑定
:core:network     OkHttp SSE 客户端
:feature:chat     聊天界面（只依赖 domain 接口）
```

依赖方向单向：`feature -> domain <- data <- network`，跨 feature 只走接口或路由。

## 构建

环境要求：JDK 17+、Android SDK Platform 35、Build Tools 35。

```bash
# 需要 local.properties 指向本机 SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties

./gradlew :app:assembleDebug   # 编译 debug 包
./gradlew installDebug        # 安装到已连接设备
```

## 配置

应用内 **侧边栏 → 设置 → API 配置**：

1. 选择供应商（或选「自定义」）
2. 填写 Base URL 与 API Key
3. 选择 / 填写模型名

Base URL 需包含版本段（如 `https://api.deepseek.com/v1`），客户端会拼上 `/chat/completions`。

**API Key 只保存在本机 DataStore，不会上传或写进代码。**

## 已知限制

- 尚未提供 Release 签名与混淆配置
- 上下文压缩使用字符数估算 token，非精确计数
- 附件只支持文本类文件，直接追加到输入框

## License

暂未指定。
