package com.moyue.util

import org.slf4j.LoggerFactory
import java.security.MessageDigest

/**
 * MD5 加密工具类
 * 
 * 提供 MD5 哈希计算功能，支持字符串和字节数组
 * 
 * @author Moyue
 * @since 1.0.0
 */
object MD5Utils {
    
    private val logger = LoggerFactory.getLogger(MD5Utils::class.java)
    
    /**
     * MD5 加密（字符串）
     * 
     * @param input 待加密字符串
     * @param toLowerCase 是否输出小写（默认 true）
     * @return 32 位 MD5 哈希字符串，失败返回空字符串
     */
    fun md5Encode(input: String, toLowerCase: Boolean = true): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            val hash = digest.joinToString("") { "%02x".format(it) }
            if (toLowerCase) hash.lowercase() else hash.uppercase()
        } catch (e: Exception) {
            logger.error("MD5 加密失败: $input", e)
            ""
        }
    }
    
    /**
     * MD5 加密（字节数组）
     * 
     * @param input 待加密字节数组
     * @param toLowerCase 是否输出小写（默认 true）
     * @return 32 位 MD5 哈希字符串，失败返回空字符串
     */
    fun md5Encode(input: ByteArray, toLowerCase: Boolean = true): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input)
            val hash = digest.joinToString("") { "%02x".format(it) }
            if (toLowerCase) hash.lowercase() else hash.uppercase()
        } catch (e: Exception) {
            logger.error("MD5 加密失败", e)
            ""
        }
    }
    
    /**
     * MD5 加密（带盐值）
     * 
     * 盐值可以增强安全性，防止彩虹表攻击
     * 
     * @param input 待加密字符串
     * @param salt 盐值字符串
     * @param toLowerCase 是否输出小写（默认 true）
     * @return 32 位 MD5 哈希字符串
     */
    fun md5EncodeWithSalt(input: String, salt: String, toLowerCase: Boolean = true): String {
        return md5Encode(input + salt, toLowerCase)
    }
}
