package com.moyue.security

enum class SecurityLevel(val displayName: String, val description: String, val riskLevel: Int) {
    STANDARD("标准模式", "最安全，仅允许基本网络请求和 DOM 操作", 0),
    COMPATIBLE("兼容模式", "允许文件操作和 Socket，中等安全", 1),
    TRUSTED("信任模式", "允许反射等高级功能，最低安全", 2);
    
    companion object {
        fun fromName(name: String): SecurityLevel? {
            return values().find { it.name.equals(name, ignoreCase = true) }
        }
        
        fun isHigherThan(level1: SecurityLevel, level2: SecurityLevel): Boolean {
            return level1.riskLevel > level2.riskLevel
        }
    }
    
    fun canUpgrade(): Boolean {
        return this != TRUSTED
    }
    
    fun canDowngrade(): Boolean {
        return this != STANDARD
    }
}

// 文件二：SecurityPolicy.kt
package com.moyue.security

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.HashSet

data class SecurityPolicy(
    val level: SecurityLevel,
    val allowFile: Boolean = false,
    val allowSocket: Boolean = false,
    val allowReflection: Boolean = false,
    val allowProcess: Boolean = false,
    val timeoutMs: Long = 30000,
    val maxStackDepth: Int = 10000,
    val maxHttpConnections: Int = 5,
    val maxFileSize: Long = 10 * 1024 * 1024,
    val maxHttpRequests: Int = 100,
    val allowInternalNetwork: Boolean = false,
    val allowRedirects: Boolean = true,
    val sandboxRoot: String = getDefaultSandboxRoot(),
    val allowedFileExtensions: Set<String> = defaultAllowedExtensions(),
    val blockedDomains: Set<String> = emptySet(),
    val blockedFilePatterns: Set<String> = setOf(".exe", ".dll", ".so", ".dylib", ".sh", ".bat", ".cmd", ".ps1", ".vbs", ".js")
) {
    
    init {
        validateParameters()
        try {
            ensureSandboxDirectory()
        } catch (e: Exception) {
            throw SecurityException("沙箱目录初始化失败: ${e.message}", e)
        }
    }
    
    companion object {
        private const val DEFAULT_SANDBOX_DIR = ".moyue-reader/sandbox"
        
        fun getDefaultSandboxRoot(): String {
            return Paths.get(System.getProperty("user.home"), DEFAULT_SANDBOX_DIR).normalize().toString()
        }
        
        fun defaultAllowedExtensions(): Set<String> {
            return setOf(".txt", ".json", ".xml", ".html", ".htm", ".css", ".js", ".jpg", ".png", ".gif", ".pdf", ".epub")
        }
        
        fun fromLevel(level: SecurityLevel): SecurityPolicy {
            return when (level) {
                SecurityLevel.STANDARD -> standard()
                SecurityLevel.COMPATIBLE -> compatible()
                SecurityLevel.TRUSTED -> trusted()
            }
        }
        
        fun standard(): SecurityPolicy {
            return SecurityPolicy(
                level = SecurityLevel.STANDARD,
                allowFile = false,
                allowSocket = false,
                allowReflection = false,
                allowProcess = false,
                timeoutMs = 15000,
                maxStackDepth = 5000,
                maxHttpConnections = 3,
                maxFileSize = 5 * 1024 * 1024,
                maxHttpRequests = 50,
                allowInternalNetwork = false,
                allowRedirects = false
            )
        }
        
        fun compatible(): SecurityPolicy {
            return SecurityPolicy(
                level = SecurityLevel.COMPATIBLE,
                allowFile = true,
                allowSocket = true,
                allowReflection = false,
                allowProcess = false,
                timeoutMs = 30000,
                maxStackDepth = 10000,
                maxHttpConnections = 5,
                maxFileSize = 10 * 1024 * 1024,
                maxHttpRequests = 100,
                allowInternalNetwork = false,
                allowRedirects = true
            )
        }
        
        fun trusted(): SecurityPolicy {
            return SecurityPolicy(
                level = SecurityLevel.TRUSTED,
                allowFile = true,
                allowSocket = true,
                allowReflection = true,
                allowProcess = true,
                timeoutMs = 60000,
                maxStackDepth = 20000,
                maxHttpConnections = 10,
                maxFileSize = 100 * 1024 * 1024,
                maxHttpRequests = 1000,
                allowInternalNetwork = true,
                allowRedirects = true
            )
        }
    }
    
    /**
     * 检查是否允许操作
     */
    fun isAllowed(operation: String): Boolean {
        return when (operation) {
            "file" -> allowFile
            "socket" -> allowSocket
            "reflection" -> allowReflection
            "process" -> allowProcess
            else -> false
        }
    }
    
    /**
     * 检查文件扩展名是否允许
     */
    fun isFileExtensionAllowed(filename: String): Boolean {
        val ext = "." + filename.substringAfterLast('.', "").lowercase()
        return ext in allowedFileExtensions || !ext.matches(Regex("""\.\w+"""))
    }
    
    /**
     * 检查文件是否被禁止
     */
    fun isFileBlocked(filename: String): Boolean {
        val lowerName = filename.lowercase()
        return blockedFilePatterns.any { lowerName.endsWith(it) }
    }
    
    /**
     * 检查域名是否被阻止
     */
    fun isDomainBlocked(domain: String): Boolean {
        return blockedDomains.any { domain == it || domain.endsWith(".$it") }
    }
    
    /**
     * 获取策略摘要
     */
    fun getSummary(): PolicySummary {
        return PolicySummary(
            level = level.name,
            displayName = level.displayName,
            riskLevel = level.riskLevel,
            features = listOfNotNull(
                if (allowFile) "文件操作" else null,
                if (allowSocket) "网络Socket" else null,
                if (allowReflection) "反射" else null,
                if (allowProcess) "进程执行" else null,
                if (allowInternalNetwork) "内网访问" else null,
                if (allowRedirects) "重定向" else null
            ),
            limits = mapOf(
                "timeout" to "${timeoutMs}ms",
                "maxFileSize" to "${maxFileSize / 1024 / 1024}MB",
                "maxHttpRequests" to maxHttpRequests
            )
        )
    }
    
    /**
     * 验证参数
     */
    private fun validateParameters() {
        require(timeoutMs > 0) { "超时时间必须大于0" }
        require(maxStackDepth > 0) { "栈深度必须大于0" }
        require(maxHttpConnections > 0) { "HTTP连接数必须大于0" }
        require(maxFileSize > 0) { "文件大小限制必须大于0" }
        require(maxHttpRequests > 0) { "HTTP请求限制必须大于0" }
        
        val sandboxPath = Paths.get(sandboxRoot)
        require(sandboxPath.isAbsolute) { "沙箱路径必须是绝对路径" }
        
        // 检查路径是否包含危险字符
        val pathStr = sandboxRoot.lowercase()
        val dangerousPaths = listOf("/dev", "/proc", "/sys", "c:\\windows\\system32")
        require(!dangerousPaths.any { pathStr.contains(it) }) { "沙箱路径包含危险路径" }
    }
    
    /**
     * 确保沙箱目录存在并设置权限
     */
    private fun ensureSandboxDirectory() {
        val path = Paths.get(sandboxRoot)
        
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
        
        // 尝试设置目录权限（仅 POSIX 系统）
        try {
            if (System.getProperty("os.name").lowercase().contains("nix") ||
                System.getProperty("os.name").lowercase().contains("nux") ||
                System.getProperty("os.name").lowercase().contains("mac")) {
                
                val permissions = HashSet<PosixFilePermission>()
                permissions.add(PosixFilePermission.OWNER_READ)
                permissions.add(PosixFilePermission.OWNER_WRITE)
                permissions.add(PosixFilePermission.OWNER_EXECUTE)
                
                Files.setPosixFilePermissions(path, permissions)
            }
        } catch (e: Exception) {
            // 非关键操作，忽略异常
        }
        
        // 确保目录可写
        if (!Files.isWritable(path)) {
            throw SecurityException("沙箱目录不可写: $sandboxRoot")
        }
    }
}

/**
 * 策略摘要
 */
data class PolicySummary(
    val level: String,
    val displayName: String,
    val riskLevel: Int,
    val features: List<String>,
    val limits: Map<String, String>
)
