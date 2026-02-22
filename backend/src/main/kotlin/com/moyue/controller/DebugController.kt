package com.moyue.controller

import com.moyue.model.BookSource
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/debug")
class DebugController(
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(DebugController::class.java)
    
    // 存储调试会话
    private val sessions = ConcurrentHashMap<String, DebugSession>()
    
    // 会话过期清理器
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()
    
    init {
        // 每5分钟清理一次过期会话
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            5, 5, TimeUnit.MINUTES
        )
    }
    
    /**
     * 创建新的调试会话
     */
    @PostMapping("/session")
    fun createSession(@RequestParam(required = false) sourceId: String?): Map<String, Any> {
        val sessionId = generateSessionId()
        val session = DebugSession(
            sessionId = sessionId,
            sourceId = sourceId,
            createdAt = System.currentTimeMillis(),
            lastActive = System.currentTimeMillis()
        )
        
        sessions[sessionId] = session
        logger.info("创建调试会话: $sessionId, 书源: $sourceId")
        
        return mapOf(
            "sessionId" to sessionId,
            "status" to "created",
            "sourceId" to sourceId
        )
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/session/{sessionId}")
    fun getSession(@PathVariable sessionId: String): ResponseEntity<Map<String, Any>> {
        val session = sessions[sessionId]
        return if (session != null) {
            updateLastActive(sessionId)
            ResponseEntity.ok(mapOf(
                "sessionId" to session.sessionId,
                "sourceId" to session.sourceId,
                "createdAt" to session.createdAt,
                "lastActive" to session.lastActive,
                "logs" to session.logs.size
            ))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 执行书源代码
     */
    @PostMapping("/session/{sessionId}/execute")
    fun executeCode(
        @PathVariable sessionId: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val session = sessions[sessionId]
        if (session == null) {
            return ResponseEntity.notFound().build()
        }
        
        updateLastActive(sessionId)
        
        val code = request["code"] as? String
        val function = request["function"] as? String ?: "search"
        val args = request["args"] as? List<*> ?: emptyList<Any>()
        
        if (code.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "代码不能为空"))
        }
        
        return try {
            // 这里应该调用 SourceService 的调试方法
            // 由于 debug 包可能不存在，这里返回模拟结果
            val startTime = System.currentTimeMillis()
            val result = when (function) {
                "search" -> sourceService.searchBook(args.firstOrNull()?.toString() ?: "test")
                    .map { it.book }
                else -> emptyList()
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            session.addLog("执行成功: $function, 耗时: ${executionTime}ms")
            
            ResponseEntity.ok(mapOf(
                "result" to result,
                "logs" to session.logs,
                "executionTime" to executionTime,
                "success" to true
            ))
        } catch (e: Exception) {
            logger.error("执行代码失败: $sessionId", e)
            session.addLog("执行失败: ${e.message}")
            
            ResponseEntity.ok(mapOf(
                "error" to e.message,
                "logs" to session.logs,
                "success" to false
            ))
        }
    }
    
    /**
     * 测试书源规则
     */
    @PostMapping("/session/{sessionId}/test-rule")
    fun testRule(
        @PathVariable sessionId: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val session = sessions[sessionId]
        if (session == null) {
            return ResponseEntity.notFound().build()
        }
        
        updateLastActive(sessionId)
        
        val ruleType = request["ruleType"] as? String ?: "search"
        val rule = request["rule"] as? String
        val html = request["html"] as? String ?: ""
        
        if (rule.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "规则不能为空"))
        }
        
        return try {
            // 模拟规则测试
            val startTime = System.currentTimeMillis()
            
            // 这里应该实现真正的规则测试逻辑
            val extracted = when (ruleType) {
                "search" -> if (html.isNotEmpty()) listOf("test") else emptyList()
                "bookInfo" -> if (html.isNotEmpty()) mapOf("name" to "test") else null
                else -> null
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            
            session.addLog("测试规则: $ruleType, 结果: ${extracted?.let { "成功" } ?: "失败"}")
            
            ResponseEntity.ok(mapOf(
                "ruleType" to ruleType,
                "extracted" to extracted,
                "error" to null,
                "executionTime" to executionTime
            ))
        } catch (e: Exception) {
            logger.error("测试规则失败: $sessionId", e)
            session.addLog("测试规则失败: ${e.message}")
            
            ResponseEntity.ok(mapOf(
                "ruleType" to ruleType,
                "extracted" to null,
                "error" to e.message,
                "executionTime" to 0
            ))
        }
    }
    
    /**
     * 获取调试日志
     */
    @GetMapping("/session/{sessionId}/logs")
    fun getLogs(@PathVariable sessionId: String): ResponseEntity<List<Map<String, Any>>> {
        val session = sessions[sessionId]
        return if (session != null) {
            updateLastActive(sessionId)
            ResponseEntity.ok(session.logs.map { log ->
                mapOf(
                    "timestamp" to log.timestamp,
                    "message" to log.message
                )
            })
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 清空日志
     */
    @DeleteMapping("/session/{sessionId}/logs")
    fun clearLogs(@PathVariable sessionId: String): ResponseEntity<Map<String, Any>> {
        val session = sessions[sessionId]
        return if (session != null) {
            session.logs.clear()
            updateLastActive(sessionId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "日志已清空"))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    fun deleteSession(@PathVariable sessionId: String): ResponseEntity<Map<String, Any>> {
        val session = sessions.remove(sessionId)
        return if (session != null) {
            logger.info("删除调试会话: $sessionId")
            ResponseEntity.ok(mapOf("success" to true, "message" to "会话已删除"))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 获取所有会话
     */
    @GetMapping("/sessions")
    fun getAllSessions(): List<Map<String, Any>> {
        return sessions.values.map { session ->
            mapOf(
                "sessionId" to session.sessionId,
                "sourceId" to session.sourceId,
                "createdAt" to session.createdAt,
                "lastActive" to session.lastActive,
                "logsCount" to session.logs.size
            )
        }
    }
    
    /**
     * 清空所有会话
     */
    @DeleteMapping("/sessions")
    fun clearAllSessions(): ResponseEntity<Map<String, Any>> {
        val count = sessions.size
        sessions.clear()
        logger.info("清空所有调试会话: $count 个")
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "cleared" to count,
            "message" to "已清空 $count 个会话"
        ))
    }
    
    // ==================== 私有方法 ====================
    
    private fun generateSessionId(): String {
        return "debug_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    private fun updateLastActive(sessionId: String) {
        sessions[sessionId]?.lastActive = System.currentTimeMillis()
    }
    
    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        val expiredThreshold = now - 30 * 60 * 1000 // 30分钟过期
        
        val expiredIds = sessions.filter { it.value.lastActive < expiredThreshold }.keys
        
        expiredIds.forEach { id ->
            logger.info("清理过期调试会话: $id")
            sessions.remove(id)
        }
        
        if (expiredIds.isNotEmpty()) {
            logger.info("清理了 ${expiredIds.size} 个过期调试会话")
        }
    }
    
    /**
     * 调试会话
     */
    data class DebugSession(
        val sessionId: String,
        val sourceId: String?,
        val createdAt: Long,
        var lastActive: Long,
        val logs: MutableList<DebugLog> = mutableListOf()
    ) {
        fun addLog(message: String) {
            logs.add(DebugLog(System.currentTimeMillis(), message))
        }
    }
    
    data class DebugLog(
        val timestamp: Long,
        val message: String
    )
}
