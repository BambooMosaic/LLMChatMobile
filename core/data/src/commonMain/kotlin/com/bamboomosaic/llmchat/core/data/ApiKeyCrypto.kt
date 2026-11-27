package com.bamboomosaic.llmchat.core.data

/**
 * API Key 存储加密（桌面端第 2 轮抽出 expect/actual）。
 * 密文带 "enc:v1:" 前缀；无前缀视为旧明文，解密时原样返回（平滑迁移，用户无感）。
 * - Android：AES-256/GCM + Android Keystore（28 轮实现，androidMain actual 原样保留）
 * - Desktop：TODO 本轮恒等实现（明文存储），后续接入系统级凭据库（Windows Credential Locker 等）
 */
expect object ApiKeyCrypto {
    fun isEncrypted(stored: String): Boolean
    fun encrypt(plain: String): String
    fun decrypt(stored: String): String
}
