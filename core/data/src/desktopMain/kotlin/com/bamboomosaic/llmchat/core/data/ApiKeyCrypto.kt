package com.bamboomosaic.llmchat.core.data

/**
 * Desktop actual：TODO 桌面端密钥加密——本轮恒等实现（API Key 明文存库），
 * 后续接入系统级凭据库（Windows Credential Locker / macOS Keychain）替换。
 */
actual object ApiKeyCrypto {

    actual fun isEncrypted(stored: String): Boolean = false

    actual fun encrypt(plain: String): String = plain

    actual fun decrypt(stored: String): String = stored
}
