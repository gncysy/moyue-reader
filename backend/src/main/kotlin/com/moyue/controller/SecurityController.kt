package com.moyue.controller

import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import com.moyue.service.SecurityService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/security")
class SecurityController(
    private val securityService: SecurityService
) {
    
    private val logger = LoggerFactory.getLogger(SecurityController::class.java)
    
    /**
     * 获取当前安全策略
     */
    @GetMapping("/policy")
    fun getCurrentPolicy(): SecurityPolicy {
        return securityService.getCurrentPolicy()
    }
    
    /**
     * 获取策略摘要
     */
    @GetMapping("/policy/summary")
    fun getPolicySummary(): Map<String, Any> {
        val policy = securityService.getCurrentPolicy()
        return policy.getSummary().toMap()
    }
    
    /**
     * 切换安全模式
     */
    @PostMapping("/policy")
    fun setPolicy(
        @RequestParam level: String,
        @RequestParam(required = false) confirmPassword: String?
    ): ResponseEntity<Map<String, Any>> {
        val newLevel = try {
            SecurityLevel.valueOf(level.uppercase())
        } catch (e: Exception) {
            logger.warn("无效的安全级别: $level")
            return ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "无效的安全级别: $level",
                    "availableLevels" to SecurityLevel.values().map { it.name }
                )
            )
        }
        
        // 信任模式需要用户确认
        if (newLevel == SecurityLevel.TRUSTED && (confirmPassword.isNullOrBlank() || confirmPassword.length < 8)) {
            logger.warn("信任模式需要确认密码")
            return ResponseEntity.ok().body(
                mapOf(
                    "success" to false,
                    "requiresConfirmation" to true,
                    "message" to "信任模式会关闭所有安全限制，请输入确认密码（至少8位）"
                )
            )
        }
        
        return try {
            val policy = securityService.setPolicy(newLevel, confirmPassword)
            logger.info("安全策略已切换: ${policy.level.name}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "policy" to policy,
                    "message" to "安全策略已切换到 ${policy.level.displayName}"
                )
            )
        } catch (e: Exception) {
            logger.error("切换安全策略失败", e)
            ResponseEntity.status(500).body(
                mapOf(
                    "success" to false,
                    "message" to "切换失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 获取所有可用安全级别
     */
    @GetMapping("/levels")
    fun getSecurityLevels(): List<Map<String, Any>> {
        return SecurityLevel.values().map { level ->
            mapOf(
                "name" to level.name,
                "displayName" to level.displayName,
                "description" to level.description,
                "riskLevel" to level.riskLevel,
                "canUpgrade" to level.canUpgrade(),
                "canDowngrade" to level.canDowngrade(),
                "features" to getLevelFeatures(level)
            )
        }
    }
    
    /**
     * 获取安全日志
     */
    @GetMapping("/logs")
    fun getSecurityLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) level: String?
    ): Map<String, Any> {
        val securityLevel = level?.let { 
            try { SecurityLevel.valueOf(it.uppercase()) } 
            catch (e: Exception) { null }
        }
        
        val logs = securityService.getSecurityLogs(page, size, securityLevel)
        val stats = securityService.getSecurityLogStats()
        
        return mapOf(
            "page" to page,
            "size" to size,
            "total" to stats.totalLogs,
            "logs" to logs
        )
    }
    
    /**
     * 清除安全日志
     */
    @DeleteMapping("/logs")
    fun clearSecurityLogs(): Map<String, Any> {
        val count = securityService.clearSecurityLogs()
        logger.info("清除安全日志: $count 条")
        return mapOf(
            "success" to true,
            "message" to "已清除 $count 条日志"
        )
    }
    
    /**
     * 导出安全日志
     */
    @GetMapping("/logs/export")
    fun exportSecurityLogs(): ResponseEntity<Map<String, String>> {
        val filePath = securityService.exportLogs()
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "filePath" to filePath
            )
        )
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
    ): ResponseEntity<Map<String, Any>> {
        val testLevel = level?.let { 
            try { SecurityLevel.valueOf(it.uppercase()) } 
            catch (e: Exception) { null }
        }
        
        val result = securityService.testSourceCompatibility(sourceId, testLevel)
        
        return ResponseEntity.ok(
            mapOf(
                "sourceId" to result.sourceId,
                "testLevel" to result.testLevel,
                "compatible" to result.compatible,
                "warnings" to result.warnings,
                "recommendedAction" to result.recommendedAction
            )
        )
    }
    
    /**
     * 获取安全建议
     */
    @GetMapping("/recommendations")
    fun getSecurityRecommendations(): List<Map<String, String>> {
        val currentLevel = securityService.getCurrentPolicy().level
        
        return listOf(
            mapOf(
                "level" to SecurityLevel.STANDARD.name,
                "title" to "推荐使用标准模式",
                "description" to "大多数书源可在标准模式下运行，安全性最高",
                "recommended" to (currentLevel == SecurityLevel.STANDARD)
            ),
            mapOf(
                "level" to SecurityLevel.COMPATIBLE.name,
                "title" to "特殊书源需兼容模式",
                "description" to "如需使用文件缓存或特殊协议的书源，请切换到兼容模式",
                "recommended" to (currentLevel == SecurityLevel.COMPATIBLE)
            ),
            mapOf(
                "level" to SecurityLevel.TRUSTED.name,
                "title" to "信任模式仅限开发者",
                "description" to "此模式会关闭所有安全限制，请确保书源可信",
                "recommended" to (currentLevel == SecurityLevel.TRUSTED)
            )
        )
    }
    
    /**
     * 获取当前模式下的限制说明
     */
    @GetMapping("/restrictions")
    fun getCurrentRestrictions(): Map<String, Any> {
        val policy = securityService.getCurrentPolicy()
        
        return mapOf(
            "level" to policy.level.name,
            "displayName" to policy.level.displayName,
            "timeoutMs" to policy.timeoutMs,
            "maxFileSize" to "${policy.maxFileSize / 1024 / 1024}MB",
            "maxHttpRequests" to policy.maxHttpRequests,
            "allowed" to getLevelFeatures(policy.level),
            "sandboxRoot" to policy.sandboxRoot,
            "blockedDomains" to policy.blockedDomains
        )
    }
    
    /**
     * 获取策略变更历史
     */
    @GetMapping("/history")
    fun getPolicyHistory(): List<Map<String, Any>> {
        val logs = securityService.getSecurityLogs(0, 20, null)
        return logs
            .filter { it.action == "切换安全模式" }
            .map { log ->
                mapOf(
                    "timestamp" to log.timestamp,
                    "level" to log.level.name,
                    "source" to log.source,
                    "result" to log.result
                )
            }
    }
    
    // ==================== 私有方法 ====================
    
    private fun getLevelFeatures(level: SecurityLevel): List<String> {
        return when (level) {
            SecurityLevel.STANDARD -> listOf(
                "基础网络请求 (GET/POST)",
                "HTML/DOM 解析",
                "基础加密编码 (MD5/Base64)"
            )
            SecurityLevel.COMPATIBLE -> listOf(
                "标准模式所有功能",
                "文件读写 (限沙箱目录)",
                "网络 Socket",
                "Cookie 管理",
                "更灵活的重定向控制"
            )
            SecurityLevel.TRUSTED -> listOf(
                "兼容模式所有功能",
                "Java 反射调用",
                "内网地址访问",
                "系统进程执行",
                "关闭大部分安全限制"
            )
        }
    }
}
