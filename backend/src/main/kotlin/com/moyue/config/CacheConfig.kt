package com.moyue.config
 
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit
 
/**
 * 缓存配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 使用 Caffeine 缓存
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
@EnableCaching
class CacheConfig {
    
    @Value("\${moyue.cache.enabled:true}")
    private var cacheEnabled: Boolean = true
    
    @Value("\${moyue.cache.default-ttl:3600}")
    private var defaultTtl: Long = 3600
    
    @Value("\${moyue.cache.max-size:10000}")
    private var maxSize: Long = 10000
    
    @Value("\${moyue.cache.initial-capacity:100}")
    private var initialCapacity: Int = 100
    
    /**
     * 配置缓存管理器
     */
    @Bean
    fun cacheManager(): CacheManager {
        return if (cacheEnabled) {
            val cacheManager = CaffeineCacheManager()
            
            // 配置缓存
            val caches = listOf(
                // 书籍缓存
                CacheSpec("books", defaultTtl, maxSize),
                CacheSpec("book", defaultTtl * 2, maxSize / 10),
                CacheSpec("book-search", defaultTtl, maxSize),
                CacheSpec("recent-books", 300, maxSize / 100),
                CacheSpec("reading-books", 600, maxSize / 100),
                CacheSpec("finished-books", 1800, maxSize / 100),
                CacheSpec("book-stats", 600, maxSize / 1000),
                
                // 书源缓存
                CacheSpec("sources", defaultTtl, maxSize),
                CacheSpec("source", defaultTtl * 2, maxSize / 10),
                CacheSpec("source-search", defaultTtl, maxSize),
                CacheSpec("enabled-sources", 300, maxSize / 100),
                CacheSpec("available-sources", 300, maxSize / 100),
                CacheSpec("source-stats", 600, maxSize / 1000),
                
                // 规则缓存
                CacheSpec("book-info", defaultTtl * 2, maxSize / 10),
                CacheSpec("chapter-list", defaultTtl * 2, maxSize),
                CacheSpec("chapter-content", defaultTtl * 6, maxSize * 2),
                
                // 用户缓存
                CacheSpec("token-user", 86400, maxSize / 100),
                CacheSpec("token-user-id", 86400, maxSize / 100),
                
                // 安全缓存
                CacheSpec("security-policy", defaultTtl, maxSize / 1000),
                
                // 偏好设置缓存
                CacheSpec("preferences", defaultTtl * 2, maxSize / 100),
                CacheSpec("all-preferences", defaultTtl * 2, maxSize / 1000),
                CacheSpec("security-level", defaultTtl * 2, maxSize / 1000),
                CacheSpec("jwt-secret", defaultTtl * 2, maxSize / 1000),
                CacheSpec("jwt-expiration", defaultTtl * 2, maxSize / 1000),
                CacheSpec("cache-enabled", defaultTtl * 2, maxSize / 1000),
                CacheSpec("cache-ttl", defaultTtl * 2, maxSize / 1000),
                CacheSpec("max-concurrent-search", defaultTtl * 2, maxSize / 1000),
                CacheSpec("request-timeout", defaultTtl * 2, maxSize / 1000),
                CacheSpec("reader-font-size", defaultTtl * 2, maxSize / 1000),
                CacheSpec("reader-line-height", defaultTtl * 2, maxSize / 1000),
                CacheSpec("reader-background-color", defaultTtl * 2, maxSize / 1000),
                CacheSpec("reader-text-color", defaultTtl * 2, maxSize / 1000),
                CacheSpec("reader-auto-scroll", defaultTtl * 2, maxSize / 1000),
                CacheSpec("source-weight", defaultTtl * 2, maxSize),
                CacheSpec("source-enabled", defaultTtl * 2, maxSize),
                CacheSpec("data-home", defaultTtl * 2, maxSize / 1000),
                CacheSpec("server-port", defaultTtl * 2, maxSize / 1000),
                CacheSpec("app-version", defaultTtl * 2, maxSize / 1000),
                CacheSpec("last-update-time", defaultTtl * 2, maxSize / 1000)
            )
            
            // 注册缓存
            caches.forEach { spec ->
                cacheManager.registerCustomCache(
                    spec.name,
                    Caffeine.newBuilder()
                        .initialCapacity(initialCapacity)
                        .maximumSize(spec.maxSize)
                        .expireAfterWrite(spec.ttl, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
                )
            }
            
            cacheManager
        } else {
            // 禁用缓存
            val cacheManager = CaffeineCacheManager()
            cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(1))
            cacheManager
        }
    }
    
    /**
     * 缓存规格
     */
    data class CacheSpec(
        val name: String,
        val ttl: Long,
        val maxSize: Long
    )
}
