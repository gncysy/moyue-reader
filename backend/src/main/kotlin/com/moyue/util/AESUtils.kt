package com.moyue.util

import org.slf4j.LoggerFactory
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * AES 加密工具类
 * 
 * 支持 AES/CBC/PKCS5Padding 模式加密解密
 * 
 * Key 长度：16 字节（128位）、24 字节（192位）、32 字节（256位）
 * IV 长度：16 字节（128位）
 * 
 * @author Moyue
 * @since 1.0.0
 */
object AESUtils {
    
    private val logger = LoggerFactory.getLogger(AESUtils::class.java)
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 16
    private const val IV_SIZE = 16
    
    /**
     * AES 加密（字符串）
     * 
     * @param data 明文字符串
     * @param key 密钥字符串（会自动填充或截断到 16 字节）
     * @param iv 初始化向量字符串（会自动填充或截断到 16 字节）
     * @return Base64 编码的密文字符串，失败返回空字符串
     */
    fun encodeToString(data: String, key: String, iv: String): String {
        return try {
            val keyBytes = normalizeKey(key.toByteArray(), KEY_SIZE)
            val ivBytes = normalizeIv(iv.toByteArray(), IV_SIZE)
            val encrypted = encrypt(data.toByteArray(Charsets.UTF_8), keyBytes, ivBytes)
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logger.error("AES 加密失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * AES 加密（字节数组）
     * 
     * @param data 明文字节数组
     * @param key 密钥字节数组（16/24/32 字节）
     * @param iv 初始化向量字节数组（16 字节）
     * @return 密文字节数组，失败返回空数组
     */
    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return try {
            validateKey(key)
            validateIv(iv)
            
            val keySpec = SecretKeySpec(key, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            logger.error("AES 加密失败: ${e.message}", e)
            ByteArray(0)
        }
    }
    
    /**
     * AES 解密（字符串）
     * 
     * @param data Base64 编码的密文字符串
     * @param key 密钥字符串（会自动填充或截断到 16 字节）
     * @param iv 初始化向量字符串（会自动填充或截断到 16 字节）
     * @return 解密后的明文字符串，失败返回空字符串
     */
    fun decodeToString(data: String, key: String, iv: String): String {
        return try {
            val keyBytes = normalizeKey(key.toByteArray(), KEY_SIZE)
            val ivBytes = normalizeIv(iv.toByteArray(), IV_SIZE)
            val encrypted = Base64.getDecoder().decode(data)
            val decrypted = decrypt(encrypted, keyBytes, ivBytes)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("AES 解密失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * AES 解密（字节数组）
     * 
     * @param data 密文字节数组
     * @param key 密钥字节数组（16/24/32 字节）
     * @param iv 初始化向量字节数组（16 字节）
     * @return 解密后的明文字节数组，失败返回空数组
     */
    fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return try {
            validateKey(key)
            validateIv(iv)
            
            val keySpec = SecretKeySpec(key, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            logger.error("AES 解密失败: ${e.message}", e)
            ByteArray(0)
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 验证密钥长度
     */
    private fun validateKey(key: ByteArray) {
        val validLengths = listOf(16, 24, 32)
        if (key.size !in validLengths) {
            throw IllegalArgumentException("AES 密钥长度必须为 ${validLengths.joinToString("/")} 字节，当前为 ${key.size} 字节")
        }
    }
    
    /**
     * 验证 IV 长度
     */
    private fun validateIv(iv: ByteArray) {
        if (iv.size != IV_SIZE) {
            throw IllegalArgumentException("AES IV 长度必须为 $IV_SIZE 字节，当前为 ${iv.size} 字节")
        }
    }
    
    /**
     * 规范化密钥（填充或截断）
     */
    private fun normalizeKey(key: ByteArray, size: Int): ByteArray {
        return if (key.size >= size) {
            key.copyOf(size)
        } else {
            val padded = ByteArray(size)
            System.arraycopy(key, 0, padded, 0, key.size)
            padded
        }
    }
    
    /**
     * 规范化 IV（填充或截断）
     */
    private fun normalizeIv(iv: ByteArray, size: Int): ByteArray {
        return if (iv.size >= size) {
            iv.copyOf(size)
        } else {
            val padded = ByteArray(size)
            System.arraycopy(iv, 0, padded, 0, iv.size)
            padded
        }
    }
}
