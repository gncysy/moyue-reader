package com.moyue.controller
 
import com.moyue.service.CacheService
import com.moyue.service.PreferenceService
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
 
/**
 * 健康检查控制器
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 应用健康检查
 * - 组件健康检查（数据库、缓存、书源等）
 * - 性能指标
 * - 系统状态监控
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@RestController
@RequestMapping("/api/health")
class HealthController(
    private val cacheService: CacheService,
    private val preferenceService: PreferenceService,
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(HealthController::class.java)
    
    // 应用启动时间
    private val startTime = Instant.now()
    
    // 组件健康检查器
    private val healthIndicators = ConcurrentHashMap<String, HealthIndicator>()
    
    @Autowired(required = false)
    fun setHealthIndicators(indicators: List<HealthIndicator>) {
        indicators.forEach { indicator ->
            val name = indicator::class.simpleName?.removeSuffix("HealthIndicator")?.lowercase()
            if (name != null) {
                healthIndicators[name] = indicator
            }
        }
    }
    
    // ==================== 健康检查 ====================
    
    /**
     * 基础健康检查
     */
    @GetMapping
    fun health(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("健康检查")
        
        val uptime = Duration.between(startTime, Instant.now())
        
        val health = mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "uptime" to uptime.seconds,
            "uptimeHuman" to formatDuration(uptime),
            "version" to "0.1.0"
        )
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = health,
                message = "系统运行正常"
            )
        )
    }
    
    /**
     * 详细健康检查
     */
    @GetMapping("/detailed")
    fun detailedHealth(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("详细健康检查")
        
        val uptime = Duration.between(startTime, Instant.now())
        
        // 检查各组件状态
        val components = mutableMapOf<String, Any>()
        
        // 数据库健康检查
        components["database"] = checkDatabase()
        
        // 缓存健康检查
        components["cache"] = checkCache()
        
        // 书源健康检查
        components["sources"] = checkSources()
        
        // 磁盘空间检查
        components["diskSpace"] = checkDiskSpace()
        
        // 内存使用检查
        components["memory"] = checkMemory()
        
        // 计算总体状态
        val overallStatus = if (components.values.all { (it as Map<*, *>)["status"] == "UP" }) {
            "UP"
        } else {
            "DEGRADED"
        }
        
        val health = mapOf(
            "status" to overallStatus,
            "timestamp" to LocalDateTime.now(),
            "uptime" to uptime.seconds,
            "uptimeHuman" to formatDuration(uptime),
            "version" to "0.1.0",
            "components" to components
        )
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = health,
                message = if (overallStatus == "UP") "系统运行正常" else "系统部分组件异常"
            )
        )
    }
    
    /**
     * 存活探针（Liveness Probe）
     */
    @GetMapping("/liveness")
    fun liveness(): ResponseEntity<Map<String, Any>> {
        // 存活探针：检查应用是否存活
        // 如果返回 500，Kubernetes 会重启 Pod
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    /**
     * 就绪探针（Readiness Probe）
     */
    @GetMapping("/readiness")
    fun readiness(): ResponseEntity<Map<String, Any>> {
        // 就绪探针：检查应用是否准备好接收流量
        val ready = checkReadiness()
        
        return if (ready) {
            ResponseEntity.ok(mapOf(
                "status" to "UP",
                "timestamp" to LocalDateTime.now()
            ))
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                "status" to "DOWN",
                "timestamp" to LocalDateTime.now()
            ))
        }
    }
    
    /**
     * 启动探针（Startup Probe）
     */
    @GetMapping("/startup")
    fun startup(): ResponseEntity<Map<String, Any>> {
        // 启动探针：检查应用是否已启动
        // 用于慢启动应用
        val uptime = Duration.between(startTime, Instant.now())
        val startupTime = Duration.ofSeconds(30)
        
        val started = uptime > startupTime
        
        return if (started) {
            ResponseEntity.ok(mapOf(
                "status" to "UP",
                "timestamp" to LocalDateTime.now()
            ))
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                "status" to "STARTING",
                "timestamp" to LocalDateTime.now()
            ))
        }
    }
    
    // ==================== 组件健康检查 ====================
    
    /**
     * 数据库健康检查
     */
    private fun checkDatabase(): Map<String, Any> {
        return try {
            // 简化实现：实际应查询数据库
            // 这里模拟数据库检查
            mapOf(
                "status" to "UP",
                "details" to mapOf(
                    "database" to "SQLite",
                    "connection" to "active"
                )
            )
        } catch (e: Exception) {
            logger.error("数据库健康检查失败", e)
            mapOf(
                "status" to "DOWN",
                "error" to e.message
            )
        }
    }
    
    /**
     * 缓存健康检查
     */
    private fun checkCache(): Map<String, Any> {
        return try {
            val stats = cacheService.getCacheStats()
            val totalCaches = stats["totalCaches"] as? Int ?: 0
            
            mapOf(
                "status" to "UP",
                "details" to mapOf(
                    "caches" to totalCaches,
                    "provider" to "Caffeine"
                )
            )
        } catch (e: Exception) {
            logger.error("缓存健康检查失败", e)
            mapOf(
                "status" to "DOWN",
                "error" to e.message
            )
        }
    }
    
    /**
     * 书源健康检查
     */
    private fun checkSources(): Map<String, Any> {
        return try {
            val stats = sourceService.getSourceStats()
            val total = stats["total"] as? Long ?: 0
            val available = stats["availableCount"] as? Long ?: 0
            val enabled = stats["enabledCount"] as? Long ?: 0
            
            val status = if (available > 0) "UP" else "DEGRADED"
            
            mapOf(
                "status" to status,
                "details" to mapOf(
                    "total" to total,
                    "enabled" to enabled,
                    "available" to available,
                    "unavailable" to (enabled - available)
                )
            )
        } catch (e: Exception) {
            logger.error("书源健康检查失败", e)
            mapOf(
                "status" to "DOWN",
                "error" to e.message
            )
        }
    }
    
    /**
     * 磁盘空间检查
     */
    private fun checkDiskSpace(): Map<String, Any> {
        return try {
            val dataHome = preferenceService.getDataHome()
            val file = java.io.File(dataHome)
            
            if (!file.exists()) {
                return mapOf(
                    "status" to "DOWN",
                    "error" to "数据目录不存在"
                )
            }
            
            val totalSpace = file.totalSpace / 1024 / 1024 / 1024  // GB
            val freeSpace = file.freeSpace / 1024 / 1024 / 1024    // GB
            val usableSpace = file.usableSpace / 1024 / 1024 / 1024  // GB
            val usage = (1.0 - freeSpace.toDouble() / totalSpace) * 100
            
            val status = when {
                freeSpace < 1 -> "DOWN"      // 少于 1GB
                freeSpace < 5 -> "DEGRADED"  // 少于 5GB
                else -> "UP"
            }
            
            mapOf(
                "status" to status,
                "details" to mapOf(
                    "path" to dataHome,
                    "total" to "${totalSpace}GB",
                    "free" to "${freeSpace}GB",
                    "usable" to "${usableSpace}GB",
                    "usage" to String.format("%.2f%%", usage)
                )
            )
        } catch (e: Exception) {
            logger.error("磁盘空间检查失败", e)
            mapOf(
                "status" to "DOWN",
                "error" to e.message
            )
        }
    }
    
    /**
     * 内存使用检查
     */
    private fun checkMemory(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        
        val maxMemory = runtime.maxMemory() / 1024 / 1024  // MB
        val totalMemory = runtime.totalMemory() / 1024 / 1024  // MB
        val freeMemory = runtime.freeMemory() / 1024 / 1024  // MB
        val usedMemory = totalMemory - freeMemory
        val usage = usedMemory.toDouble() / maxMemory * 100
        
        val status = when {
            usage > 90 -> "DOWN"      // 超过 90%
            usage > 80 -> "DEGRADED"  // 超过 80%
            else -> "UP"
        }
        
        return mapOf(
            "status" to status,
            "details" to mapOf(
                "max" to "${maxMemory}MB",
                "total" to "${totalMemory}MB",
                "used" to "${usedMemory}MB",
                "free" to "${freeMemory}MB",
                "usage" to String.format("%.2f%%", usage)
            )
        )
    }
    
    /**
     * 检查应用就绪状态
     */
    private fun checkReadiness(): Boolean {
        val uptime = Duration.between(startTime, Instant.now())
        
        // 启动至少 10 秒后认为就绪
        if (uptime.seconds < 10) {
            return false
        }
        
        // 检查关键组件
        val dbOk = checkDatabase()["status"] == "UP"
        val cacheOk = checkCache()["status"] == "UP"
        
        return dbOk && cacheOk
    }
    
    // ==================== 性能指标 ====================
    
    /**
     * 获取性能指标
     */
    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取性能指标")
        
        val uptime = Duration.between(startTime, Instant.now())
        val runtime = Runtime.getRuntime()
        
        val metrics = mapOf(
            "jvm" to mapOf(
                "memory" to mapOf(
                    "max" to "${runtime.maxMemory() / 1024 / 1024}MB",
                    "used" to "${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}MB",
                    "free" to "${runtime.freeMemory() / 1024 / 1024}MB"
                ),
                "threads" to mapOf(
                    "count" to Thread.activeCount()
                ),
                "uptime" to uptime.seconds,
                "uptimeHuman" to formatDuration(uptime)
            ),
            "system" to mapOf(
                "processors" to runtime.availableProcessors(),
                "loadAverage" to getLoadAverage()
            ),
            "timestamp" to LocalDateTime.now()
        )
        
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = metrics,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取缓存指标
     */
    @GetMapping("/metrics/cache")
    fun getCacheMetrics(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取缓存指标")
        
        val stats = cacheService.getCacheStats()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = stats,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取书源指标
     */
    @GetMapping("/metrics/sources")
    fun getSourceMetrics(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取书源指标")
        
        val stats = sourceService.getSourceStats()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = stats,
                message = "获取成功"
            )
        )
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 格式化持续时间
     */
    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        
        return when {
            days > 0 -> "${days}天${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟${seconds}秒"
            else -> "${seconds}秒"
        }
    }
    
    /**
     * 获取系统负载
     */
    private fun getLoadAverage(): Double {
        return try {
            val bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
            val method = bean.javaClass.getMethod("getSystemLoadAverage")
            method.invoke(bean) as? Double ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
