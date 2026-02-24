package com.moyue.service
 
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.jspecify.annotations.Nullable
import java.time.Duration
import java.time.LocalDateTime
 
/**
 * 缓存服务
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 使用 Caffeine 缓存
 *
 * 功能：
 * - 缓存管理
 * - 缓存清除
 * - 缓存统计
 * - 定时缓存清理
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Service
class CacheService(
    private val cacheManager: CacheManager
) {
    
    private val logger = LoggerFactory.getLogger(CacheService::class.java)
    
    // ==================== 缓存查询操作 ====================
    
    /**
     * 获取所有缓存名称
     */
    fun getCacheNames(): Set<String> {
        return cacheManager.cacheNames
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Any> {
        logger.debug("获取缓存统计")
        
        val stats = mutableMapOf<String, Any>()
        val cacheDetails = mutableMapOf<String, Map<String, Any>>()
        
        cacheManager.cacheNames.forEach { cacheName ->
            val cache = cacheManager.getCache(cacheName)
            if (cache != null) {
                val nativeCache = getNativeCache(cache)
                cacheDetails[cacheName] = mapOf(
                    "name" to cacheName,
                    "type" to nativeCache?.javaClass?.simpleName ?: "unknown",
                    "size" to getCacheSize(nativeCache),
                    "stats" to getCacheStats(nativeCache)
                )
            }
        }
        
        stats["caches"] = cacheDetails
        stats["totalCaches"] = cacheDetails.size
        stats["timestamp"] = LocalDateTime.now()
        
        return stats
    }
    
    /**
     * 获取特定缓存的统计
     */
    fun getCacheStats(cacheName: String): @Nullable Map<String, Any>? {
        logger.debug("获取缓存统计: $cacheName")
        
        val cache = cacheManager.getCache(cacheName) ?: return null
        val nativeCache = getNativeCache(cache)
        
        return mapOf(
            "name" to cacheName,
            "type" to nativeCache?.javaClass?.simpleName ?: "unknown",
            "size" to getCacheSize(nativeCache),
            "stats" to getCacheStats(nativeCache),
            "timestamp" to LocalDateTime.now()
        )
    }
    
    // ==================== 缓存清除操作 ====================
    
    /**
     * 清除指定缓存
     */
    fun clearCache(cacheName: String): Boolean {
        logger.info("清除缓存: $cacheName")
        
        val cache = cacheManager.getCache(cacheName)
        return if (cache != null) {
            cache.clear()
            true
        } else {
            logger.warn("缓存不存在: $cacheName")
            false
        }
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCaches(): Int {
        logger.info("清除所有缓存")
        
        var count = 0
        cacheManager.cacheNames.forEach { cacheName ->
            if (clearCache(cacheName)) count++
        }
        
        logger.info("已清除 $count 个缓存")
        return count
    }
    
    /**
     * 清除缓存项
     */
    fun evictCache(cacheName: String, key: Any): Boolean {
        logger.debug("清除缓存项: $cacheName::$key")
        
        val cache = cacheManager.getCache(cacheName)
        return if (cache != null) {
            cache.evict(key)
            true
        } else {
            false
        }
    }
    
    /**
     * 批量清除缓存项
     */
    fun evictCaches(cacheName: String, keys: List<Any>): Int {
        logger.debug("批量清除缓存项: $cacheName, count=${keys.size}")
        
        val cache = cacheManager.getCache(cacheName) ?: return 0
        var count = 0
        keys.forEach { key ->
            cache.evict(key)
            count++
        }
        
        return count
    }
    
    /**
     * 清除特定模式的缓存
     */
    fun clearCachesByPattern(pattern: String): Int {
        logger.info("清除匹配模式的缓存: $pattern")
        
        val regex = Regex(pattern)
        var count = 0
        
        cacheManager.cacheNames.filter { 
            regex.matches(it) 
        }.forEach { cacheName ->
            if (clearCache(cacheName)) count++
        }
        
        logger.info("已清除 $count 个匹配的缓存")
        return count
    }
    
    // ==================== 缓存预热操作 ====================
    
    /**
     * 预热指定缓存（需要自定义实现）
     */
    fun warmupCache(cacheName: String, dataLoader: () -> List<Any>): Int {
        logger.info("预热缓存: $cacheName")
        
        val cache = cacheManager.getCache(cacheName) ?: return 0
        var count = 0
        
        try {
            val data = dataLoader()
            data.forEach { item ->
                // 这里简化处理，实际需要根据缓存的具体逻辑来
                count++
            }
            logger.info("缓存预热完成: $cacheName, 加载 $count 项")
        } catch (e: Exception) {
            logger.error("缓存预热失败: $cacheName", e)
        }
        
        return count
    }
    
    // ==================== 定时任务 ====================
    
    /**
     * 每小时清理过期缓存
     */
    @Scheduled(fixedRate = 3600000)  // 1小时
    fun cleanupExpiredCache() {
        logger.debug("定时清理过期缓存")
        
        // Caffeine 会自动清理过期条目，这里主要是记录日志
        // 如果需要手动清理，可以在这里实现
        val stats = getCacheStats()
        logger.debug("缓存统计: $stats")
    }
    
    /**
     * 每天凌晨 3 点清理所有缓存
     */
    @Scheduled(cron = "0 0 3 * * ?")
    fun dailyCleanup() {
        logger.info("每日定时清理所有缓存")
        clearAllCaches()
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取原生缓存对象
     */
    @Suppress("UNCHECKED_CAST")
    private fun getNativeCache(cache: Cache): Any? {
        return try {
            val getNativeCache = cache.javaClass.getMethod("getNativeCache")
            getNativeCache.invoke(cache)
        } catch (e: Exception) {
            logger.debug("无法获取原生缓存: ${cache.javaClass.name}")
            null
        }
    }
    
    /**
     * 获取缓存大小
     */
    @Suppress("UNCHECKED_CAST")
    private fun getCacheSize(nativeCache: Any?): Long {
        return try {
            when (nativeCache) {
                is com.github.benmanes.caffeine.cache.Cache<*, *> -> {
                    nativeCache.estimatedSize()
                }
                else -> {
                    -1
                }
            }
        } catch (e: Exception) {
            logger.debug("无法获取缓存大小")
            -1
        }
    }
    
    /**
     * 获取缓存统计
     */
    @Suppress("UNCHECKED_CAST")
    private fun getCacheStats(nativeCache: Any?): Map<String, Any> {
        return try {
            when (nativeCache) {
                is com.github.benmanes.caffeine.cache.Cache<*, *> -> {
                    val stats = nativeCache.stats()
                    mapOf(
                        "hitCount" to stats.hitCount(),
                        "missCount" to stats.missCount(),
                        "hitRate" to String.format("%.2f%%", stats.hitRate() * 100),
                        "missRate" to String.format("%.2f%%", stats.missRate() * 100),
                        "totalRequestCount" to stats.requestCount(),
                        "evictionCount" to stats.evictionCount(),
                        "loadSuccessCount" to stats.loadSuccessCount(),
                        "loadFailureCount" to stats.loadFailureCount(),
                        "averageLoadPenalty" to String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0)
                    )
                }
                else -> {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            logger.debug("无法获取缓存统计")
            emptyMap()
        }
    }
    
    /**
     * 格式化持续时间
     */
    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}小时${minutes % 60}分钟"
            minutes > 0 -> "${minutes}分钟${seconds % 60}秒"
            else -> "${seconds}秒"
        }
    }
}
