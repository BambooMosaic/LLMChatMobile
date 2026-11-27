package com.bamboomosaic.llmchat.core.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android actual：API Key 加密存储——AES-256/GCM，密钥存 Android Keystore（不可导出）。
 * 28 轮实现原样保留；Keystore 异常（厂商 ROM 差异）时加密回退明文、解密返回空串（提示重填）。
 */
actual object ApiKeyCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "llmchat_apikey_aes"
    private const val PREFIX = "enc:v1:"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    actual fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    actual fun encrypt(plain: String): String = try {
        if (plain.isBlank()) plain
        else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(cipher.iv + ct, Base64.NO_WRAP)
        }
    } catch (e: Exception) {
        plain // Keystore 不可用时回退明文存储（功能优先）
    }

    actual fun decrypt(stored: String): String = try {
        if (!stored.startsWith(PREFIX)) stored
        else {
            val data = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                obtainKey(),
                GCMParameterSpec(GCM_TAG_BITS, data, 0, IV_BYTES),
            )
            String(cipher.doFinal(data, IV_BYTES, data.size - IV_BYTES), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        "" // 密文解不开（Keystore 被清等）：返回空串让用户重填
    }

    private fun obtainKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance("AES", KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        val key = generator.generateKey()
        // 确保 Keystore 里真正可用（个别 ROM generateKey 后 getEntry 失败，提前暴露）
        check((ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey != null)
        return key
    }
}
