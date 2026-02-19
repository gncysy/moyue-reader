package com.moyue.util

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object AESUtils {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    
    fun encodeToString(data: String, key: String, iv: String): String {
        try {
            val keySpec = SecretKeySpec(key.toByteArray(), ALGORITHM)
            val ivSpec = IvParameterSpec(iv.toByteArray())
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            throw RuntimeException("AES加密失败: ${e.message}", e)
        }
    }
    
    fun decodeToString(data: String, key: String, iv: String): String {
        try {
            val keySpec = SecretKeySpec(key.toByteArray(), ALGORITHM)
            val ivSpec = IvParameterSpec(iv.toByteArray())
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(data))
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("AES解密失败: ${e.message}", e)
        }
    }
}
