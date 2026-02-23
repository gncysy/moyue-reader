package com.moyue.service
 
import com.moyue.engine.RhinoEngine
import com.moyue.model.BookSource
import com.moyue.security.SecurityLevel
import mu.KotlinLogging
 
/**
 * 安全服务
 * 管理书源的安全策略和 JavaScript 执行
 */
class SecurityService(
    private val rhinoEngine: RhinoEngine,
    private val cacheService: CacheService
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 默认安全级别
     */
    private var defaultSecurityLevel: SecurityLevel = SecurityLevel.STANDARD
    
    /**
     * 设置默认安全级别
     */
    fun setDefaultSecurityLevel(level: SecurityLevel) {
        this.defaultSecurityLevel = level
        logger.info { "设置默认安全级别: $level" }
    }
    
    /**
     * 获取默认安全级别
     */
    fun getDefaultSecurityLevel(): SecurityLevel = defaultSecurityLevel
    
    /**
     * 执行书源规则（带安全沙箱）
     */
    fun executeRule(
        source: BookSource,
        rule: String,
        context: Map<String, Any>
    ): Any? {
        val securityLevel = determineSecurityLevel(source)
        
        logger.debug { "执行规则 - 书源: ${source.name}, 安全级别: $securityLevel" }
        
        return when (securityLevel) {
            SecurityLevel.TRUSTED -> executeTrusted(rule, context)
            SecurityLevel.COMPATIBLE -> executeCompatible(rule, context)
            SecurityLevel.STANDARD -> executeStandard(rule, context)
        }
    }
    
    /**
     * 确定安全级别
     */
    private fun determineSecurityLevel(source: BookSource): SecurityLevel {
        // 优先使用书源配置的安全级别
        return when (source.securityRating) {
            1 -> SecurityLevel.TRUSTED
            2 -> SecurityLevel.COMPATIBLE
            else -> SecurityLevel.STANDARD
        }
    }
    
    /**
     * 信任级别执行（无限制）
     */
    private fun executeTrusted(rule: String, context: Map<String, Any>): Any? {
        return try {
            rhinoEngine.evaluate(rule, context)
        } catch (e: Exception) {
            logger.error(e) { "信任级别执行失败" }
            throw e
        }
    }
    
    /**
     * 兼容级别执行（适度限制）
     */
    private fun executeCompatible(rule: String, context: Map<String, Any>): Any? {
        // 缓存白名单检查
        val cacheKey = "security:compatible:${rule.hashCode()}"
        return cacheService.getOrCompute(cacheKey) {
            rhinoEngine.evaluateWithSandbox(rule, context, compatibleMode = true)
        }
    }
    
    /**
     * 标准级别执行（严格限制）
     */
    private fun executeStandard(rule: String, context: Map<String, Any>): Any? {
        return try {
            rhinoEngine.evaluateWithSandbox(rule, context, compatibleMode = false)
        } catch (e: Exception) {
            logger.error(e) { "标准级别执行失败" }
            null
        }
    }
    
    /**
     * 验证书源规则安全性
     */
    fun validateRule(rule: String): Boolean {
        val dangerousKeywords = listOf(
            "System.exit",
            "Runtime.exec",
            "ProcessBuilder",
            "java.io.File",
            "Class.forName"
        )
        
        return dangerousKeywords.none { rule.contains(it) }
    }
    
    /**
     * 获取安全统计
     */
    fun getSecurityStats(): Map<String, Any> {
        return mapOf(
            "defaultLevel" to defaultSecurityLevel.name,
            "cacheStats" to cacheService.getStats()
        )
    }
}
