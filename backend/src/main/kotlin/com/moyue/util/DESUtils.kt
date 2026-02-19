package com.moyue.util

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import java.util.Base64

object DESUtils {
    
    private const val ALGORITHM = "DES"
    
    fun encodeToString(data: String, key: String): String {
        try {
            val keySpec = DESKeySpec(key.toByteArray())
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            val secretKey = keyFactory.generateSecret(keySpec)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            throw RuntimeException("DES加密失败: ${e.message}", e)
        }
    }
    
    fun decodeToString(data: String, key: String): String {
        try {
            val keySpec = DESKeySpec(key.toByteArray())
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            val secretKey = keyFactory.generateSecret(keySpec)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(data))
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("DES解密失败: ${e.message}", e)
        }
    }
}
