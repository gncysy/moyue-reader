package com.moyue.service

import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque

@Service
class SecurityService {

    private var currentPolicy: SecurityPolicy = SecurityPolicy.standard()
    
    // 安全日志，最多保存 1000 条
    private val logs = ConcurrentLinkedDeque<SecurityLog>()
    
    data class SecurityLog(
        val timestamp: LocalDateTime,
        val level: SecurityLevel,
        val action: String,
        val source: String?,
        val result: String
    )

    fun getCurrentPolicy(): SecurityPolicy {
        return currentPolicy
    }

    fun setPolicy(level: SecurityLevel, confirmed: Boolean): SecurityPolicy {
        currentPolicy = when (level) {
            SecurityLevel.STANDARD -> SecurityPolicy.standard()
            SecurityLevel.COMPATIBLE -> SecurityPolicy.compatible()
            SecurityLevel.TRUSTED -> {
                if (confirmed) SecurityPolicy.trusted() else currentPolicy
            }
        }
        
        // 记录日志
        addLog(level, "切换安全模式", "系统", "成功")
        
        return currentPolicy
    }

    fun getSecurityLogs(limit: Int): List<Map<String, Any>> {
        return logs.take(limit).map { log ->
            mapOf(
                "timestamp" to log.timestamp.toString(),
                "level" to log.level.name,
                "action" to log.action,
                "source" to log.source,
                "result" to log.result
            )
        }
    }

    fun clearSecurityLogs() {
        logs.clear()
    }

    fun getSandboxStatus(): Map<String, Any> {
        return mapOf(
            "currentPolicy" to currentPolicy.level.name,
            "totalLogs" to logs.size,
            "uptime" to "正常"
        )
    }

    fun testSourceCompatibility(sourceId: String, level: SecurityLevel?): Map<String, Any> {
        val testLevel = level ?: currentPolicy.level
        
        return mapOf(
            "sourceId" to sourceId,
            "testLevel" to testLevel.name,
            "compatible" to (testLevel != SecurityLevel.TRUSTED), // 简化逻辑
            "warnings" to if (testLevel == SecurityLevel.STANDARD) 
                listOf("可能无法使用文件操作") else emptyList()
        )
    }

    private fun addLog(level: SecurityLevel, action: String, source: String, result: String) {
        logs.addFirst(
            SecurityLog(
                timestamp = LocalDateTime.now(),
                level = level,
                action = action,
                source = source,
                result = result
            )
        )
        
        // 限制日志数量
        while (logs.size > 1000) {
            logs.removeLast()
        }
    }

    // 供 JsExtensions 调用的日志方法
    fun logSecurityEvent(level: SecurityLevel, action: String, source: String, result: String) {
        addLog(level, action, source, result)
    }
}
