package com.moyue.controller
 
import com.moyue.engine.RhinoEngine
import com.moyue.model.BookSource
import com.moyue.service.CacheService
import com.moyue.service.PreferenceService
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
 
/**
 * 调试控制器
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 书源规则调试
 * - JavaScript 代码执行
 * - 网络请求测试
 * - 缓存管理
 * - 系统信息查询
 *
 * 注意：此控制器仅用于开发调试，生产环境应禁用
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@RestController
@RequestMapping("/api/debug")
class DebugController(
    private val rhinoEngine: RhinoEngine,
    private val sourceService: SourceService,
    private val cacheService: CacheService,
    private val preferenceService: PreferenceService,
    private val cacheManager: CacheManager,
    private val asyncExecutor: Executor
) {
    
    private val logger = LoggerFactory.getLogger(DebugController::class.java)
    
    @Value("\${moyue.security.default-level:standard}")
    private lateinit var defaultSecurityLevel: String
    
    // ==================== 系统信息 ====================
    
    /**
     * 获取系统信息
     */
    @GetMapping("/system/info")
    fun getSystemInfo(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取系统信息")
        
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory
        
        val info = mapOf(
            "timestamp" to LocalDateTime.now(),
            "javaVersion" to System.getProperty("java.version"),
            "javaHome" to System.getProperty("java.home"),
            "osName" to System.getProperty("os.name"),
            "osVersion" to System.getProperty("os.version"),
            "osArch" to System.getProperty("os.arch"),
            "processors" to runtime.availableProcessors(),
            "memory" to mapOf(
                "max" to maxMemory,
                "total" to totalMemory,
                "free" to freeMemory,
                "used" to usedMemory,
                "usage" to String.format("%.2f%%", usedMemory * 100.0 / maxMemory)
            ),
            "workingDir" to System.getProperty("user.dir"),
            "tempDir" to System.getProperty("java.io.tmpdir"),
            "securityLevel" to defaultSecurityLevel
        )
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = info,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取缓存统计
     */
    @GetMapping("/cache/stats")
    fun getCacheStats(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取缓存统计")
        
        val stats = cacheService.getCacheStats()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = stats,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取特定缓存统计
     */
    @GetMapping("/cache/stats/{cacheName}")
    fun getCacheStatsByName(
        @PathVariable cacheName: String
    ): ResponseEntity<BookController.ApiResponse<Map<String, Any>?>> {
        logger.debug("获取缓存统计: $cacheName")
        
        val stats = cacheService.getCacheStats(cacheName)
        return if (stats != null) {
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = stats,
                    message = "获取成功"
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BookController.ApiResponse.error(
                    code = "CACHE_NOT_FOUND",
                    message = "缓存不存在: $cacheName"
                )
            )
        }
    }
    
    /**
     * 获取所有缓存名称
     */
    @GetMapping("/cache/names")
    fun getCacheNames(): ResponseEntity<BookController.ApiResponse<Set<String>>> {
        logger.debug("获取所有缓存名称")
        
        val names = cacheService.getCacheNames()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = names,
                message = "获取成功"
            )
        )
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清除指定缓存
     */
    @DeleteMapping("/cache/{cacheName}")
    fun clearCache(@PathVariable cacheName: String): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("清除缓存: $cacheName")
        
        val success = cacheService.clearCache(cacheName)
        return if (success) {
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    message = "缓存清除成功"
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BookController.ApiResponse.error(
                    code = "CACHE_NOT_FOUND",
                    message = "缓存不存在: $cacheName"
                )
            )
        }
    }
    
    /**
     * 清除所有缓存
     */
    @DeleteMapping("/cache")
    fun clearAllCaches(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.warn("清除所有缓存")
        
        val count = cacheService.clearAllCaches()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = mapOf("cleared" to count),
                message = "已清除 $count 个缓存"
            )
        )
    }
    
    // ==================== 书源调试 ====================
    
    /**
     * 测试书源 URL
     */
    @PostMapping("/source/test-url")
    fun testSourceUrl(@RequestBody request: TestUrlRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.info("测试书源 URL: ${request.url}")
        
        return try {
            val start = System.currentTimeMillis()
            val result = rhinoEngine.checkUrl(request.url)
            val elapsed = System.currentTimeMillis() - start
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "success" to result,
                        "elapsed" to elapsed,
                        "url" to request.url
                    ),
                    message = if (result) "URL 可访问" else "URL 不可访问"
                )
            )
        } catch (e: Exception) {
            logger.error("测试 URL 失败", e)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "success" to false,
                        "error" to e.message,
                        "url" to request.url
                    ),
                    message = "测试失败"
                )
            )
        }
    }
    
    /**
     * 调试书源规则
     */
    @PostMapping("/source/debug-rule")
    fun debugSourceRule(@RequestBody request: DebugRuleRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.info("调试书源规则: source=${request.sourceId}, rule=${request.ruleType}")
        
        return try {
            val source = sourceService.getSourceById(request.sourceId)
            val rules = source.rules
            
            val start = System.currentTimeMillis()
            
            val result = when (request.ruleType.lowercase()) {
                "search" -> {
                    val results = rhinoEngine.executeSearchRule(
                        source,
                        rules ?: throw IllegalArgumentException("书源规则不存在"),
                        request.keyword ?: ""
                    )
                    mapOf("results" to results)
                }
                "book" -> {
                    val bookInfo = rhinoEngine.executeBookInfoRule(
                        source,
                        rules ?: throw IllegalArgumentException("书源规则不存在"),
                        request.bookUrl ?: ""
                    )
                    mapOf("bookInfo" to bookInfo)
                }
                "chapter" -> {
                    val chapters = rhinoEngine.executeChapterListRule(
                        source,
                        rules ?: throw IllegalArgumentException("书源规则不存在"),
                        request.bookUrl ?: ""
                    )
                    mapOf("chapters" to chapters)
                }
                "content" -> {
                    val content = rhinoEngine.executeContentRule(
                        source,
                        rules ?: throw IllegalArgumentException("书源规则不存在"),
                        request.chapterUrl ?: "",
                        request.bookUrl ?: ""
                    )
                    mapOf("content" to content)
                }
                else -> {
                    throw IllegalArgumentException("未知规则类型: ${request.ruleType}")
                }
            }
            
            val elapsed = System.currentTimeMillis() - start
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = result + mapOf(
                        "elapsed" to elapsed,
                        "sourceId" to request.sourceId,
                        "ruleType" to request.ruleType
                    ),
                    message = "调试完成"
                )
            )
        } catch (e: Exception) {
            logger.error("调试规则失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "DEBUG_RULE_FAILED",
                    message = "调试失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 批量测试书源
     */
    @PostMapping("/source/batch-test")
    fun batchTestSources(@RequestBody request: BatchTestRequest): ResponseEntity<BookController.ApiResponse<List<Map<String, Any>>>> {
        logger.info("批量测试书源: ${request.sourceIds?.size ?: "all"}")
        
        val sources = if (request.sourceIds.isNullOrEmpty()) {
            sourceService.getEnabledSources().take(10)
        } else {
            request.sourceIds.mapNotNull { 
                try {
                    sourceService.getSourceById(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        if (sources.isEmpty()) {
            return ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = emptyList(),
                    message = "没有需要测试的书源"
                )
            )
        }
        
        // 并发测试
        val futures = sources.map { source ->
            CompletableFuture.supplyAsync {
                val start = System.currentTimeMillis()
                try {
                    val available = rhinoEngine.checkUrl(source.url ?: "")
                    val elapsed = System.currentTimeMillis() - start
                    
                    mapOf(
                        "sourceId" to source.sourceId,
                        "name" to source.name,
                        "url" to source.url,
                        "available" to available,
                        "elapsed" to elapsed,
                        "error" to null
                    )
                } catch (e: Exception) {
                    val elapsed = System.currentTimeMillis() - start
                    
                    mapOf(
                        "sourceId" to source.sourceId,
                        "name" to source.name,
                        "url" to source.url,
                        "available" to false,
                        "elapsed" to elapsed,
                        "error" to e.message
                    )
                }
            }, asyncExecutor
        }
        
        val results = futures.mapNotNull { 
            try {
                it.get()
            } catch (e: Exception) {
                null
            }
        }
        
        val successCount = results.count { (it["available"] as? Boolean) == true }
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = results,
                message = "测试完成: $successCount/${results.size} 个书源可用"
            )
        )
    }
    
    // ==================== JavaScript 调试 ====================
    
    /**
     * 执行 JavaScript 代码
     */
    @PostMapping("/js/execute")
    fun executeJavaScript(@RequestBody request: ExecuteJSRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.info("执行 JavaScript 代码")
        
        return try {
            val start = System.currentTimeMillis()
            val result = rhinoEngine.executeCode(request.code, request.context ?: emptyMap())
            val elapsed = System.currentTimeMillis() - start
            
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "result" to result,
                        "elapsed" to elapsed
                    ),
                    message = "执行成功"
                )
            )
        } catch (e: Exception) {
            logger.error("执行 JavaScript 失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "JS_EXECUTION_FAILED",
                    message = "执行失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 验证 JavaScript 代码语法
     */
    @PostMapping("/js/validate")
    fun validateJavaScript(@RequestBody request: ValidateJSRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("验证 JavaScript 代码语法")
        
        return try {
            val result = rhinoEngine.validateCode(request.code)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "valid" to result.first,
                        "error" to result.second
                    ),
                    message = if (result.first) "代码有效" else "代码有语法错误"
                )
            )
        } catch (e: Exception) {
            logger.error("验证代码失败", e)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf(
                        "valid" to false,
                        "error" to e.message
                    ),
                    message = "验证失败"
                )
            )
        }
    }
    
    // ==================== 偏好设置调试 ====================
    
    /**
     * 获取所有偏好设置
     */
    @GetMapping("/preferences")
    fun getAllPreferences(): ResponseEntity<BookController.ApiResponse<Map<String, String>>> {
        logger.debug("获取所有偏好设置")
        
        val prefs = preferenceService.getAllPreferences()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = prefs,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取单个偏好设置
     */
    @GetMapping("/preferences/{key}")
    fun getPreference(
        @PathVariable key: String,
        @RequestParam(defaultValue = "") defaultValue: String
    ): ResponseEntity<BookController.ApiResponse<String?>> {
        logger.debug("获取偏好设置: $key")
        
        val value = preferenceService.getPreference(key, defaultValue.ifEmpty { null })
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = value,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 设置偏好设置
     */
    @PutMapping("/preferences/{key}")
    fun setPreference(
        @PathVariable key: String,
        @RequestBody request: SetPreferenceRequest
    ): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("设置偏好: $key = ${request.value}")
        
        preferenceService.setPreference(key, request.value)
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                message = "设置成功"
            )
        )
    }
    
    /**
     * 删除偏好设置
     */
    @DeleteMapping("/preferences/{key}")
    fun deletePreference(@PathVariable key: String): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("删除偏好: $key")
        
        preferenceService.removePreference(key)
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                message = "删除成功"
            )
        )
    }
    
    /**
     * 导出偏好设置
     */
    @GetMapping("/preferences/export")
    fun exportPreferences(): ResponseEntity<String> {
        logger.info("导出偏好设置")
        
        val json = preferenceService.exportPreferences()
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=preferences.json")
            .header("Content-Type", "application/json;charset=UTF-8")
            .body(json)
    }
    
    /**
     * 导入偏好设置
     */
    @PostMapping("/preferences/import")
    fun importPreferences(@RequestBody request: ImportPreferencesRequest): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("导入偏好设置")
        
        return try {
            preferenceService.importPreferences(request.json)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    message = "导入成功"
                )
            )
        } catch (e: Exception) {
            logger.error("导入偏好设置失败", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BookController.ApiResponse.error(
                    code = "IMPORT_PREFERENCES_FAILED",
                    message = "导入失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 重置所有偏好设置
     */
    @DeleteMapping("/preferences")
    fun resetPreferences(): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.warn("重置所有偏好设置")
        
        preferenceService.clearAllPreferences()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                message = "重置成功"
            )
        )
    }
    
    // ==================== 日志查询 ====================
    
    /**
     * 获取应用日志（简化版）
     */
    @GetMapping("/logs")
    fun getLogs(
        @RequestParam(defaultValue = "100") lines: Int,
        @RequestParam(defaultValue = "INFO") level: String
    ): ResponseEntity<BookController.ApiResponse<List<String>>> {
        logger.debug("获取日志: lines=$lines, level=$level")
        
        // 简化实现：返回一些模拟日志
        // 实际应从日志文件读取
        val logs = listOf(
            "[${LocalDateTime.now()}] [INFO] [main] c.moyue.MoyueApplication - Started MoyueApplication in 5.234 seconds",
            "[${LocalDateTime.now()}] [INFO] [main] o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port 8080",
            "[${LocalDateTime.now()}] [DEBUG] [main] c.m.controller.DebugController - 获取日志: lines=$lines, level=$level"
        )
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = logs.take(lines),
                message = "获取成功"
            )
        )
    }
    
    // ==================== 请求/响应 DTO ====================
    
    /**
     * 测试 URL 请求
     */
    data class TestUrlRequest(
        val url: String,
        val timeout: Int = 10
    )
    
    /**
     * 调试规则请求
     */
    data class DebugRuleRequest(
        val sourceId: String,
        val ruleType: String,  // search, book, chapter, content
        val keyword: String? = null,
        val bookUrl: String? = null,
        val chapterUrl: String? = null
    )
    
    /**
     * 批量测试请求
     */
    data class BatchTestRequest(
        val sourceIds: List<String>? = null
    )
    
    /**
     * 执行 JS 请求
     */
    data class ExecuteJSRequest(
        val code: String,
        val context: Map<String, Any>? = null
    )
    
    /**
     * 验证 JS 请求
     */
    data class ValidateJSRequest(
        val code: String
    )
    
    /**
     * 设置偏好请求
     */
    data class SetPreferenceRequest(
        val value: String
    )
    
    /**
     * 导入偏好请求
     */
    data class ImportPreferencesRequest(
        val json: String
    )
}
