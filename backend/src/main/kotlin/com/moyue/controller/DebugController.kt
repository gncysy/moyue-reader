package com.moyue.controller

import com.moyue.model.BookSource
import com.moyue.service.SourceService
import com.moyue.debug.SourceDebugger
import com.moyue.debug.DebugSession
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api/debug")
class DebugController(
    private val sourceService: SourceService,
    private val sourceDebugger: SourceDebugger
) {
    
    // 存储调试会话
    private val sessions = ConcurrentHashMap<String, DebugSession>()
    
    /**
     * 创建新的调试会话
     */
    @PostMapping("/session")
    fun createSession(@RequestParam(required = false) sourceId: String?): Map<String, Any> {
        val sessionId = sourceDebugger.createSession(sourceId)
        sessions[sessionId] = sourceDebugger.getSession(sessionId)!!
        
        return mapOf(
            "sessionId" to sessionId,
            "status" to "created"
        )
    }
    
    /**
     * 执行书源代码
     */
    @PostMapping("/session/{sessionId}/execute")
    fun executeCode(
        @PathVariable sessionId: String,
        @RequestBody request: Map<String, Any>
    ): Map<String, Any> {
        val code = request["code"] as? String ?: return mapOf("error" to "代码不能为空")
        val function = request["function"] as? String ?: "search"
        val args = request["args"] as? List<*> ?: emptyList<Any>()
        
        val result = sourceDebugger.executeCode(sessionId, code, function, args)
        return mapOf(
            "result" to result.result,
            "logs" to result.logs,
            "error" to result.error,
            "executionTime" to result.executionTime
        )
    }
    
    /**
     * 测试书源规则
     */
    @PostMapping("/session/{sessionId}/test-rule")
    fun testRule(
        @PathVariable sessionId: String,
        @RequestBody request: Map<String, Any>
    ): Map<String, Any> {
        val ruleType = request["ruleType"] as? String ?: "search"
        val rule = request["rule"] as? String ?: return mapOf("error" to "规则不能为空")
        val html = request["html"] as? String ?: ""
        
        val result = sourceDebugger.testRule(sessionId, ruleType, rule, html)
        return mapOf(
            "extracted" to result.extracted,
            "error" to result.error,
            "executionTime" to result.executionTime
        )
    }
    
    /**
     * 获取调试日志
     */
    @GetMapping("/session/{sessionId}/logs")
    fun getLogs(@PathVariable sessionId: String): List<Map<String, Any>> {
        return sourceDebugger.getLogs(sessionId)
    }
    
    /**
     * 清空日志
     */
    @DeleteMapping("/session/{sessionId}/logs")
    fun clearLogs(@PathVariable sessionId: String): Map<String, Any> {
        sourceDebugger.clearLogs(sessionId)
        return mapOf("success" to true)
    }
    
    /**
     * 获取所有会话
     */
    @GetMapping("/sessions")
    fun getAllSessions(): List<Map<String, Any>> {
        return sourceDebugger.getAllSessions().map { session ->
            mapOf(
                "sessionId" to session.sessionId,
                "sourceId" to session.sourceId,
                "createdAt" to session.createdAt,
                "lastActive" to session.lastActive
            )
        }
    }
}
