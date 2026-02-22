package com.moyue.debug

import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.source.engine.RhinoEngine
import com.moyue.security.SecurityPolicy
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

@Service
class SourceDebugger(
    private val bookSourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine
) {
    
    private val logger = LoggerFactory.getLogger(SourceDebugger::class.java)
    
    private val sessions = ConcurrentHashMap<String, DebugSession>()
    
    // 会话统计
    private val sessionCounter = AtomicLong(0)
    private val executionCounter = AtomicLong(0)
    
    // 会话超时时间（分钟）
    private val sessionTimeoutMinutes = 30L
    
    companion object {
        private const val MAX_LOG_SIZE = 1000
        private const val MAX_SESSION_SIZE = 100
        private const val CLEANUP_INTERVAL_MINUTES = 10L
    }
    
    init {
        logger.info("SourceDebugger 初始化完成")
    }
    
    // ==================== 数据类 ====================
    
    data class DebugSession(
        val sessionId: String,
        val sourceId: String?,
        val createdAt: LocalDateTime = LocalDateTime.now(),
        var lastActive: LocalDateTime = LocalDateTime.now(),
        val logs: CopyOnWriteArrayList<DebugLog> = CopyOnWriteArrayList(),
        var context: Map<String, Any> = mapOf(),
        var variables: MutableMap<String, Any> = mutableMapOf(),
        val metadata: MutableMap<String, Any> = mutableMapOf()
    ) {
        fun addLog(level: String, message: String, data: Any? = null) {
            logs.add(DebugLog(LocalDateTime.now(), level, message, data))
            
            // 限制日志大小
            while (logs.size > MAX_LOG_SIZE) {
                logs.removeAt(0)
            }
        }
        
        fun setVariable(name: String, value: Any) {
            variables[name] = value
        }
        
        fun getVariable(name: String): Any? = variables[name]
        
        fun isExpired(): Boolean {
            return ChronoUnit.MINUTES.between(lastActive, LocalDateTime.now()) > sessionTimeoutMinutes
        }
    }
    
    data class DebugLog(
        val timestamp: LocalDateTime,
        val level: String,
        val message: String,
        val data: Any?
    )
    
    data class ExecuteResult(
        val result: Any?,
        val logs: List<DebugLog>,
        val error: String?,
        val executionTime: Long,
        val success: Boolean
    )
    
    data class RuleTestResult(
        val extracted: Any?,
        val error: String?,
        val executionTime: Long,
        val success: Boolean
    )
    
    data class DebugStatistics(
        val totalSessions: Int,
        val activeSessions: Int,
        val expiredSessions: Int,
        val totalExecutions: Long,
        val totalLogs: Long
    )
    
    // ==================== 会话管理 ====================
    
    /**
     * 创建新的调试会话
     */
    fun createSession(sourceId: String?): String {
        if (sessions.size >= MAX_SESSION_SIZE) {
            // 清理过期会话
            cleanupExpiredSessions()
            
            // 如果还是满了，删除最旧的会话
            if (sessions.size >= MAX_SESSION_SIZE) {
                val oldestSession = sessions.values.minByOrNull { it.lastActive }
                oldestSession?.let { sessions.remove(it.sessionId) }
            }
        }
        
        val sessionId = generateSessionId()
        val session = DebugSession(
            sessionId = sessionId,
            sourceId = sourceId
        )
        
        // 加载书源信息
        sourceId?.let { id ->
            val source = bookSourceRepository.findById(id).orElse(null)
            if (source != null) {
                session.metadata["sourceName"] = source.name
                session.metadata["sourceUrl"] = source.url
                session.metadata["securityRating"] = source.securityRating
            }
        }
        
        sessions[sessionId] = session
        sessionCounter.incrementAndGet()
        
        logger.info("创建调试会话: $sessionId, 书源: $sourceId")
        
        return sessionId
    }
    
    /**
     * 获取会话
     */
    fun getSession(sessionId: String): DebugSession? {
        val session = sessions[sessionId]
        if (session != null) {
            session.lastActive = LocalDateTime.now()
        }
        return session
    }
    
    /**
     * 获取所有会话
     */
    fun getAllSessions(): List<DebugSession> {
        return sessions.values.sortedByDescending { it.lastActive }
    }
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String): Boolean {
        val removed = sessions.remove(sessionId) != null
        if (removed) {
            logger.info("删除调试会话: $sessionId")
        }
        return removed
    }
    
    /**
     * 清空所有会话
     */
    fun clearAllSessions(): Int {
        val count = sessions.size
        sessions.clear()
        logger.info("清空所有调试会话: $count 个")
        return count
    }
    
    // ==================== 代码执行 ====================
    
    /**
     * 执行书源代码
     */
    fun executeCode(
        sessionId: String,
        code: String,
        function: String,
        args: List<Any>
    ): ExecuteResult {
        val session = getSession(sessionId)
        if (session == null) {
            return ExecuteResult(null, emptyList(), "会话不存在", 0, false)
        }
        
        val startTime = System.currentTimeMillis()
        executionCounter.incrementAndGet()
        session.addLog("info", "开始执行代码: $function", mapOf("args" to args))
        
        try {
            // 获取书源和安全策略
            val source = session.sourceId?.let { 
                bookSourceRepository.findById(it).orElse(null)
            }
            
            val policy = determineSecurityPolicy(source)
            session.metadata["securityLevel"] = policy.level.name
            
            // 执行代码
            val logCollector = { level: String, message: String, data: Any? ->
                session.addLog(level, message, data)
            }
            
            val result = executeWithLogging(code, function, args, policy, logCollector, session)
            
            val executionTime = System.currentTimeMillis() - startTime
            session.addLog("info", "执行完成", mapOf("executionTime" to executionTime))
            
            return ExecuteResult(
                result = result,
                logs = session.logs.toList(),
                error = null,
                executionTime = executionTime,
                success = true
            )
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            session.addLog("error", "执行失败: ${e.message}", e.stackTraceToString())
            logger.error("执行代码失败: $sessionId", e)
            
            return ExecuteResult(
                result = null,
                logs = session.logs.toList(),
                error = e.message,
                executionTime = executionTime,
                success = false
            )
        }
    }
    
    /**
     * 执行单步调试
     */
    fun executeStep(sessionId: String, code: String): ExecuteResult {
        val session = getSession(sessionId)
        if (session == null) {
            return ExecuteResult(null, emptyList(), "会话不存在", 0, false)
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            val source = session.sourceId?.let { 
                bookSourceRepository.findById(it).orElse(null)
            }
            val policy = determineSecurityPolicy(source)
            
            val logCollector = { level: String, message: String, data: Any? ->
                session.addLog(level, message, data)
            }
            
            val result = executeExpression(code, policy, logCollector, session)
            
            val executionTime = System.currentTimeMillis() - startTime
            
            return ExecuteResult(
                result = result,
                logs = session.logs.toList(),
                error = null,
                executionTime = executionTime,
                success = true
            )
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            session.addLog("error", "执行失败: ${e.message}")
            
            return ExecuteResult(
                result = null,
                logs = session.logs.toList(),
                error = e.message,
                executionTime = executionTime,
                success = false
            )
        }
    }
    
    // ==================== 规则测试 ====================
    
    /**
     * 测试书源规则
     */
    fun testRule(
        sessionId: String,
        ruleType: String,
        rule: String,
        html: String
    ): RuleTestResult {
        val session = getSession(sessionId)
        val startTime = System.currentTimeMillis()
        
        session?.addLog("info", "测试规则: $ruleType", mapOf("rule" to rule))
        
        try {
            val doc = Jsoup.parse(html)
            
            val extracted = when (ruleType.lowercase()) {
                "xpath", "css" -> extractByCss(doc, rule)
                "regex" -> extractByRegex(html, rule)
                "json" -> extractByJsonPath(html, rule)
                else -> throw IllegalArgumentException("未知规则类型: $ruleType")
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            session?.addLog("info", "规则测试完成", mapOf("executionTime" to executionTime))
            
            return RuleTestResult(
                extracted = extracted,
                error = null,
                executionTime = executionTime,
                success = true
            )
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            session?.addLog("error", "规则测试失败: ${e.message}")
            logger.error("规则测试失败", e)
            
            return RuleTestResult(
                extracted = null,
                error = e.message,
                executionTime = executionTime,
                success = false
            )
        }
    }
    
    /**
     * 批量测试规则
     */
    fun testRules(
        sessionId: String,
        rules: List<Map<String, String>>,
        html: String
    ): List<RuleTestResult> {
        return rules.map { ruleMap ->
            val ruleType = ruleMap["type"] ?: "css"
            val rule = ruleMap["rule"] ?: ""
            testRule(sessionId, ruleType, rule, html)
        }
    }
    
    // ==================== 日志管理 ====================
    
    /**
     * 获取日志
     */
    fun getLogs(sessionId: String): List<Map<String, Any>> {
        val session = getSession(sessionId) ?: return emptyList()
        return session.logs.map { log ->
            mapOf(
                "timestamp" to log.timestamp.toString(),
                "level" to log.level,
                "message" to log.message,
                "data" to log.data
            )
        }
    }
    
    /**
     * 清空日志
     */
    fun clearLogs(sessionId: String) {
        val session = getSession(sessionId) ?: return
        session.logs.clear()
        session.addLog("info", "日志已清空")
        logger.info("清空日志: $sessionId")
    }
    
    /**
     * 按级别过滤日志
     */
    fun getLogsByLevel(sessionId: String, level: String): List<Map<String, Any>> {
        val session = getSession(sessionId) ?: return emptyList()
        return session.logs.filter { it.level == level }.map { log ->
            mapOf(
                "timestamp" to log.timestamp.toString(),
                "level" to log.level,
                "message" to log.message,
                "data" to log.data
            )
        }
    }
    
    // ==================== 变量管理 ====================
    
    /**
     * 设置会话变量
     */
    fun setVariable(sessionId: String, name: String, value: Any): Boolean {
        val session = getSession(sessionId) ?: return false
        session.setVariable(name, value)
        return true
    }
    
    /**
     * 获取会话变量
     */
    fun getVariable(sessionId: String, name: String): Any? {
        val session = getSession(sessionId) ?: return null
        return session.getVariable(name)
    }
    
    /**
     * 获取所有变量
     */
    fun getAllVariables(sessionId: String): Map<String, Any> {
        val session = getSession(sessionId) ?: return emptyMap()
        return session.variables.toMap()
    }
    
    /**
     * 删除变量
     */
    fun deleteVariable(sessionId: String, name: String): Boolean {
        val session = getSession(sessionId) ?: return false
        return session.variables.remove(name) != null
    }
    
    /**
     * 清空所有变量
     */
    fun clearVariables(sessionId: String): Boolean {
        val session = getSession(sessionId) ?: return false
        session.variables.clear()
        return true
    }
    
    // ==================== 统计信息 ====================
    
    /**
     * 获取调试统计
     */
    fun getStatistics(): DebugStatistics {
        val now = LocalDateTime.now()
        val expiredCount = sessions.values.count { it.isExpired() }
        
        return DebugStatistics(
            totalSessions = sessions.size,
            activeSessions = sessions.size - expiredCount,
            expiredSessions = expiredCount,
            totalExecutions = executionCounter.get(),
            totalLogs = sessions.values.sumOf { it.logs.size.toLong() }
        )
    }
    
    // ==================== 私有方法 ====================
    
    private fun executeWithLogging(
        code: String,
        function: String,
        args: List<Any>,
        policy: SecurityPolicy,
        logCollector: (String, String, Any?) -> Unit,
        session: DebugSession
    ): Any? {
        val cx = Context.enter()
        try {
            cx.optimizationLevel = -1
            cx.languageVersion = Context.VERSION_ES5
            
            val scope = cx.initStandardObjects()
            
            // 注入 console 对象
            val consoleObj = Context.javaToJS(object {
                fun log(msg: String) = logCollector("info", msg, null)
                fun warn(msg: String) = logCollector("warn", msg, null)
                fun error(msg: String) = logCollector("error", msg, null)
                fun debug(msg: String, data: Any?) = logCollector("debug", msg, data)
                fun info(msg: String) = logCollector("info", msg, null)
            }, scope)
            ScriptableObject.putProperty(scope, "console", consoleObj)
            
            // 注入 session 变量
            session.variables.forEach { (name, value) ->
                val jsValue = Context.javaToJS(value, scope)
                ScriptableObject.putProperty(scope, name, jsValue)
            }
            
            // 执行代码
            cx.evaluateString(scope, code, "debug", 1, null)
            
            // 调用函数
            val func = scope.get(function, scope)
            if (func is org.mozilla.javascript.Function) {
                val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
                val result = func.call(cx, scope, scope, jsArgs)
                
                // 保存返回值到会话变量
                val javaResult = Context.jsToJava(result, Any::class.java)
                session.setVariable("return", javaResult)
                
                return javaResult
            }
            
            return null
        } finally {
            Context.exit()
        }
    }
    
    private fun executeExpression(
        code: String,
        policy: SecurityPolicy,
        logCollector: (String, String, Any?) -> Unit,
        session: DebugSession
    ): Any? {
        val cx = Context.enter()
        try {
            cx.optimizationLevel = -1
            cx.languageVersion = Context.VERSION_ES5
            
            val scope = cx.initStandardObjects()
            
            // 注入 console 和变量
            val consoleObj = Context.javaToJS(object {
                fun log(msg: String) = logCollector("info", msg, null)
                fun warn(msg: String) = logCollector("warn", msg, null)
                fun error(msg: String) = logCollector("error", msg, null)
                fun debug(msg: String, data: Any?) = logCollector("debug", msg, data)
            }, scope)
            ScriptableObject.putProperty(scope, "console", consoleObj)
            
            session.variables.forEach { (name, value) ->
                val jsValue = Context.javaToJS(value, scope)
                ScriptableObject.putProperty(scope, name, jsValue)
            }
            
            // 执行表达式
            val result = cx.evaluateString(scope, code, "step", 1, null)
            val javaResult = Context.jsToJava(result, Any::class.java)
            
            // 保存结果
            session.setVariable("_", javaResult)
            
            return javaResult
        } finally {
            Context.exit()
        }
    }
    
    private fun extractByCss(doc: Document, rule: String): List<Map<String, Any>> {
        val elements = doc.select(rule)
        return elements.mapIndexed { index, element ->
            mapOf(
                "index" to index,
                "text" to element.text(),
                "html" to element.outerHtml(),
                "attributes" to element.attributes().map { 
                    mapOf("key" to it.key, "value" to it.value)
                }
            )
        }
    }
    
    private fun extractByRegex(html: String, rule: String): List<Map<String, Any>> {
        val pattern = Regex(rule)
        val matches = pattern.findAll(html)
        return matches.mapIndexed { index, match ->
            mapOf(
                "index" to index,
                "match" to match.value,
                "groups" to match.groupValues.drop(1)
            )
        }.toList()
    }
    
    private fun extractByJsonPath(html: String, rule: String): Any? {
        // 简化实现：假设 html 是 JSON 字符串
        return try {
            val json = com.google.gson.Gson().fromJson(html, Map::class.java)
            // 这里应该实现真正的 JSONPath 解析
            // 简化版：支持简单的点号分隔路径
            val parts = rule.split(".")
            var current: Any? = json
            parts.forEach { part ->
                if (current is Map<*, *>) {
                    current = current[part]
                }
            }
            current
        } catch (e: Exception) {
            null
        }
    }
    
    private fun determineSecurityPolicy(source: BookSource?): SecurityPolicy {
        return when (source?.securityRating) {
            1, 2 -> SecurityPolicy.trusted()
            3, 4 -> SecurityPolicy.compatible()
            else -> SecurityPolicy.standard()
        }
    }
    
    private fun generateSessionId(): String {
        return "debug_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    // ==================== 定时任务 ====================
    
    /**
     * 定时清理过期会话
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL_MINUTES * 60 * 1000)
    fun cleanupExpiredSessions() {
        val now = LocalDateTime.now()
        val expiredIds = sessions.filter { 
            ChronoUnit.MINUTES.between(it.value.lastActive, now) > sessionTimeoutMinutes
        }.keys
        
        expiredIds.forEach { id ->
            logger.info("清理过期调试会话: $id")
            sessions.remove(id)
        }
        
        if (expiredIds.isNotEmpty()) {
            logger.info("清理了 ${expiredIds.size} 个过期调试会话")
        }
    }
}
