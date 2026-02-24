package com.moyue.security
 
import com.moyue.security.SecurityLevel
import org.slf4j.LoggerFactory
 
/**
 * 安全策略
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 安全策略说明：
 * - 根据安全等级定义权限和行为限制
 * - 控制书源脚本的访问范围
 * - 防止恶意代码执行
 *
 * @author Moyue Team
 * @since 4.0.3
 */
data class SecurityPolicy(
    /**
     * 安全等级
     */
    val securityLevel: SecurityLevel,
    
    /**
     * 是否允许网络访问
     */
    val allowsNetwork: Boolean = securityLevel.allowsNetwork,
    
    /**
     * 是否允许文件系统访问
     */
    val allowsFileSystem: Boolean = securityLevel.allowsFileSystem,
    
    /**
     * 是否允许系统命令执行
     */
    val allowsSystem: Boolean = securityLevel.allowsSystem,
    
    /**
     * 是否允许反射
     */
    val allowsReflection: Boolean = securityLevel.allowsReflection,
    
    /**
     * 是否允许原生代码加载
     */
    val allowsNative: Boolean = securityLevel.allowsNative,
    
    /**
     * 最大执行时间（秒）
     */
    val maxExecutionTime: Long = securityLevel.maxExecutionTime,
    
    /**
     * 是否启用沙箱
     */
    val sandboxEnabled: Boolean = securityLevel.sandboxEnabled,
    
    /**
     * 允许的网络域名白名单
     */
    val allowedDomains: Set<String> = emptySet(),
    
    /**
     * 禁止的网络域名黑名单
     */
    val blockedDomains: Set<String> = setOf(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "169.254.169.254"  // AWS 元数据服务
    ),
    
    /**
     * 允许的文件路径白名单
     */
    val allowedPaths: Set<String> = emptySet(),
    
    /**
     * 禁止的文件路径黑名单
     */
    val blockedPaths: Set<String> = setOf(
        "/",
        "/etc",
        "/sys",
        "/proc",
        "/root",
        "/home",
        "C:\\",
        "C:\\Windows",
        "C:\\Program Files"
    ),
    
    /**
     * 允许的 Java 类白名单
     */
    val allowedClasses: Set<String> = setOf(
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.util.Map",
        "java.util.List",
        "java.util.ArrayList",
        "java.util.HashMap",
        "java.util.regex.Pattern",
        "java.util.regex.Matcher",
        "java.text.SimpleDateFormat",
        "java.net.URL",
        "java.net.URI",
        "java.net.URLEncoder",
        "java.net.URLDecoder",
        "javax.crypto.*",
        "java.security.*"
    ),
    
    /**
     * 禁止的 Java 类黑名单
     */
    val blockedClasses: Set<String> = setOf(
        "java.lang.System",
        "java.lang.Runtime",
        "java.lang.ProcessBuilder",
        "java.lang.ClassLoader",
        "java.io.File",
        "java.nio.file.*",
        "java.lang.reflect.*",
        "sun.misc.Unsafe",
        "java.lang.Thread",
        "java.lang.ThreadGroup",
        "java.util.concurrent.*"
    ),
    
    /**
     * 最大内存使用（MB）
     */
    val maxMemoryMb: Long = 256,
    
    /**
     * 最大递归深度
     */
    val maxRecursionDepth: Int = 100,
    
    /**
     * 最大字符串长度
     */
    val maxStringLength: Int = 10_000_000
) {
    
    private val logger = LoggerFactory.getLogger(SecurityPolicy::class.java)
    
    companion object {
        /**
         * 根据安全等级创建策略
         */
        fun forLevel(level: SecurityLevel): SecurityPolicy {
            return when (level) {
                SecurityLevel.TRUSTED -> SecurityPolicy(
                    securityLevel = level,
                    allowedDomains = emptySet(),
                    blockedDomains = emptySet(),
                    allowedPaths = emptySet(),
                    blockedPaths = emptySet(),
                    allowedClasses = emptySet(),
                    blockedClasses = emptySet()
                )
                SecurityLevel.STANDARD -> SecurityPolicy(
                    securityLevel = level,
                    allowedDomains = setOf("*"),  // 允许所有域名
                    blockedDomains = setOf(
                        "localhost",
                        "127.0.0.1",
                        "0.0.0.0"
                    ),
                    allowedPaths = emptySet(),
                    blockedPaths = setOf(
                        "/",
                        "/etc",
                        "/sys",
                        "/proc"
                    )
                )
                SecurityLevel.COMPATIBLE -> SecurityPolicy(
                    securityLevel = level,
                    allowedDomains = setOf("*"),
                    blockedDomains = emptySet(),
                    allowedPaths = setOf(
                        System.getProperty("java.io.tmpdir") ?: "/tmp"
                    ),
                    blockedPaths = setOf(
                        "/",
                        "/etc",
                        "/sys",
                        "/proc"
                    )
                )
            }
        }
    }
    
    /**
     * 检查是否允许执行操作
     */
    fun allows(action: String, resource: String? = null): Boolean {
        return when (action.lowercase()) {
            "network" -> allowsNetwork && checkNetworkAccess(resource)
            "filesystem" -> allowsFileSystem && checkFileSystemAccess(resource)
            "system" -> allowsSystem
            "reflection" -> allowsReflection
            "native" -> allowsNative
            "class" -> checkClassAccess(resource)
            else -> true
        }
    }
    
    /**
     * 检查网络访问权限
     */
    private fun checkNetworkAccess(url: String?): Boolean {
        if (url == null || !allowsNetwork) {
            return false
        }
        
        try {
            val uri = java.net.URI(url)
            val host = uri.host ?: return false
            
            // 检查黑名单
            if (blockedDomains.any { host == it || host.endsWith(".$it") }) {
                logger.warn("访问被拒绝（黑名单）: $url")
                return false
            }
            
            // 检查白名单
            if (allowedDomains.isNotEmpty()) {
                val allowed = allowedDomains.any {
                    it == "*" || host == it || host.endsWith(".$it")
                }
                if (!allowed) {
                    logger.warn("访问被拒绝（白名单）: $url")
                    return false
                }
            }
            
            return true
        } catch (e: Exception) {
            logger.error("检查网络访问失败: $url", e)
            return false
        }
    }
    
    /**
     * 检查文件系统访问权限
     */
    private fun checkFileSystemAccess(path: String?): Boolean {
        if (path == null || !allowsFileSystem) {
            return false
        }
        
        // 检查黑名单
        if (blockedPaths.any { path.startsWith(it) }) {
            logger.warn("文件访问被拒绝（黑名单）: $path")
            return false
        }
        
        // 检查白名单
        if (allowedPaths.isNotEmpty()) {
            val allowed = allowedPaths.any { path.startsWith(it) }
            if (!allowed) {
                logger.warn("文件访问被拒绝（白名单）: $path")
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查类访问权限
     */
    private fun checkClassAccess(className: String?): Boolean {
        if (className == null) {
            return false
        }
        
        // 检查黑名单
        if (blockedClasses.any { className.startsWith(it.removeSuffix("*")) }) {
            logger.warn("类访问被拒绝（黑名单）: $className")
            return false
        }
        
        // 检查白名单
        if (allowedClasses.isNotEmpty()) {
            val allowed = allowedClasses.any { className.startsWith(it.removeSuffix("*")) }
            if (!allowed) {
                logger.warn("类访问被拒绝（白名单）: $className")
                return false
            }
        }
        
        return true
    }
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }
    
    companion object {
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): SecurityPolicy {
            return com.google.gson.Gson().fromJson(json, SecurityPolicy::class.java)
        }
    }
}
