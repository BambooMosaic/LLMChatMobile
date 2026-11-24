package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    /** 推理模型的思考内容，仅助手消息可能有值，用于历史会话回看。 */
    val reasoning: String? = null,
    /** 多模态图片 base64，用 \u0001 分隔多张；仅用户消息可能有值。 */
    val images: String? = null,
    /** 联网搜索到的网页总数（仅助手消息）；null = 未开启搜索。 */
    val searchPages: Int? = null,
    /**
     * 消息版本号：同一用户提问下的第 N 版 AI 回复（用户消息恒为 1）。
     * 重新生成 / 编辑重发产生新版本（version+1），旧版本保留不覆盖。
     */
    val version: Int = 1,
    /** 版本组键 = 触发该回复的用户消息 id；同一组下的助手消息互为多版本。 */
    val versionKey: Long? = null,
    /** 该版本对应的 token 用量记录 id（token_usage_records.id），用于按版本展示消耗。 */
    val usageId: Long? = null,
    /**
     * 27 轮：父消息 id（消息分叉树）。普通发送=激活链尾消息 id；编辑重发的新用户消息=被编辑用户消息的父；
     * 助手回复=触发的用户消息 id。null=27 轮前的旧数据（按线性逻辑退化处理）。
     */
    val parentId: Long? = null,
)
