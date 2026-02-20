package com.moyue.debug

import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.engine.RhinoEngine
import com.moyue.security.SecurityPolicy
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.springframework.stereotype.Service
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SourceDebugger(
    private val bookSourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine
) {
    
    private val sessions = ConcurrentHashMap<String, DebugSession>()
    
    data class DebugSession(
        val sessionId: String,
        val sourceId: String?,
        val createdAt: LocalDateTime = LocalDateTime.now(),
        var lastActive: LocalDateTime = LocalDateTime.now(),
        val logs: CopyOnWriteArrayList<DebugLog> = CopyOnWriteArrayList(),
        var context: Map<String, Any> = mapOf()
    )
    
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
        val executionTime: Long
    )
    
    data class RuleTestResult(
        val extracted: Any?,
        val error: String?,
        val executionTime: Long
    )
    
    fun createSession(sourceId: String?): String {
        val sessionId = UUID.randomUUID().toString()
        sessions[sessionId] = DebugSession(
            sessionId = sessionId,
            sourceId = sourceId
        )
        return sessionId
    }
    
    fun getSession(sessionId: String): DebugSession? {
        return sessions[sessionId]?.apply {
            lastActive = LocalDateTime.now()
        }
    }
    
    fun getAllSessions(): List<DebugSession> {
        return sessions.values.toList()
    }
    
    fun executeCode(sessionId: String, code: String, function: String, args: List<Any>): ExecuteResult {
        val session = getSession(sessionId) ?: return ExecuteResult(null, emptyList(), "会话不存在", 0)
        
        val startTime = System.currentTimeMillis()
        val logs = mutableListOf<DebugLog>()
        
        try {
            // 创建自定义日志收集器
            val logCollector = { level: String, message: String, data: Any? ->
                val log = DebugLog(LocalDateTime.now(), level, message, data)
                session.logs.add(log)
                logs.add(log)
            }
            
            // 执行代码
            val source = session.sourceId?.let { bookSourceRepository.findById(it).orElse(null) }
            val policy = source?.let { 
                when (it.securityRating) {
                    1, 2 -> SecurityPolicy.trusted()
                    3, 4 -> SecurityPolicy.compatible()
                    else -> SecurityPolicy.standard()
                }
            } ?: SecurityPolicy.standard()
            
            val result = executeWithLogging(code, function, args, policy, logCollector)
            
            return ExecuteResult(
                result = result,
                logs = logs,
                error = null,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            return ExecuteResult(
                result = null,
                logs = logs,
                error = e.message,
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    fun testRule(sessionId: String, ruleType: String, rule: String, html: String): RuleTestResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val doc = Jsoup.parse(html)
            
            val extracted = when (ruleType) {
                "xpath" -> extractByXPath(doc, rule)
                "css" -> extractByCss(doc, rule)
                "regex" -> extractByRegex(html, rule)
                "json" -> extractByJsonPath(html, rule)
                else -> throw IllegalArgumentException("未知规则类型: $ruleType")
            }
            
            return RuleTestResult(
                extracted = extracted,
                error = null,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            return RuleTestResult(
                extracted = null,
                error = e.message,
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
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
    
    fun clearLogs(sessionId: String) {
        val session = getSession(sessionId) ?: return
        session.logs.clear()
    }
    
    private fun executeWithLogging(
        code: String,
        function: String,
        args: List<Any>,
        policy: SecurityPolicy,
        logCollector: (String, String, Any?) -> Unit
    ): Any? {
        val cx = Context.enter()
        try {
            cx.optimizationLevel = -1
            cx.languageVersion = Context.VERSION_1_8
            
            val scope = cx.initStandardObjects()
            
            // 注入日志函数
            val loggerObj = Context.javaToJS(object {
                fun log(msg: String) = logCollector("info", msg, null)
                fun warn(msg: String) = logCollector("warn", msg, null)
                fun error(msg: String) = logCollector("error", msg, null)
                fun debug(msg: String, data: Any?) = logCollector("debug", msg, data)
            }, scope)
            ScriptableObject.putProperty(scope, "console", loggerObj)
            
            // 执行代码
            cx.evaluateString(scope, code, "debug", 1, null)
            
            // 调用函数
            val func = scope.get(function, scope)
            if (func is org.mozilla.javascript.Function) {
                val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
                val result = func.call(cx, scope, scope, jsArgs)
                return Context.jsToJava(result, Any::class.java)
            }
            
            return null
        } finally {
            Context.exit()
        }
    }
    
    private fun extractByXPath(doc: Document, xpath: String): List<String> {
        // 简单实现，实际需要用 JsoupXpath
        val result = mutableListOf<String>()
        try {
            val nodes = doc.select(xpath.replace("/", " "))
            nodes.forEach { node -> result.add(node.toString()) }
        } catch (e: Exception) {
            result.add("XPath解析错误: ${e.message}")
        }
        return result
    }
    
    private fun extractByCss(doc: Document, css: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val elements = doc.select(css)
            elements.forEach { element -> result.add(element.toString()) }
        } catch (e: Exception) {
            result.add("CSS选择器错误: ${e.message}")
        }
        return result
    }
    
    private fun extractByRegex(text: String, regex: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val pattern = java.util.regex.Pattern.compile(regex)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                result.add(matcher.group())
            }
        } catch (e: Exception) {
            result.add("正则表达式错误: ${e.message}")
        }
        return result
    }
    
    private fun extractByJsonPath(json: String, path: String): Any? {
        return try {
            // 简化实现
            val gson = com.google.gson.Gson()
            val obj = gson.fromJson(json, Map::class.java)
            obj[path]
        } catch (e: Exception) {
            "JSON解析错误: ${e.message}"
        }
    }
}
