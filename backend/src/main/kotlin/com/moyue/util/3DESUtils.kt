package com.moyue.util

import org.slf4j.LoggerFactory
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.IvParameterSpec
import java.util.Base64

/**
 * 3DES（TripleDES）加密工具类
 * 
 * 支持 DESede/ECB/PKCS5Padding 和 DESede/CBC/PKCS5Padding 模式
 * 
 * Key 长度：24 字节（192位）
 * IV 长度：8 字节（CBC 模式需要）
 * 
 * 注意：虽然比 DES 安全，但仍推荐使用 AES
 * 
 * @author Moyue
 * @since 1.0.0
 */
object DESUtils {
    
    private val logger = LoggerFactory.getLogger(DESUtils::class.java)
    
    private const val ALGORITHM = "DESede"
    private const val TRANSFORMATION_ECB = "DESede/ECB/PKCS5Padding"
    private const val TRANSFORMATION_CBC = "DESede/CBC/PKCS5Padding"
    private const val KEY_SIZE = 24
    private const val IV_SIZE = 8
    
    /**
     * 3DES 加密（字符串，ECB 模式）
     * 
     * @param data 明文字符串
     * @param key 密钥字符串（会自动填充或截断到 24 字节）
     * @return Base64 编码的密文字符串，失败返回空字符串
     */
    fun encodeToString(data: String, key: String): String {
        return encodeToString(data, key, "")
    }
    
    /**
     * 3DES 加密（字符串，可选 CBC/ECB 模式）
     * 
     * @param data 明文字符串
     * @param key 密钥字符串（会自动填充或截断到 24 字节）
     * @param iv 初始化向量字符串（空字符串使用 ECB 模式，否则使用 CBC 模式）
     * @return Base64 编码的密文字符串，失败返回空字符串
     */
    fun encodeToString(data: String, key: String, iv: String): String {
        return try {
            val keyBytes = normalizeKey(key.toByteArray(), KEY_SIZE)
            val useCBC = iv.isNotEmpty()
            val encrypted = if (useCBC) {
                val ivBytes = normalizeIv(iv.toByteArray(), IV_SIZE)
                encrypt(data.toByteArray(Charsets.UTF_8), keyBytes, ivBytes, TRANSFORMATION_CBC)
            } else {
                encrypt(data.toByteArray(Charsets.UTF_8), keyBytes, null, TRANSFORMATION_ECB)
            }
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logger.error("3DES 加密失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * 3DES 加密（字节数组）
     * 
     * @param data 明文字节数组
     * @param key 密钥字节数组（24 字节）
     * @param iv 初始化向量字节数组（8 字节，null 表示 ECB 模式）
     * @param transformation 加密模式
     * @return 密文字节数组，失败返回空数组
     */
    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray?, transformation: String): ByteArray {
        return try {
            if (key.size != KEY_SIZE) {
                throw IllegalArgumentException("3DES 密钥长度必须为 $KEY_SIZE 字节，当前为 ${key.size} 字节")
            }
            
            val keySpec = DESedeKeySpec(key)
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            val secretKey = keyFactory.generateSecret(keySpec)
            
            val cipher = Cipher.getInstance(transformation)
            if (iv != null) {
                val ivSpec = IvParameterSpec(iv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            }
            cipher.doFinal(data)
        } catch (e: Exception) {
            logger.error("3DES 加密失败: ${e.message}", e)
            ByteArray(0)
        }
    }
    
    /**
     * 3DES 解密（字符串，ECB 模式）
     * 
     * @param data Base64 编码的密文字符串
     * @param key 密钥字符串（会自动填充或截断到 24 字节）
     * @return 解密后的明文字符串，失败返回空字符串
     */
    fun decodeToString(data: String, key: String): String {
        return decodeToString(data, key, "")
    }
    
    /**
     * 3DES 解密（字符串，可选 CBC/ECB 模式）
     * 
     * @param data Base64 编码的密文字符串
     * @param key 密钥字符串（会自动填充或截断到 24 字节）
     * @param iv 初始化向量字符串（空字符串使用 ECB 模式，否则使用 CBC 模式）
     * @return 解密后的明文字符串，失败返回空字符串
     */
    fun decodeToString(data: String, key: String, iv: String): String {
        return try {
            val keyBytes = normalizeKey(key.toByteArray(), KEY_SIZE)
            val useCBC = iv.isNotEmpty()
            val encrypted = Base64.getDecoder().decode(data)
            val decrypted = if (useCBC) {
                val ivBytes = normalizeIv(iv.toByteArray(), IV_SIZE)
                decrypt(encrypted, keyBytes, ivBytes, TRANSFORMATION_CBC)
            } else {
                decrypt(encrypted, keyBytes, null, TRANSFORMATION_ECB)
            }
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("3DES 解密失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * 3DES 解密（字节数组）
     * 
     * @param data 密文字节数组
     * @param key 密钥字节数组（24 字节）
     * @param iv 初始化向量字节数组（8 字节，null 表示 ECB 模式）
     * @param transformation 加密模式
     * @return 解密后的明文字节数组，失败返回空数组
     */
    fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray?, transformation: String): ByteArray {
        return try {
            if (key.size != KEY_SIZE) {
                throw IllegalArgumentException("3DES 密钥长度必须为 $KEY_SIZE 字节，当前为 ${key.size} 字节")
            }
            
            val keySpec = DESedeKeySpec(key)
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            val secretKey = keyFactory.generateSecret(keySpec)
            
            val cipher = Cipher.getInstance(transformation)
            if (iv != null) {
                val ivSpec = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey)
            }
            cipher.doFinal(data)
        } catch (e: Exception) {
            logger.error("3DES 解密失败: ${e.message}", e)
            ByteArray(0)
        }
    }
    
    // ==================== 私有方法 ====================
    
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
