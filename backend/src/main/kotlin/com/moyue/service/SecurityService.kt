package com.moyue.service
 
import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.jspecify.annotations.Nullable
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
 
/**
 * 安全服务
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - JWT Token 生成和验证
 * - 密码加密
 * - 安全策略管理
 * - 会话管理
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Service
@Transactional
class SecurityService(
    private val preferenceService: PreferenceService
) {
    
    private val logger = LoggerFactory.getLogger(SecurityService::class.java)
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    private val secureRandom = SecureRandom()
    
    @Value("\${moyue.security.jwt.secret:}")
    private lateinit var jwtSecret: String
    
    @Value("\${moyue.security.jwt.expiration:86400}")
    private var jwtExpiration: Long = 86400
    
    // ==================== 密码加密 ====================
    
    /**
     * 加密密码
     */
    fun encryptPassword(password: String): String {
        return passwordEncoder.encode(password)
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(password: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(password, encodedPassword)
    }
    
    /**
     * 生成随机密码
     */
    fun generateRandomPassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    // ==================== JWT Token 管理 ====================
    
    /**
     * 生成 JWT Token
     */
    fun generateToken(subject: String, additionalClaims: Map<String, Any> = emptyMap()): String {
        logger.debug("生成 JWT Token: $subject")
        
        val secret = getJwtSecret()
        val expiration = getJwtExpiration()
        
        val now = System.currentTimeMillis()
        val expiryDate = now + expiration * 1000
        
        val claims = mutableMapOf<String, Any>(
            "sub" to subject,
            "iat" to (now / 1000),
            "exp" to (expiryDate / 1000),
            "iss" to "moyue-reader"
        )
        
        claims.putAll(additionalClaims)
        
        // 简化的 JWT 生成（实际应使用 JWT 库）
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(com.google.gson.Gson().toJson(claims).toByteArray())
        
        val signature = generateSignature("$header.$payload", secret)
        
        return "$header.$payload.$signature"
    }
    
    /**
     * 验证 JWT Token
     */
    fun verifyToken(token: String): @Nullable Map<String, Any>? {
        logger.debug("验证 JWT Token")
        
        try {
            val parts = token.split(".")
            if (parts.size != 3) {
                logger.warn("无效的 Token 格式")
                return null
            }
            
            val (header, payload, signature) = parts
            val secret = getJwtSecret()
            
            // 验证签名
            val expectedSignature = generateSignature("$header.$payload", secret)
            if (signature != expectedSignature) {
                logger.warn("Token 签名验证失败")
                return null
            }
            
            // 解析 payload
            val payloadJson = String(Base64.getUrlDecoder().decode(payload))
            val claims = com.google.gson.Gson().fromJson(payloadJson, Map::class.java) as Map<String, Any>
            
            // 检查过期时间
            val exp = (claims["exp"] as? Double)?.toLong() ?: 0
            if (System.currentTimeMillis() / 1000 > exp) {
                logger.warn("Token 已过期")
                return null
            }
            
            return claims
        } catch (e: Exception) {
            logger.error("Token 验证失败", e)
            return null
        }
    }
    
    /**
     * 刷新 JWT Token
     */
    fun refreshToken(token: String): @Nullable String? {
        logger.debug("刷新 JWT Token")
        
        val claims = verifyToken(token) ?: return null
        
        val subject = claims["sub"] as? String ?: return null
        val additionalClaims = claims.filter { it.key != "iat" && it.key != "exp" }
        
        return generateToken(subject, additionalClaims)
    }
    
    /**
     * 从 Token 中提取用户名
     */
    @Cacheable(value = ["token-user"], key = "#token")
    fun extractUsername(token: String): @Nullable String? {
        val claims = verifyToken(token) ?: return null
        return claims["sub"] as? String
    }
    
    /**
     * 从 Token 中提取用户 ID
     */
    @Cacheable(value = ["token-user-id"], key = "#token")
    fun extractUserId(token: String): @Nullable String? {
        val claims = verifyToken(token) ?: return null
        return claims["userId"] as? String
    }
    
    // ==================== 安全策略管理 ====================
    
    /**
     * 获取当前安全等级
     */
    fun getSecurityLevel(): SecurityLevel {
        return preferenceService.getSecurityLevel()
    }
    
    /**
     * 设置安全等级
     */
    fun setSecurityLevel(level: SecurityLevel) {
        logger.info("设置安全等级: $level")
        preferenceService.setSecurityLevel(level)
    }
    
    /**
     * 获取安全策略
     */
    @Cacheable(value = ["security-policy"])
    fun getSecurityPolicy(): SecurityPolicy {
        val level = getSecurityLevel()
        return SecurityPolicy.forLevel(level)
    }
    
    /**
     * 检查权限
     */
    fun checkPermission(action: String, resource: String? = null): Boolean {
        val policy = getSecurityPolicy()
        return policy.allows(action, resource)
    }
    
    // ==================== 会话管理 ====================
    
    /**
     * 创建会话
     */
    fun createSession(userId: String, deviceInfo: String? = null): String {
        logger.debug("创建会话: userId=$userId")
        
        val token = generateToken(userId, mapOf(
            "userId" to userId,
            "deviceInfo" to (deviceInfo ?: "unknown"),
            "createdAt" to LocalDateTime.now().toString()
        ))
        
        return token
    }
    
    /**
     * 销毁会话（将 Token 加入黑名单）
     */
    fun destroySession(token: String): Boolean {
        logger.debug("销毁会话")
        
        // 简化实现：实际应使用 Redis 黑名单
        // 这里暂时不做处理
        return true
    }
    
    /**
     * 检查会话是否有效
     */
    fun isSessionValid(token: String): Boolean {
        return verifyToken(token) != null
    }
    
    // ==================== 哈希工具 ====================
    
    /**
     * 生成 SHA-256 哈希
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成 SHA-512 哈希
     */
    fun sha512(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-512").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成 MD5 哈希（仅用于非安全场景）
     */
    fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    // ==================== 随机数生成 ====================
    
    /**
     * 生成随机字符串
     */
    fun generateRandomString(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * 生成 UUID
     */
    fun generateUUID(): String {
        return java.util.UUID.randomUUID().toString()
    }
    
    /**
     * 生成验证码
     */
    fun generateCaptcha(length: Int = 6): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取 JWT 密钥
     */
    private fun getJwtSecret(): String {
        val prefSecret = preferenceService.getJwtSecret()
        return if (prefSecret.isNotEmpty()) {
            prefSecret
        } else {
            jwtSecret
        }
    }
    
    /**
     * 获取 JWT 过期时间
     */
    private fun getJwtExpiration(): Long {
        val prefExpiration = preferenceService.getJwtExpiration()
        return if (prefExpiration > 0) {
            prefExpiration
        } else {
            jwtExpiration
        }
    }
    
    /**
     * 生成 JWT 签名（简化版）
     */
    private fun generateSignature(data: String, secret: String): String {
        val key = secret.toByteArray()
        val dataBytes = data.toByteArray()
        
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
        val signature = mac.doFinal(dataBytes)
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }
}
