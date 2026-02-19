package com.moyue.service

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class CacheService {
    
    // L1: 内存缓存 (Caffeine)
    private val memoryCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, Any>()
    
    // L2: 持久化缓存 (模拟，实际用 Redis 或 SQLite)
    private val persistentCache = ConcurrentHashMap<String, CacheEntry>()
    
    data class CacheEntry(
        val value: Any,
        val expireAt: Long
    )
    
    fun get(key: String): Any? {
        // 先查内存
        memoryCache.getIfPresent(key)?.let { return it }
        
        // 再查持久化
        val entry = persistentCache[key]
        if (entry != null && entry.expireAt > System.currentTimeMillis()) {
            memoryCache.put(key, entry.value)
            return entry.value
        }
        
        return null
    }
    
    fun put(key: String, value: Any, ttlSeconds: Long = 3600) {
        memoryCache.put(key, value)
        persistentCache[key] = CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000)
    }
    
    fun remove(key: String) {
        memoryCache.invalidate(key)
        persistentCache.remove(key)
    }
    
    fun contains(key: String): Boolean {
        return get(key) != null
    }
    
    fun keys(): List<String> {
        return persistentCache.keys.toList()
    }
    
    fun clear() {
        memoryCache.invalidateAll()
        persistentCache.clear()
    }
    
    // Cookie 管理
    private val cookieStore = ConcurrentHashMap<String, MutableMap<String, String>>()
    
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
            val (key, value) = part.trim().split("=", limit = 2).let {
                it[0] to (it.getOrNull(1) ?: "")
            }
            cookies[key] = value
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
