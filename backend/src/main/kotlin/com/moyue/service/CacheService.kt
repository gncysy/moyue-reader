package com.moyue.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.HttpCookie
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Service
class CacheService {
    
    companion object {
        private const val CONTENT_CACHE_MAX_SIZE = 1000L
        private const val CONTENT_CACHE_TTL_HOURS = 1L
        private const val TOC_CACHE_MAX_SIZE = 500L
        private const val TOC_CACHE_TTL_HOURS = 6L
        private const val PERSISTENT_CACHE_TTL_SECONDS = 86400L // 24小时
        private const val PERSISTENT_CACHE_MAX_SIZE = 10000L
        private const val CACHE_CLEANUP_INTERVAL_MINUTES = 30L
        private const val CACHE_WARNING_THRESHOLD = 0.9 // 90%
    }

    // L1: 内存缓存 - 章节内容 (LRU, 最大1000条)
    private val contentCache: Cache<String, CachedContent> = Caffeine.newBuilder()
        .maximumSize(CONTENT_CACHE_MAX_SIZE)
        .expireAfterAccess(CONTENT_CACHE_TTL_HOURS, TimeUnit.HOURS)
        .recordStats()
        .build()
    
    // L2: 内存缓存 - 目录 (LRU, 最大500条)
    private val tocCache: Cache<String, CachedToc> = Caffeine.newBuilder()
        .maximumSize(TOC_CACHE_MAX_SIZE)
        .expireAfterWrite(TOC_CACHE_TTL_HOURS, TimeUnit.HOURS)
        .recordStats()
        .build()
    
    // L3: 持久化缓存 - 长期存储
    private val persistentCache = ConcurrentHashMap<String, CacheEntry>()
    
    // 缓存穿透保护：空值缓存
    private val nullValueCache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<String, Boolean>()
    
    // Cookie 存储
    private val cookieStore = ConcurrentHashMap<String, MutableMap<String, HttpCookie>>()
    
    // 缓存访问计数统计
    private val contentAccessCount = AtomicLong(0)
    private val tocAccessCount = AtomicLong(0)
    private val persistentAccessCount = AtomicLong(0)
    
    // 缓存监听器
    private val cacheListeners = mutableListOf<CacheListener>()
    
    // ==================== 数据模型 ====================
    
    data class CachedContent(
        val content: String,
        val cachedAt: LocalDateTime,
        var accessCount: Int = 0,
        val size: Int = content.length
    )
    
    data class CachedToc(
        val chapters: List<CachedChapter>,
        val cachedAt: LocalDateTime,
        var accessCount: Int = 0
    )
    
    data class CachedChapter(
        val title: String,
        val url: String,
        val index: Int
    )
    
    data class CacheEntry(
        val value: Any,
        val expireAt: Long,
        var accessCount: Int = 0,
        val cachedAt: Long = System.currentTimeMillis()
    )
    
    data class CacheStatistics(
        val name: String,
        val size: Long,
        val hitCount: Long,
        val missCount: Long,
        val hitRate: Double,
        val averageLoadTime: Double,
        val evictionCount: Long
    )
    
    interface CacheListener {
        fun onCacheEvicted(cacheName: String, key: String, value: Any?)
        fun onCacheFullWarning(cacheName: String, currentSize: Long, maxSize: Long)
    }
    
    // ==================== L1: 章节内容缓存 ====================
    
    /**
     * 获取章节内容
     */
    fun getContent(key: String): String? {
        // 检查空值缓存
        if (nullValueCache.getIfPresent(key) == true) {
            return null
        }
        
        val cached = contentCache.getIfPresent(key)
        if (cached != null) {
            cached.accessCount++
            contentAccessCount.incrementAndGet()
            return cached.content
        }
        
        return null
    }
    
    /**
     * 缓存章节内容
     */
    fun putContent(key: String, content: String?) {
        if (content == null) {
            // 缓存空值，防止缓存穿透
            nullValueCache.put(key, true)
            return
        }
        
        contentCache.put(key, CachedContent(
            content = content,
            cachedAt = LocalDateTime.now(),
            accessCount = 1,
            size = content.length
        ))
        
        checkCacheWarning("contentCache", contentCache.estimatedSize(), CONTENT_CACHE_MAX_SIZE)
    }
    
    /**
     * 批量缓存章节内容
     */
    fun putContents(contents: Map<String, String>) {
        contents.forEach { (key, content) ->
            putContent(key, content)
        }
    }
    
    // ==================== L2: 目录缓存 ====================
    
    /**
     * 获取目录
     */
    fun getToc(key: String): List<CachedChapter>? {
        val cached = tocCache.getIfPresent(key)
        if (cached != null) {
            cached.accessCount++
            tocAccessCount.incrementAndGet()
            return cached.chapters
        }
        
        return null
    }
    
    /**
     * 缓存目录
     */
    fun putToc(key: String, chapters: List<CachedChapter>) {
        tocCache.put(key, CachedToc(
            chapters = chapters,
            cachedAt = LocalDateTime.now(),
            accessCount = 1
        ))
        
        checkCacheWarning("tocCache", tocCache.estimatedSize(), TOC_CACHE_MAX_SIZE)
    }
    
    // ==================== L3: 持久化缓存 ====================
    
    /**
     * 获取持久化缓存
     */
    fun getPersistent(key: String): Any? {
        val entry = persistentCache[key]
        if (entry != null) {
            if (entry.expireAt > System.currentTimeMillis()) {
                entry.accessCount++
                persistentAccessCount.incrementAndGet()
                return entry.value
            } else {
                // 过期删除
                persistentCache.remove(key)
                return null
            }
        }
        return null
    }
    
    /**
     * 设置持久化缓存
     */
    fun putPersistent(key: String, value: Any?, ttlSeconds: Long = PERSISTENT_CACHE_TTL_SECONDS) {
        if (value == null) {
            nullValueCache.put(key, true)
            return
        }
        
        persistentCache[key] = CacheEntry(
            value = value,
            expireAt = System.currentTimeMillis() + ttlSeconds * 1000,
            cachedAt = System.currentTimeMillis()
        )
        
        checkCacheWarning("persistentCache", persistentCache.size.toLong(), PERSISTENT_CACHE_MAX_SIZE)
    }
    
    /**
     * 批量设置持久化缓存
     */
    fun putPersistentAll(entries: Map<String, Pair<Any, Long>>) {
        entries.forEach { (key, valueAndTtl) ->
            putPersistent(key, valueAndTtl.first, valueAndTtl.second)
        }
    }
    
    /**
     * 删除持久化缓存
     */
    fun removePersistent(key: String) {
        persistentCache.remove(key)
        nullValueCache.invalidate(key)
    }
    
    // ==================== Cookie 管理 ====================
    
    /**
     * 获取所有 Cookie（Cookie 字符串格式）
     */
    fun getCookies(url: String): String {
        val host = extractHost(url)
        val cookies = cookieStore[host]?.values ?: emptyList()
        
        // 过滤过期 Cookie
        val validCookies = cookies.filter { it.hasExpired().not() }
        
        return validCookies.joinToString("; ") { "${it.name}=${it.value}" }
    }
    
    /**
     * 获取单个 Cookie
     */
    fun getCookie(url: String, name: String): String? {
        val host = extractHost(url)
        val cookie = cookieStore[host]?.get(name)
        
        if (cookie != null && !cookie.hasExpired()) {
            return cookie.value
        }
        
        return null
    }
    
    /**
     * 设置 Cookie（支持 Set-Cookie 格式）
     */
    fun setCookie(url: String, cookieString: String) {
        val host = extractHost(url)
        val cookies = cookieStore.getOrPut(host) { mutableMapOf() }
        
        try {
            val httpCookie = HttpCookie.parse(cookieString).firstOrNull()
            if (httpCookie != null) {
                cookies[httpCookie.name] = httpCookie
            }
        } catch (e: Exception) {
            // 兼容简单的 key=value 格式
            val parts = cookieString.split("=", limit = 2)
            if (parts.size == 2) {
                cookies[parts[0].trim()] = HttpCookie(parts[0].trim(), parts[1].trim())
            }
        }
    }
    
    /**
     * 设置 Cookie（直接使用 HttpCookie）
     */
    fun setCookie(url: String, cookie: HttpCookie) {
        val host = extractHost(url)
        val cookies = cookieStore.getOrPut(host) { mutableMapOf() }
        cookies[cookie.name] = cookie
    }
    
    /**
     * 删除域名下的所有 Cookie
     */
    fun removeCookies(url: String) {
        val host = extractHost(url)
        cookieStore.remove(host)
    }
    
    /**
     * 删除单个 Cookie
     */
    fun removeCookie(url: String, name: String) {
        val host = extractHost(url)
        cookieStore[host]?.remove(name)
    }
    
    /**
     * 清空所有 Cookie
     */
    fun removeAllCookies() {
        cookieStore.clear()
    }
    
    // ==================== 统计与监控 ====================
    
    /**
     * 获取缓存统计信息
     */
    fun getStats(): Map<String, CacheStatistics> {
        return mapOf(
            "contentCache" to toCacheStatistics("contentCache", contentCache),
            "tocCache" to toCacheStatistics("tocCache", tocCache),
            "persistentCache" to CacheStatistics(
                name = "persistentCache",
                size = persistentCache.size.toLong(),
                hitCount = persistentAccessCount.get(),
                missCount = 0,
                hitRate = 0.0,
                averageLoadTime = 0.0,
                evictionCount = 0
            )
        )
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        val contentSize = contentCache.estimatedSize()
        val tocSize = tocCache.estimatedSize()
        val persistentSize = persistentCache.size
        
        contentCache.invalidateAll()
        tocCache.invalidateAll()
        persistentCache.clear()
        nullValueCache.invalidateAll()
        cookieStore.clear()
        
        // 重置计数器
        contentAccessCount.set(0)
        tocAccessCount.set(0)
        persistentAccessCount.set(0)
    }
    
    /**
     * 清空特定缓存
     */
    fun clearCache(cacheName: String) {
        when (cacheName) {
            "contentCache" -> contentCache.invalidateAll()
            "tocCache" -> tocCache.invalidateAll()
            "persistentCache" -> persistentCache.clear()
            "nullValueCache" -> nullValueCache.invalidateAll()
            "cookieStore" -> cookieStore.clear()
        }
    }
    
    /**
     * 预热缓存
     */
    fun warmup(keys: List<String>, loader: (String) -> Pair<String?, List<CachedChapter>?>) {
        keys.parallelStream().forEach { key ->
            val (content, chapters) = loader(key)
            if (content != null) {
                putContent(key, content)
            }
            if (chapters != null) {
                putToc(key, chapters)
            }
        }
    }
    
    // ==================== 缓存监听 ====================
    
    fun registerCacheListener(listener: CacheListener) {
        cacheListeners.add(listener)
    }
    
    fun unregisterCacheListener(listener: CacheListener) {
        cacheListeners.remove(listener)
    }
    
    // ==================== 定时任务 ====================
    
    /**
     * 定时清理过期缓存
     */
    @Scheduled(fixedRate = CACHE_CLEANUP_INTERVAL_MINUTES * 60 * 1000)
    fun cleanupExpiredCache() {
        val now = System.currentTimeMillis()
        val expiredKeys = persistentCache.filterValues { it.expireAt <= now }.keys
        
        expiredKeys.forEach { key ->
            val entry = persistentCache.remove(key)
            cacheListeners.forEach { it.onCacheEvicted("persistentCache", key, entry?.value) }
        }
        
        // 清理过期 Cookie
        cookieStore.values.forEach { cookies ->
            cookies.values.removeIf { it.hasExpired() }
        }
    }
    
    // ==================== 私有方法 ====================
    
    private fun toCacheStatistics(name: String, cache: Cache<*, *>): CacheStatistics {
        val stats = cache.stats()
        return CacheStatistics(
            name = name,
            size = cache.estimatedSize(),
            hitCount = stats.hitCount(),
            missCount = stats.missCount(),
            hitRate = stats.hitRate(),
            averageLoadTime = stats.averageLoadPenalty() / 1_000_000.0, // 纳秒转毫秒
            evictionCount = stats.evictionCount()
        )
    }
    
    private fun checkCacheWarning(cacheName: String, currentSize: Long, maxSize: Long) {
        if (currentSize >= maxSize * CACHE_WARNING_THRESHOLD) {
            cacheListeners.forEach {
                it.onCacheFullWarning(cacheName, currentSize, maxSize)
            }
        }
    }
    
    private fun extractHost(url: String): String {
        return try {
            java.net.URL(url).host
        } catch (e: Exception) {
            url
        }
    }
}
