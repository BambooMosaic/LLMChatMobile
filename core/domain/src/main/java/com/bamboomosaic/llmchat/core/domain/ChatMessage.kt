package com.bamboomosaic.llmchat.core.domain

enum class MessageRole { SYSTEM, USER, ASSISTANT }

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    /** 推理模型的思考内容（reasoning_content）。只用于展示/持久化，不会回传给 API。 */
    val reasoning: String? = null,
    /** 多模态图片：存 base64 编码的图片数据（仅 USER 消息可能非空）。发送时嵌入请求。 */
    val images: List<String> = emptyList(),
    /** 联网搜索到的网页总数（仅助手消息）。null = 本轮未开启搜索；0 = 开启了但未拿到结果。 */
    val searchPages: Int? = null,
)
