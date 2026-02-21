package com.moyue.service
 
import com.moyue.config.PreferenceService
import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
 
@Service
class SecurityService(
    private val preferenceService: PreferenceService
) {
 
    companion object {
        private const val MAX_LOG_SIZE = 1000
        private const val PREF_KEY_SECURITY_LEVEL = "security.level"
        private const val PREF_KEY_TRUSTED_CONFIRMED = "security.trusted.confirmed"
        private const val LOG_DIR = ".moyue-reader/logs"
        private const val LOG_FILE_PREFIX = "security_"
        private const val LOG_FILE_SUFFIX = ".log"
    }
 
    private var currentPolicy: SecurityPolicy = SecurityPolicy.standard()
    
    // 安全日志，最多保存 1000 条
    private val logs = ConcurrentLinkedDeque<SecurityLog>()
    
    // 使用读写锁保护日志操作
    private val logLock = ReentrantReadWriteLock()
    
    // 丢弃的日志计数（用于监控）
    private val discardedLogCount = AtomicInteger(0)
    
    // 策略变更监听器
    private val policyChangeListeners = mutableListOf<(SecurityPolicy, SecurityPolicy) -> Unit>()
    
    /**
     * 初始化：从持久化存储加载安全级别
     */
    @PostConstruct
    fun init() {
        loadPolicyFromPreferences()
        addLog(currentPolicy.level, "服务启动", "系统", "成功")
    }
    
    /**
     * 销毁前：保存当前安全级别
     */
    @PreDestroy
    fun destroy() {
        savePolicyToPreferences()
        addLog(currentPolicy.level, "服务关闭", "系统", "成功")
        exportLogs() // 导出日志
    }
    
    /**
     * 获取当前安全策略
     */
    fun getCurrentPolicy(): SecurityPolicy {
        return currentPolicy
    }
    
    /**
     * 设置安全策略
     * @param level 目标安全级别
     * @param confirmPassword 信任模式需要确认密码或令牌
     * @return 新的安全策略
     * @throws SecurityException 如果权限不足或确认失败
     */
    @Synchronized
    fun setPolicy(level: SecurityLevel, confirmPassword: String? = null): SecurityPolicy {
        val oldPolicy = currentPolicy
        
        // 信任模式需要额外确认
        if (level == SecurityLevel.TRUSTED) {
            if (confirmPassword.isNullOrBlank()) {
                throw SecurityException("切换到信任模式需要提供确认凭证")
            }
            if (!validateTrustConfirmation(confirmPassword)) {
                throw SecurityException("确认凭证无效")
            }
        }
        
        // 应用新策略
        currentPolicy = when (level) {
            SecurityLevel.STANDARD -> SecurityPolicy.standard()
            SecurityLevel.COMPATIBLE -> SecurityPolicy.compatible()
            SecurityLevel.TRUSTED -> SecurityPolicy.trusted()
        }
        
        // 保存到持久化存储
        savePolicyToPreferences()
        
        // 记录日志
        addLog(level, "切换安全模式", "系统", "成功")
        
        // 通知监听器
        notifyPolicyChange(oldPolicy, currentPolicy)
        
        return currentPolicy
    }
    
    /**
     * 获取安全日志（分页）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param level 可选的安全级别过滤
     */
    fun getSecurityLogs(page: Int = 0, size: Int = 50, level: SecurityLevel? = null): List<SecurityLogDTO> {
        val filteredLogs = level?.let { filterLevel ->
            logs.filter { it.level == filterLevel }
        } ?: logs.toList()
        
        val start = page * size
        val end = (start + size).coerceAtMost(filteredLogs.size)
        
        return filteredLogs.subList(start, end).map { it.toDTO() }
    }
    
    /**
     * 获取安全日志统计
     */
    fun getSecurityLogStats(): SecurityLogStats {
        return logLock.read {
            SecurityLogStats(
                totalLogs = logs.size,
                discardedLogs = discardedLogCount.get(),
                byLevel = logs.groupingBy { it.level }.eachCount(),
                latestLog = logs.firstOrNull()?.toDTO()
            )
        }
    }
    
    /**
     * 清空安全日志
     */
    @Synchronized
    fun clearSecurityLogs(): Int {
        val count = logs.size
        logs.clear()
        discardedLogCount.set(0)
        addLog(currentPolicy.level, "清空日志", "系统", "成功")
        return count
    }
    
    /**
     * 导出日志到文件
     * @return 导出的文件路径
     */
    fun exportLogs(): String {
        val logDir = File(System.getProperty("user.home"), LOG_DIR)
        logDir.mkdirs()
        
        val timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val logFile = File(logDir, "${LOG_FILE_PREFIX}${timestamp}${LOG_FILE_SUFFIX}")
        
        val content = logs.joinToString("\n") { log ->
            "${log.timestamp} | ${log.level.name.padEnd(10)} | ${log.action.padEnd(20)} | ${log.source ?: "N/A".padEnd(15)} | ${log.result}"
        }
        
        logFile.writeText(content)
        
        addLog(currentPolicy.level, "导出日志", "系统", "文件: ${logFile.name}")
        
        return logFile.absolutePath
    }
    
    /**
     * 获取沙箱状态
     */
    fun getSandboxStatus(): SandboxStatus {
        val isHealthy = checkSandboxHealth()
        
        return SandboxStatus(
            currentPolicy = currentPolicy.level.name,
            totalLogs = logs.size,
            uptime = if (isHealthy) "正常" else "异常",
            memoryUsage = getMemoryUsage(),
            threadCount = Thread.activeCount(),
            lastPolicyChange = getLastPolicyChangeTime()
        )
    }
    
    /**
     * 测试书源兼容性
     * @param sourceId 书源ID
     * @param testLevel 测试的安全级别（默认使用当前级别）
     * @return 兼容性测试结果
     */
    fun testSourceCompatibility(sourceId: String, testLevel: SecurityLevel? = null): SourceCompatibilityResult {
        val targetLevel = testLevel ?: currentPolicy.level
        
        // 模拟兼容性检测（实际应该执行书源规则测试）
        val isCompatible = checkCompatibility(sourceId, targetLevel)
        
        val warnings = mutableListOf<String>()
        
        when (targetLevel) {
            SecurityLevel.STANDARD -> {
                warnings.add("文件操作 API 将被禁用")
                warnings.add("部分网络请求可能受限")
            }
            SecurityLevel.COMPATIBLE -> {
                warnings.add("仅允许基本的 DOM 操作")
            }
            SecurityLevel.TRUSTED -> {
                warnings.add("完全信任模式，请确保书源来源可靠")
            }
        }
        
        return SourceCompatibilityResult(
            sourceId = sourceId,
            testLevel = targetLevel.name,
            compatible = isCompatible,
            warnings = warnings,
            recommendedAction = if (isCompatible) "可以使用" else "建议修改书源规则"
        )
    }
    
    /**
     * 添加安全日志
     */
    fun addLog(level: SecurityLevel, action: String, source: String, result: String) {
        val log = SecurityLog(
            timestamp = LocalDateTime.now(),
            level = level,
            action = action,
            source = source,
            result = result
        )
        
        logLock.write {
            logs.addFirst(log)
            
            // 限制日志数量
            while (logs.size > MAX_LOG_SIZE) {
                logs.removeLast()
                discardedLogCount.incrementAndGet()
            }
        }
    }
    
    /**
     * 供 JsExtensions 调用的日志方法
     */
    fun logSecurityEvent(level: SecurityLevel, action: String, source: String, result: String) {
        addLog(level, action, source, result)
    }
    
    /**
     * 注册策略变更监听器
     */
    fun registerPolicyChangeListener(listener: (SecurityPolicy, SecurityPolicy) -> Unit) {
        policyChangeListeners.add(listener)
    }
    
    /**
     * 移除策略变更监听器
     */
    fun unregisterPolicyChangeListener(listener: (SecurityPolicy, SecurityPolicy) -> Unit) {
        policyChangeListeners.remove(listener)
    }
    
    // ==================== 私有方法 ====================
    
    private fun loadPolicyFromPreferences() {
        val levelName = preferenceService.getString(PREF_KEY_SECURITY_LEVEL, SecurityLevel.STANDARD.name)
        currentPolicy = try {
            when (SecurityLevel.valueOf(levelName)) {
                SecurityLevel.STANDARD -> SecurityPolicy.standard()
                SecurityLevel.COMPATIBLE -> SecurityPolicy.compatible()
                SecurityLevel.TRUSTED -> {
                    if (preferenceService.getBoolean(PREF_KEY_TRUSTED_CONFIRMED, false)) {
                        SecurityPolicy.trusted()
                    } else {
                        SecurityPolicy.standard()
                    }
                }
            }
        } catch (e: Exception) {
            SecurityPolicy.standard()
        }
    }
    
    private fun savePolicyToPreferences() {
        preferenceService.putString(PREF_KEY_SECURITY_LEVEL, currentPolicy.level.name)
        if (currentPolicy.level == SecurityLevel.TRUSTED) {
            preferenceService.putBoolean(PREF_KEY_TRUSTED_CONFIRMED, true)
        }
    }
    
    private fun validateTrustConfirmation(confirmation: String): Boolean {
        // 实际应该验证用户密码或令牌
        // 这里简化为检查确认字符串
        return confirmation.length >= 8
    }
    
    private fun checkCompatibility(sourceId: String, level: SecurityLevel): Boolean {
        // 实际应该执行书源规则测试
        // 这里简化实现：信任模式总是兼容，其他模式有一定概率不兼容
        return when (level) {
            SecurityLevel.TRUSTED -> true
            else -> (sourceId.hashCode() % 10) > 2 // 模拟 80% 兼容率
        }
    }
    
    private fun checkSandboxHealth(): Boolean {
        // 检查 Rhino 引擎是否正常
        // 检查内存使用是否合理
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        return usedMemory < maxMemory * 0.8
    }
    
    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMB = runtime.maxMemory() / 1024 / 1024
        return "${usedMB}MB / ${maxMB}MB"
    }
    
    private fun getLastPolicyChangeTime(): LocalDateTime {
        // 从日志中查找最后一次策略变更
        return logs.find { it.action == "切换安全模式" }?.timestamp ?: LocalDateTime.now()
    }
    
    private fun notifyPolicyChange(oldPolicy: SecurityPolicy, newPolicy: SecurityPolicy) {
        policyChangeListeners.forEach { it(oldPolicy, newPolicy) }
    }
}
 
// ==================== 数据模型类 ====================
 
/**
 * 安全日志
 */
data class SecurityLog(
    val timestamp: LocalDateTime,
    val level: SecurityLevel,
    val action: String,
    val source: String?,
    val result: String
) {
    fun toDTO(): SecurityLogDTO {
        return SecurityLogDTO(
            timestamp = timestamp.toString(),
            level = level.name,
            action = action,
            source = source,
            result = result
        )
    }
}
 
/**
 * 安全日志 DTO
 */
data class SecurityLogDTO(
    val timestamp: String,
    val level: String,
    val action: String,
    val source: String?,
    val result: String
)
 
/**
 * 安全日志统计
 */
data class SecurityLogStats(
    val totalLogs: Int,
    val discardedLogs: Int,
    val byLevel: Map<SecurityLevel, Int>,
    val latestLog: SecurityLogDTO?
)
 
/**
 * 沙箱状态
 */
data class SandboxStatus(
    val currentPolicy: String,
    val totalLogs: Int,
    val uptime: String,
    val memoryUsage: String,
    val threadCount: Int,
    val lastPolicyChange: LocalDateTime
)
 
/**
 * 书源兼容性测试结果
 */
data class SourceCompatibilityResult(
    val sourceId: String,
    val testLevel: String,
    val compatible: Boolean,
    val warnings: List<String>,
    val recommendedAction: String
)
