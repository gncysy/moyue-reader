package com.moyue.service
 
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
 
/**
 * 缓存服务
 * 使用内存缓存 + Caffeine（可选）
 */
class CacheService {
    
    private val logger = KotlinLogging.logger {}
    
    // 内存缓存
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    
    // 缓存配置
    private val defaultTtl: Long = 3600 * 1000 // 1小时（毫秒）
    
    /**
     * 缓存条目
     */
    data class CacheEntry(
        val value: Any,
        val expireAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireAt
    }
    
    /**
     * 获取缓存
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key]
        
        if (entry == null) {
            logger.debug { "缓存未命中: $key" }
            return null
        }
        
        if (entry.isExpired()) {
            logger.debug { "缓存已过期: $key" }
            cache.remove(key)
            return null
        }
        
        logger.debug { "缓存命中: $key" }
        return entry.value as? T
    }
    
    /**
     * 设置缓存
     */
    fun put(key: String, value: Any, ttl: Long = defaultTtl) {
        val expireAt = System.currentTimeMillis() + ttl
        cache[key] = CacheEntry(value, expireAt)
        logger.debug { "设置缓存: $key, TTL: ${ttl}ms" }
    }
    
    /**
     * 删除缓存
     */
    fun evict(key: String) {
        cache.remove(key)
        logger.debug { "删除缓存: $key" }
    }
    
    /**
     * 清空所有缓存
     */
    fun clear() {
        cache.clear()
        logger.info { "清空所有缓存" }
    }
    
    /**
     * 获取或计算
     */
    fun <T> getOrCompute(key: String, ttl: Long = defaultTtl, compute: () -> T): T {
        val cached = get<T>(key)
        if (cached != null) return cached
        
        val value = compute()
        put(key, value, ttl)
        return value
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            logger.info { "清理过期缓存: ${expiredKeys.size} 条" }
        }
    }
    
    /**
     * 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "size" to cache.size,
            "expiredCount" to cache.values.count { it.isExpired() }
        )
    }
}
