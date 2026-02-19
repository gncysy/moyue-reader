package com.moyue.util

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

object RSAUtils {
    
    private const val ALGORITHM = "RSA"
    
    fun encodeToString(data: String, publicKeyStr: String): String {
        try {
            val keyBytes = Base64.getDecoder().decode(publicKeyStr)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val publicKey = keyFactory.generatePublic(keySpec) as PublicKey
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            throw RuntimeException("RSA加密失败: ${e.message}", e)
        }
    }
    
    fun decodeToString(data: String, privateKeyStr: String): String {
        try {
            val keyBytes = Base64.getDecoder().decode(privateKeyStr)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val privateKey = keyFactory.generatePrivate(keySpec) as PrivateKey
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(data))
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("RSA解密失败: ${e.message}", e)
        }
    }
}
