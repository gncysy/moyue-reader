package com.moyue.controller

import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import com.moyue.service.SecurityService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/security")
class SecurityController(
    private val securityService: SecurityService
) {

    /**
     * 获取当前安全策略
     */
    @GetMapping("/policy")
    fun getCurrentPolicy(): SecurityPolicy {
        return securityService.getCurrentPolicy()
    }

    /**
     * 切换安全模式
     */
    @PostMapping("/policy")
    fun setPolicy(
        @RequestParam level: String,
        @RequestParam(required = false) confirmed: Boolean
    ): ResponseEntity<Map<String, Any>> {
        val newLevel = try {
            SecurityLevel.valueOf(level.uppercase())
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "无效的安全级别")
            )
        }

        // 信任模式需要用户确认
        if (newLevel == SecurityLevel.TRUSTED && confirmed != true) {
            return ResponseEntity.ok().body(
                mapOf(
                    "success" to false,
                    "requiresConfirmation" to true,
                    "message" to "信任模式会关闭所有安全限制，请确认"
                )
            )
        }

        val policy = securityService.setPolicy(newLevel, confirmed == true)
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "policy" to policy
            )
        )
    }

    /**
     * 获取所有可用安全级别
     */
    @GetMapping("/levels")
    fun getSecurityLevels(): List<Map<String, Any>> {
        return SecurityLevel.values().map { level ->
            mapOf(
                "name" to level.name,
                "displayName" to when (level) {
                    SecurityLevel.STANDARD -> "标准模式"
                    SecurityLevel.COMPATIBLE -> "兼容模式"
                    SecurityLevel.TRUSTED -> "信任模式"
                },
                "description" to when (level) {
                    SecurityLevel.STANDARD -> "禁止文件/Socket/反射，适合日常阅读"
                    SecurityLevel.COMPATIBLE -> "允许文件/Socket，禁止反射，适合特殊书源"
                    SecurityLevel.TRUSTED -> "仅超时保护，需手动确认，适合开发者调试"
                },
                "allowFile" to when (level) {
                    SecurityLevel.STANDARD -> false
                    SecurityLevel.COMPATIBLE -> true
                    SecurityLevel.TRUSTED -> true
                },
                "allowSocket" to when (level) {
                    SecurityLevel.STANDARD -> false
                    SecurityLevel.COMPATIBLE -> true
                    SecurityLevel.TRUSTED -> true
                },
                "allowReflection" to when (level) {
                    SecurityLevel.STANDARD -> false
                    SecurityLevel.COMPATIBLE -> false
                    SecurityLevel.TRUSTED -> true
                }
            )
        }
    }

    /**
     * 获取安全日志
     */
    @GetMapping("/logs")
    fun getSecurityLogs(
        @RequestParam(defaultValue = "50") limit: Int
    ): List<Map<String, Any>> {
        return securityService.getSecurityLogs(limit)
    }

    /**
     * 清除安全日志
     */
    @DeleteMapping("/logs")
    fun clearSecurityLogs(): Map<String, Any> {
        securityService.clearSecurityLogs()
        return mapOf("success" to true, "message" to "日志已清除")
    }

    /**
     * 获取沙箱状态
     */
    @GetMapping("/sandbox/status")
    fun getSandboxStatus(): Map<String, Any> {
        return securityService.getSandboxStatus()
    }

    /**
     * 测试书源在当前模式下的兼容性
     */
    @PostMapping("/test-source")
    fun testSourceCompatibility(
        @RequestParam sourceId: String,
        @RequestParam(required = false) level: String?
    ): Map<String, Any> {
        val testLevel = level?.let { 
            try { SecurityLevel.valueOf(it.uppercase()) } 
            catch (e: Exception) { null }
        }
        
        return securityService.testSourceCompatibility(sourceId, testLevel)
    }

    /**
     * 获取安全建议
     */
    @GetMapping("/recommendations")
    fun getSecurityRecommendations(): List<Map<String, String>> {
        return listOf(
            mapOf(
                "level" to "STANDARD",
                "title" to "推荐使用标准模式",
                "description" to "大多数书源可在标准模式下运行，安全性最高"
            ),
            mapOf(
                "level" to "COMPATIBLE",
                "title" to "特殊书源需兼容模式",
                "description" to "如需使用文件缓存或特殊协议的书源，请切换到兼容模式"
            ),
            mapOf(
                "level" to "TRUSTED",
                "title" to "信任模式仅限开发者",
                "description" to "此模式会关闭所有安全限制，请确保书源可信"
            )
        )
    }

    /**
     * 获取当前模式下的限制说明
     */
    @GetMapping("/restrictions")
    fun getCurrentRestrictions(): Map<String, Any> {
        val policy = securityService.getCurrentPolicy()
        
        val restricted = mutableListOf<String>()
        val allowed = mutableListOf<String>()
        
        if (!policy.allowFile) {
            restricted.add("文件读写")
        } else {
            allowed.add("文件读写（限沙箱目录）")
        }
        
        if (!policy.allowSocket) {
            restricted.add("Socket连接")
        } else {
            allowed.add("Socket连接（限非特权端口）")
        }
        
        if (!policy.allowReflection) {
            restricted.add("反射调用")
        } else {
            allowed.add("反射调用")
        }
        
        return mapOf(
            "level" to policy.level.name,
            "timeoutMs" to policy.timeoutMs,
            "restricted" to restricted,
            "allowed" to allowed,
            "sandboxRoot" to policy.sandboxRoot
        )
    }
}
