package com.moyue.controller
 
import com.moyue.security.SecurityLevel
import com.moyue.service.SecurityService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
 
/**
 * 安全控制器
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 用户认证（登录/登出）
 * - Token 管理（生成/刷新/验证）
 * - 密码管理（修改/重置）
 * - 安全策略管理
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@RestController
@RequestMapping("/api/security")
class SecurityController(
    private val securityService: SecurityService
) {
    
    private val logger = LoggerFactory.getLogger(SecurityController::class.java)
    
    // ==================== 用户认证 ====================
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<BookController.ApiResponse<LoginResponse>> {
        logger.info("用户登录: ${request.username}")
        
        // 简化实现：实际应从数据库验证用户
        // 这里使用硬编码的默认用户
        val validUser = request.username == "admin" && request.password == "admin123"
        
        return if (validUser) {
            val token = securityService.createSession(
                userId = request.username,
                deviceInfo = request.deviceInfo
            )
            
            val response = LoginResponse(
                token = token,
                tokenType = "Bearer",
                expiresIn = 86400,
                user = UserInfo(
                    id = request.username,
                    username = request.username,
                    roles = listOf("ADMIN")
                )
            )
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = response,
                    message = "登录成功"
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                BookController.ApiResponse.error(
                    code = "INVALID_CREDENTIALS",
                    message = "用户名或密码错误"
                )
            )
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") authorization: String?): ResponseEntity<BookController.ApiResponse<Unit>> {
        val token = authorization?.removePrefix("Bearer ")
        
        if (token != null) {
            logger.info("用户登出")
            securityService.destroySession(token)
        }
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                message = "登出成功"
            )
        )
    }
    
    // ==================== Token 管理 ====================
    
    /**
     * 刷新 Token
     */
    @PostMapping("/token/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<BookController.ApiResponse<LoginResponse>> {
        logger.debug("刷新 Token")
        
        val newToken = securityService.refreshToken(request.token)
        
        return if (newToken != null) {
            val response = LoginResponse(
                token = newToken,
                tokenType = "Bearer",
                expiresIn = 86400,
                user = null
            )
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = response,
                    message = "刷新成功"
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                BookController.ApiResponse.error(
                    code = "INVALID_TOKEN",
                    message = "Token 无效或已过期"
                )
            )
        }
    }
    
    /**
     * 验证 Token
     */
    @PostMapping("/token/verify")
    fun verifyToken(@RequestBody request: VerifyTokenRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("验证 Token")
        
        val claims = securityService.verifyToken(request.token)
        
        return if (claims != null) {
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "valid" to true,
                        "claims" to claims
                    ),
                    message = "Token 有效"
                )
            )
        } else {
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "valid" to false,
                        "claims" to null
                    ),
                    message = "Token 无效"
                )
            )
        }
    }
    
    // ==================== 密码管理 ====================
    
    /**
     * 修改密码
     */
    @PostMapping("/password/change")
    fun changePassword(@RequestBody @Valid request: ChangePasswordRequest): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("修改密码")
        
        // 验证旧密码
        val validPassword = request.oldPassword == "admin123"
        
        return if (validPassword) {
            // 实际应加密并存储新密码
            val hashedPassword = securityService.encryptPassword(request.newPassword)
            logger.debug("新密码已加密: ${hashedPassword.take(10)}...")
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    message = "密码修改成功"
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BookController.ApiResponse.error(
                    code = "INVALID_PASSWORD",
                    message = "旧密码错误"
                )
            )
        }
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/password/reset")
    fun resetPassword(@RequestBody @Valid request: ResetPasswordRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.info("重置密码: ${request.username}")
        
        // 生成随机新密码
        val newPassword = securityService.generateRandomPassword(16)
        
        // 实际应通过邮件或短信发送新密码
        logger.info("为用户 ${request.username} 生成新密码: $newPassword")
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf(
                    "username" to request.username,
                    "newPassword" to newPassword
                ),
                message = "密码已重置"
            )
        )
    }
    
    /**
     * 生成随机密码
     */
    @GetMapping("/password/generate")
    fun generatePassword(
        @RequestParam(defaultValue = "16") length: Int
    ): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("生成随机密码: length=$length")
        
        val password = securityService.generateRandomPassword(length.coerceIn(8, 32))
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf(
                    "password" to password,
                    "length" to password.length
                ),
                message = "生成成功"
            )
        )
    }
    
    // ==================== 安全策略管理 ====================
    
    /**
     * 获取安全等级
     */
    @GetMapping("/policy/level")
    fun getSecurityLevel(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取安全等级")
        
        val level = securityService.getSecurityLevel()
        val policy = securityService.getSecurityPolicy()
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf(
                    "level" to level.name.lowercase(),
                    "description" to level.description,
                    "policy" to mapOf(
                        "allowsNetwork" to policy.allowsNetwork,
                        "allowsFileSystem" to policy.allowsFileSystem,
                        "allowsSystem" to policy.allowsSystem,
                        "maxExecutionTime" to policy.maxExecutionTime,
                        "sandboxEnabled" to policy.sandboxEnabled
                    )
                ),
                message = "获取成功"
            )
        )
    }
    
    /**
     * 设置安全等级
     */
    @PutMapping("/policy/level")
    fun setSecurityLevel(@RequestBody @Valid request: SetSecurityLevelRequest): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("设置安全等级: ${request.level}")
        
        return try {
            val level = SecurityLevel.valueOf(request.level.uppercase())
            securityService.setSecurityLevel(level)
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    message = "安全等级已设置为 ${level.name}"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BookController.ApiResponse.error(
                    code = "INVALID_SECURITY_LEVEL",
                    message = "无效的安全等级: ${request.level}"
                )
            )
        }
    }
    
    /**
     * 检查权限
     */
    @PostMapping("/policy/check")
    fun checkPermission(@RequestBody @Valid request: CheckPermissionRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("检查权限: action=${request.action}, resource=${request.resource}")
        
        val allowed = securityService.checkPermission(request.action, request.resource)
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf(
                    "allowed" to allowed,
                    "action" to request.action,
                    "resource" to request.resource
                ),
                message = if (allowed) "允许访问" else "拒绝访问"
            )
        )
    }
    
    // ==================== 哈希工具 ====================
    
    /**
     * 生成哈希
     */
    @PostMapping("/hash")
    fun generateHash(@RequestBody @Valid request: HashRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("生成哈希: algorithm=${request.algorithm}")
        
        val hash = when (request.algorithm.lowercase()) {
            "md5" -> securityService.md5(request.input)
            "sha256" -> securityService.sha256(request.input)
            "sha512" -> securityService.sha512(request.input)
            else -> throw IllegalArgumentException("不支持的算法: ${request.algorithm}")
        }
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf(
                    "algorithm" to request.algorithm,
                    "input" to request.input,
                    "hash" to hash
                ),
                message = "哈希生成成功"
            )
        )
    }
    
    /**
     * 生成随机数
     */
    @GetMapping("/random")
    fun generateRandom(
        @RequestParam(defaultValue = "string") type: String,
        @RequestParam(defaultValue = "32") length: Int
    ): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("生成随机数: type=$type, length=$length")
        
        val result = when (type.lowercase()) {
            "string" -> securityService.generateRandomString(length)
            "uuid" -> securityService.generateUUID()
            "captcha" -> securityService.generateCaptcha(length.coerceIn(4, 10))
            else -> throw IllegalArgumentException("不支持的类型: $type")
        }
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf(
                    "type" to type,
                    "result" to result
                ),
                message = "生成成功"
            )
        )
    }
    
    // ==================== 请求/响应 DTO ====================
    
    /**
     * 登录请求
     */
    data class LoginRequest(
        @field:NotBlank(message = "用户名不能为空")
        val username: String,
        
        @field:NotBlank(message = "密码不能为空")
        val password: String,
        
        val deviceInfo: String? = null
    )
    
    /**
     * 登录响应
     */
    data class LoginResponse(
        val token: String,
        val tokenType: String,
        val expiresIn: Long,
        val user: @Nullable UserInfo?
    )
    
    /**
     * 用户信息
     */
    data class UserInfo(
        val id: String,
        val username: String,
        val roles: List<String>
    )
    
    /**
     * 刷新 Token 请求
     */
    data class RefreshTokenRequest(
        val token: String
    )
    
    /**
     * 验证 Token 请求
     */
    data class VerifyTokenRequest(
        val token: String
    )
    
    /**
     * 修改密码请求
     */
    data class ChangePasswordRequest(
        @field:NotBlank(message = "旧密码不能为空")
        val oldPassword: String,
        
        @field:NotBlank(message = "新密码不能为空")
        val newPassword: String
    )
    
    /**
     * 重置密码请求
     */
    data class ResetPasswordRequest(
        @field:NotBlank(message = "用户名不能为空")
        val username: String
    )
    
    /**
     * 设置安全等级请求
     */
    data class SetSecurityLevelRequest(
        @field:NotBlank(message = "安全等级不能为空")
        val level: String  // standard, compatible, trusted
    )
    
    /**
     * 检查权限请求
     */
    data class CheckPermissionRequest(
        @field:NotBlank(message = "操作不能为空")
        val action: String,
        
        val resource: String? = null
    )
    
    /**
     * 生成哈希请求
     */
    data class HashRequest(
        @field:NotBlank(message = "输入不能为空")
        val input: String,
        
        @field:NotBlank(message = "算法不能为空")
        val algorithm: String  // md5, sha256, sha512
    )
}
