package com.moyue.service
 
import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.security.SecurityLevel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import java.util.prefs.Preferences
 
/**
 * 偏好设置服务
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 应用偏好设置
 * - 用户偏好设置
 * - 书源偏好设置
 * - 阅读偏好设置
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Service
@Transactional
class PreferenceService(
    private val bookSourceRepository: BookSourceRepository
) {
    
    private val logger = LoggerFactory.getLogger(PreferenceService::class.java)
    
    // Java Preferences API（本地存储）
    private val appPreferences: Preferences = Preferences.userNodeForPackage(PreferenceService::class.java)
    
    // ==================== 配置属性 ====================
    
    @Value("\${moyue.security.default-level:standard}")
    private lateinit var defaultSecurityLevel: String
    
    @Value("\${moyue.performance.cache.enabled:true}")
    private var cacheEnabled: Boolean = true
    
    @Value("\${moyue.performance.cache.ttl:3600}")
    private var cacheTtl: Int = 3600
    
    @Value("\${moyue.book-source.max-concurrent-search:5}")
    private var maxConcurrentSearch: Int = 5
    
    @Value("\${moyue.book-source.request-timeout:10}")
    private var requestTimeout: Int = 10
    
    @Value("\${server.port:8080}")
    private var serverPort: Int = 8080
    
    // ==================== 应用偏好设置 ====================
    
    /**
     * 获取偏好设置
     */
    @Cacheable(value = ["preferences"], key = "#key")
    fun getPreference(key: String, defaultValue: String? = null): @Nullable String? {
        return appPreferences.get(key, defaultValue)
    }
    
    /**
     * 设置偏好设置
     */
    @CacheEvict(value = ["preferences"], allEntries = true)
    fun setPreference(key: String, value: String) {
        logger.debug("设置偏好: $key = $value")
        appPreferences.put(key, value)
        try {
            appPreferences.flush()
        } catch (e: Exception) {
            logger.error("保存偏好失败: $key", e)
        }
    }
    
    /**
     * 删除偏好设置
     */
    @CacheEvict(value = ["preferences"], allEntries = true)
    fun removePreference(key: String) {
        logger.debug("删除偏好: $key")
        appPreferences.remove(key)
        try {
            appPreferences.flush()
        } catch (e: Exception) {
            logger.error("删除偏好失败: $key", e)
        }
    }
    
    /**
     * 批量设置偏好
     */
    @CacheEvict(value = ["preferences"], allEntries = true)
    fun setPreferences(prefs: Map<String, String>) {
        logger.info("批量设置偏好: ${prefs.size} 项")
        prefs.forEach { (key, value) ->
            appPreferences.put(key, value)
        }
        try {
            appPreferences.flush()
        } catch (e: Exception) {
            logger.error("批量保存偏好失败", e)
        }
    }
    
    /**
     * 获取所有偏好设置
     */
    @Cacheable(value = ["all-preferences"])
    fun getAllPreferences(): Map<String, String> {
        logger.debug("获取所有偏好设置")
        
        val prefs = mutableMapOf<String, String>()
        val keys = appPreferences.keys()
        
        while (keys.hasMoreElements()) {
            val key = keys.nextElement()
            val value = appPreferences.get(key, "")
            prefs[key] = value
        }
        
        return prefs
    }
    
    /**
     * 清除所有偏好设置
     */
    @CacheEvict(value = ["preferences", "all-preferences"], allEntries = true)
    fun clearAllPreferences() {
        logger.warn("清除所有偏好设置")
        appPreferences.clear()
        try {
            appPreferences.flush()
        } catch (e: Exception) {
            logger.error("清除偏好失败", e)
        }
    }
    
    // ==================== 安全偏好设置 ====================
    
    /**
     * 获取安全等级
     */
    @Cacheable(value = ["security-level"])
    fun getSecurityLevel(): SecurityLevel {
        val level = getPreference("security.level", defaultSecurityLevel) ?: "standard"
        return try {
            SecurityLevel.valueOf(level.uppercase())
        } catch (e: Exception) {
            logger.warn("无效的安全等级: $level, 使用默认值")
            SecurityLevel.STANDARD
        }
    }
    
    /**
     * 设置安全等级
     */
    @CacheEvict(value = ["security-level"], allEntries = true)
    fun setSecurityLevel(level: SecurityLevel) {
        logger.info("设置安全等级: $level")
        setPreference("security.level", level.name.lowercase())
    }
    
    /**
     * 获取 JWT 密钥
     */
    @Cacheable(value = ["jwt-secret"])
    fun getJwtSecret(): String {
        return getPreference("jwt.secret", "") ?: ""
    }
    
    /**
     * 设置 JWT 密钥
     */
    @CacheEvict(value = ["jwt-secret"], allEntries = true)
    fun setJwtSecret(secret: String) {
        logger.info("设置 JWT 密钥")
        setPreference("jwt.secret", secret)
    }
    
    /**
     * 获取 JWT 过期时间
     */
    @Cacheable(value = ["jwt-expiration"])
    fun getJwtExpiration(): Long {
        val value = getPreference("jwt.expiration", "86400") ?: "86400"
        return value.toLongOrNull() ?: 86400
    }
    
    /**
     * 设置 JWT 过期时间
     */
    @CacheEvict(value = ["jwt-expiration"], allEntries = true)
    fun setJwtExpiration(expiration: Long) {
        logger.info("设置 JWT 过期时间: $expiration 秒")
        setPreference("jwt.expiration", expiration.toString())
    }
    
    // ==================== 性能偏好设置 ====================
    
    /**
     * 缓存是否启用
     */
    @Cacheable(value = ["cache-enabled"])
    fun isCacheEnabled(): Boolean {
        val value = getPreference("cache.enabled", cacheEnabled.toString())
        return value?.toBoolean() ?: cacheEnabled
    }
    
    /**
     * 设置缓存启用状态
     */
    @CacheEvict(value = ["cache-enabled"], allEntries = true)
    fun setCacheEnabled(enabled: Boolean) {
        logger.info("设置缓存: $enabled")
        setPreference("cache.enabled", enabled.toString())
    }
    
    /**
     * 获取缓存 TTL
     */
    @Cacheable(value = ["cache-ttl"])
    fun getCacheTtl(): Int {
        val value = getPreference("cache.ttl", cacheTtl.toString())
        return value?.toIntOrNull() ?: cacheTtl
    }
    
    /**
     * 设置缓存 TTL
     */
    @CacheEvict(value = ["cache-ttl"], allEntries = true)
    fun setCacheTtl(ttl: Int) {
        logger.info("设置缓存 TTL: $ttl 秒")
        setPreference("cache.ttl", ttl.toString())
    }
    
    /**
     * 获取最大并发搜索数
     */
    @Cacheable(value = ["max-concurrent-search"])
    fun getMaxConcurrentSearch(): Int {
        val value = getPreference("maxConcurrentSearch", maxConcurrentSearch.toString())
        return value?.toIntOrNull() ?: maxConcurrentSearch
    }
    
    /**
     * 设置最大并发搜索数
     */
    @CacheEvict(value = ["max-concurrent-search"], allEntries = true)
    fun setMaxConcurrentSearch(count: Int) {
        logger.info("设置最大并发搜索数: $count")
        setPreference("maxConcurrentSearch", count.toString())
    }
    
    /**
     * 获取请求超时时间
     */
    @Cacheable(value = ["request-timeout"])
    fun getRequestTimeout(): Int {
        val value = getPreference("requestTimeout", requestTimeout.toString())
        return value?.toIntOrNull() ?: requestTimeout
    }
    
    /**
     * 设置请求超时时间
     */
    @CacheEvict(value = ["request-timeout"], allEntries = true)
    fun setRequestTimeout(timeout: Int) {
        logger.info("设置请求超时: $timeout 秒")
        setPreference("requestTimeout", timeout.toString())
    }
    
    // ==================== 阅读偏好设置 ====================
    
    /**
     * 获取阅读器字体大小
     */
    @Cacheable(value = ["reader-font-size"])
    fun getReaderFontSize(): Int {
        val value = getPreference("reader.fontSize", "16")
        return value?.toIntOrNull() ?: 16
    }
    
    /**
     * 设置阅读器字体大小
     */
    @CacheEvict(value = ["reader-font-size"], allEntries = true)
    fun setReaderFontSize(size: Int) {
        logger.info("设置阅读器字体大小: $size")
        setPreference("reader.fontSize", size.toString())
    }
    
    /**
     * 获取阅读器行间距
     */
    @Cacheable(value = ["reader-line-height"])
    fun getReaderLineHeight(): Float {
        val value = getPreference("reader.lineHeight", "1.6")
        return value?.toFloatOrNull() ?: 1.6f
    }
    
    /**
     * 设置阅读器行间距
     */
    @CacheEvict(value = ["reader-line-height"], allEntries = true)
    fun setReaderLineHeight(height: Float) {
        logger.info("设置阅读器行间距: $height")
        setPreference("reader.lineHeight", height.toString())
    }
    
    /**
     * 获取阅读器背景色
     */
    @Cacheable(value = ["reader-background-color"])
    fun getReaderBackgroundColor(): String {
        return getPreference("reader.backgroundColor", "#ffffff") ?: "#ffffff"
    }
    
    /**
     * 设置阅读器背景色
     */
    @CacheEvict(value = ["reader-background-color"], allEntries = true)
    fun setReaderBackgroundColor(color: String) {
        logger.info("设置阅读器背景色: $color")
        setPreference("reader.backgroundColor", color)
    }
    
    /**
     * 获取阅读器文字颜色
     */
    @Cacheable(value = ["reader-text-color"])
    fun getReaderTextColor(): String {
        return getPreference("reader.textColor", "#000000") ?: "#000000"
    }
    
    /**
     * 设置阅读器文字颜色
     */
    @CacheEvict(value = ["reader-text-color"], allEntries = true)
    fun setReaderTextColor(color: String) {
        logger.info("设置阅读器文字颜色: $color")
        setPreference("reader.textColor", color)
    }
    
    /**
     * 是否自动滚动
     */
    @Cacheable(value = ["reader-auto-scroll"])
    fun isAutoScroll(): Boolean {
        val value = getPreference("reader.autoScroll", "false")
        return value?.toBoolean() ?: false
    }
    
    /**
     * 设置自动滚动
     */
    @CacheEvict(value = ["reader-auto-scroll"], allEntries = true)
    fun setAutoScroll(enabled: Boolean) {
        logger.info("设置自动滚动: $enabled")
        setPreference("reader.autoScroll", enabled.toString())
    }
    
    // ==================== 书源偏好设置 ====================
    
    /**
     * 获取书源权重
     */
    @Cacheable(value = ["source-weight"], key = "#sourceId")
    fun getSourceWeight(sourceId: String): Int {
        val source = bookSourceRepository.findBySourceId(sourceId)
            ?: return 0
        
        // 优先使用书源的权重，如果没有则检查偏好设置
        val prefWeight = getPreference("source.weight.$sourceId")?.toIntOrNull()
        return prefWeight ?: source.weight
    }
    
    /**
     * 设置书源权重
     */
    @CacheEvict(value = ["source-weight"], key = "#sourceId")
    fun setSourceWeight(sourceId: String, weight: Int) {
        logger.info("设置书源权重: $sourceId = $weight")
        setPreference("source.weight.$sourceId", weight.toString())
    }
    
    /**
     * 书源是否启用
     */
    @Cacheable(value = ["source-enabled"], key = "#sourceId")
    fun isSourceEnabled(sourceId: String): Boolean {
        val source = bookSourceRepository.findBySourceId(sourceId)
            ?: return false
        
        val prefEnabled = getPreference("source.enabled.$sourceId")?.toBoolean()
        return prefEnabled ?: source.enabled
    }
    
    /**
     * 设置书源启用状态
     */
    @CacheEvict(value = ["source-enabled"], key = "#sourceId")
    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        logger.info("设置书源启用状态: $sourceId = $enabled")
        setPreference("source.enabled.$sourceId", enabled.toString())
    }
    
    // ==================== 系统偏好设置 ====================
    
    /**
     * 获取数据目录
     */
    @Cacheable(value = ["data-home"])
    fun getDataHome(): String {
        return getPreference("data.home", System.getProperty("user.home") + "/MoyueData") 
            ?: System.getProperty("user.home") + "/MoyueData"
    }
    
    /**
     * 设置数据目录
     */
    @CacheEvict(value = ["data-home"], allEntries = true)
    fun setDataHome(path: String) {
        logger.info("设置数据目录: $path")
        setPreference("data.home", path)
    }
    
    /**
     * 获取服务器端口
     */
    @Cacheable(value = ["server-port"])
    fun getServerPort(): Int {
        return serverPort
    }
    
    /**
     * 获取应用版本
     */
    @Cacheable(value = ["app-version"])
    fun getAppVersion(): String {
        return getPreference("app.version", "0.1.0") ?: "0.1.0"
    }
    
    /**
     * 设置应用版本
     */
    @CacheEvict(value = ["app-version"], allEntries = true)
    fun setAppVersion(version: String) {
        logger.info("设置应用版本: $version")
        setPreference("app.version", version)
    }
    
    /**
     * 获取最后更新时间
     */
    @Cacheable(value = ["last-update-time"])
    fun getLastUpdateTime(): LocalDateTime {
        val value = getPreference("lastUpdateTime")
        return if (value != null) {
            try {
                LocalDateTime.parse(value)
            } catch (e: Exception) {
                LocalDateTime.now()
            }
        } else {
            LocalDateTime.now()
        }
    }
    
    /**
     * 设置最后更新时间
     */
    @CacheEvict(value = ["last-update-time"], allEntries = true)
    fun setLastUpdateTime() {
        val now = LocalDateTime.now()
        setPreference("lastUpdateTime", now.toString())
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清除所有偏好缓存
     */
    @CacheEvict(value = [
        "preferences", "all-preferences", 
        "security-level", "jwt-secret", "jwt-expiration",
        "cache-enabled", "cache-ttl", "max-concurrent-search", "request-timeout",
        "reader-font-size", "reader-line-height", "reader-background-color", "reader-text-color", "reader-auto-scroll",
        "source-weight", "source-enabled", "data-home", "server-port", "app-version", "last-update-time"
    ], allEntries = true)
    fun clearCache() {
        logger.info("清除偏好缓存")
    }
    
    /**
     * 导出偏好设置（JSON）
     */
    fun exportPreferences(): String {
        val prefs = getAllPreferences()
        return com.google.gson.Gson().toJson(prefs)
    }
    
    /**
     * 导入偏好设置（JSON）
     */
    @CacheEvict(value = [
        "preferences", "all-preferences", 
        "security-level", "jwt-secret", "jwt-expiration",
        "cache-enabled", "cache-ttl", "max-concurrent-search", "request-timeout",
        "reader-font-size", "reader-line-height", "reader-background-color", "reader-text-color", "reader-auto-scroll",
        "source-weight", "source-enabled", "data-home", "server-port", "app-version", "last-update-time"
    ], allEntries = true)
    fun importPreferences(json: String) {
        logger.info("导入偏好设置")
        
        try {
            val prefs = com.google.gson.Gson().fromJson(json, Map::class.java) as Map<String, String>
            setPreferences(prefs)
        } catch (e: Exception) {
            logger.error("导入偏好设置失败", e)
            throw IllegalArgumentException("无效的偏好设置格式: ${e.message}")
        }
    }
}
