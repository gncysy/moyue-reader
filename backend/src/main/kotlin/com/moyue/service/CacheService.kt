package com.moyue.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class CacheService {
    
    // L1: 内存缓存 - 章节内容 (LRU, 最大1000条)
    private val contentCache: Cache<String, CachedContent> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .recordStats()
        .build()
    
    // L2: 内存缓存 - 目录 (LRU, 最大500条)
    private val tocCache: Cache<String, List<CachedChapter>> = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build()
    
    // L3: 持久化缓存 - 长期存储
    private val persistentCache = ConcurrentHashMap<String, CacheEntry>()
    
    data class CachedContent(
        val content: String,
        val cachedAt: LocalDateTime,
        val accessCount: Int = 0
    )
    
    data class CachedChapter(
        val title: String,
        val url: String,
        val index: Int
    )
    
    data class CacheEntry(
        val value: Any,
        val expireAt: Long,
        val accessCount: Int = 0
    )
    
    // Cookie 存储
    private val cookieStore = ConcurrentHashMap<String, MutableMap<String, String>>()
    
    /**
     * 获取章节内容（带访问计数）
     */
    fun getContent(key: String): String? {
        val cached = contentCache.getIfPresent(key)
        if (cached != null) {
            // 更新缓存条目（Caffeine 会自动处理）
            return cached.content
        }
        return null
    }
    
    /**
     * 缓存章节内容
     */
    fun putContent(key: String, content: String) {
        contentCache.put(key, CachedContent(
            content = content,
            cachedAt = LocalDateTime.now()
        ))
    }
    
    /**
     * 获取目录
     */
    fun getToc(key: String): List<CachedChapter>? {
        return tocCache.getIfPresent(key)
    }
    
    /**
     * 缓存目录
     */
    fun putToc(key: String, chapters: List<CachedChapter>) {
        tocCache.put(key, chapters)
    }
    
    /**
     * 获取持久化缓存
     */
    fun getPersistent(key: String): Any? {
        val entry = persistentCache[key]
        if (entry != null && entry.expireAt > System.currentTimeMillis()) {
            return entry.value
        }
        persistentCache.remove(key)
        return null
    }
    
    /**
     * 设置持久化缓存
     */
    fun putPersistent(key: String, value: Any, ttlSeconds: Long = 86400) {
        persistentCache[key] = CacheEntry(
            value = value,
            expireAt = System.currentTimeMillis() + ttlSeconds * 1000
        )
    }
    
    /**
     * 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "contentCache" to mapOf(
                "size" to contentCache.estimatedSize(),
                "hitCount" to contentCache.stats().hitCount(),
                "missCount" to contentCache.stats().missCount(),
                "hitRate" to contentCache.stats().hitRate()
            ),
            "tocCache" to mapOf(
                "size" to tocCache.estimatedSize(),
                "hitCount" to tocCache.stats().hitCount(),
                "missCount" to tocCache.stats().missCount()
            ),
            "persistentCache" to mapOf(
                "size" to persistentCache.size
            )
        )
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        contentCache.invalidateAll()
        tocCache.invalidateAll()
        persistentCache.clear()
    }
    
    // ==================== Cookie 管理 ====================
    
    fun getCookies(url: String): String {
        val host = extractHost(url)
        return cookieStore[host]?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""
    }
    
    fun getCookie(url: String, name: String): String? {
        val host = extractHost(url)
        return cookieStore[host]?.get(name)
    }
    
    fun setCookie(url: String, cookie: String) {
        val host = extractHost(url)
        val cookies = cookieStore.getOrPut(host) { mutableMapOf() }
        
        cookie.split(";").forEach { part ->
            val parts = part.trim().split("=", limit = 2)
            if (parts.size == 2) {
                cookies[parts[0]] = parts[1]
            }
        }
    }
    
    fun removeCookie(url: String) {
        cookieStore.remove(extractHost(url))
    }
    
    fun removeAllCookies() {
        cookieStore.clear()
    }
    
    private fun extractHost(url: String): String {
        return try {
            java.net.URL(url).host
        } catch (e: Exception) {
            url
        }
    }
}
