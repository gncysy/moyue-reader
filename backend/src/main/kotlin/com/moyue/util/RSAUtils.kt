package com.moyue.util

import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

/**
 * RSA 加密工具类
 * 
 * 支持 RSA/ECB/PKCS1Padding 模式
 * 
 * 密钥长度：1024/2048/4096 位（推荐 2048 位）
 * 数据长度限制：密钥长度 - 11 字节（2048 位密钥最大 245 字节）
 * 
 * 注意：超过最大长度时需要分段加密/解密
 * 
 * @author Moyue
 * @since 1.0.0
 */
object RSAUtils {
    
    private val logger = LoggerFactory.getLogger(RSAUtils::class.java)
    
    private const val ALGORITHM = "RSA"
    private const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    
    /**
     * 计算最大加密数据长度
     * 
     * @param keyLength 密钥长度（位）
     * @return 最大加密数据长度（字节）
     */
    fun getMaxDataLength(keyLength: Int): Int {
        return keyLength / 8 - 11
    }
    
    /**
     * RSA 加密（字符串，自动分段）
     * 
     * @param data 明文字符串
     * @param publicKeyStr Base64 编码的公钥字符串
     * @param keyLength 密钥长度（位），默认 2048
     * @return Base64 编码的密文字符串，失败返回空字符串
     */
    fun encodeToString(data: String, publicKeyStr: String, keyLength: Int = 2048): String {
        return try {
            val keyBytes = Base64.getDecoder().decode(publicKeyStr)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val publicKey = keyFactory.generatePublic(keySpec) as PublicKey
            
            val dataBytes = data.toByteArray(Charsets.UTF_8)
            val encrypted = encrypt(dataBytes, publicKey, keyLength)
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logger.error("RSA 加密失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * RSA 加密（字节数组，自动分段）
     * 
     * @param data 明文字节数组
     * @param publicKey 公钥对象
     * @param keyLength 密钥长度（位）
     * @return 密文字节数组，失败返回空数组
     */
    fun encrypt(data: ByteArray, publicKey: PublicKey, keyLength: Int): ByteArray {
        return try {
            val maxLength = getMaxDataLength(keyLength)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            
            // 分段加密
            val result = mutableListOf<ByteArray>()
            var offset = 0
            
            while (offset < data.size) {
                val length = minOf(maxLength, data.size - offset)
                val block = cipher.doFinal(data, offset, length)
                result.add(block)
                offset += length
            }
            
            // 合并结果
            val totalSize = result.sumOf { it.size }
            val encrypted = ByteArray(totalSize)
            var pos = 0
            result.forEach { block ->
                System.arraycopy(block, 0, encrypted, pos, block.size)
                pos += block.size
            }
            
            encrypted
        } catch (e: Exception) {
            logger.error("RSA 加密失败: ${e.message}", e)
            ByteArray(0)
        }
    }
    
    /**
     * RSA 解密（字符串，自动分段）
     * 
     * @param data Base64 编码的密文字符串
     * @param privateKeyStr Base64 编码的私钥字符串
     * @param keyLength 密钥长度（位），默认 2048
     * @return 解密后的明文字符串，失败返回空字符串
     */
    fun decodeToString(data: String, privateKeyStr: String, keyLength: Int = 2048): String {
        return try {
            val keyBytes = Base64.getDecoder().decode(privateKeyStr)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val privateKey = keyFactory.generatePrivate(keySpec) as PrivateKey
            
            val encrypted = Base64.getDecoder().decode(data)
            val decrypted = decrypt(encrypted, privateKey, keyLength)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("RSA 解密失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * RSA 解密（字节数组，自动分段）
     * 
     * @param data 密文字节数组
     * @param privateKey 私钥对象
     * @param keyLength 密钥长度（位）
     * @return 解密后的明文字节数组，失败返回空数组
     */
    fun decrypt(data: ByteArray, privateKey: PrivateKey, keyLength: Int): ByteArray {
        return try {
            val blockSize = keyLength / 8
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            
            // 分段解密
            val result = mutableListOf<ByteArray>()
            var offset = 0
            
            while (offset < data.size) {
                val length = minOf(blockSize, data.size - offset)
                val block = cipher.doFinal(data, offset, length)
                result.add(block)
                offset += length
            }
            
            // 合并结果
            val totalSize = result.sumOf { it.size }
            val decrypted = ByteArray(totalSize)
            var pos = 0
            result.forEach { block ->
                System.arraycopy(block, 0, decrypted, pos, block.size)
                pos += block.size
            }
            
            decrypted
        } catch (e: Exception) {
            logger.error("RSA 解密失败: ${e.message}", e)
            ByteArray(0)
        }
    }
}
