# LLMChatMobile R8 keep 规则（31 轮）
# 目标：混淆/裁剪不影响运行时反射——Hilt 注入、Room 数据库、kotlinx-serialization
# 序列化（备份实体 / 模式配置 / SSE 数据模型）。

# ================= kotlinx-serialization =================
# @Serializable 类依赖编译期生成的 serializer 与 Companion 字段，混淆会破坏反射查找
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.bamboomosaic.llmchat.**$$serializer { *; }
-keepclassmembers class com.bamboomosaic.llmchat.** {
    *** Companion;
}
-keepclasseswithmembers class com.bamboomosaic.llmchat.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ================= Room =================
# 实体 / DAO / 迁移类兜底（Room 自带 consumer 规则，此处防裁剪）
-keep class com.bamboomosaic.llmchat.core.data.db.** { *; }

# ================= Hilt / javax.inject =================
# Hilt 自带 consumer 规则，此处兜底注入注解
-keep class javax.inject.** { *; }

# ================= 其他 =================
# SSE 流式与 TTS 响应基于 kotlinx JsonObject 手动解析（非数据类反射），
# @Serializable DTO 自动被上方序列化规则覆盖。
