package com.moyue.service
 
import mu.KotlinLogging
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
 
/**
 * 偏好设置服务
 * 管理用户配置和系统设置
 */
class PreferenceService {
    
    private val logger = KotlinLogging.logger {}
    
    // 内存存储
    private val preferences = ConcurrentHashMap<String, Any>()
    
    // 默认值
    private val defaults = mapOf(
        "reader.font-size" to 18,
        "reader.line-height" to 1.8,
        "reader.theme" to "light",
        "reader.auto-scroll" to false,
        "reader.auto-scroll-speed" to 1,
        "source.max-concurrent" to 5,
        "cache.enabled" to true,
        "cache.ttl" to 3600,
        "security.default-level" to "standard"
    )
    
    init {
        // 加载默认值
        preferences.putAll(defaults)
        logger.info { "偏好设置服务初始化完成" }
    }
    
    /**
     * 获取设置
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        return preferences[key] as? T
    }
    
    /**
     * 获取设置（带默认值）
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T): T {
        return preferences[key] as? T ?: defaultValue
    }
    
    /**
     * 设置值
     */
    fun set(key: String, value: Any) {
        preferences[key] = value
        logger.debug { "设置偏好: $key = $value" }
    }
    
    /**
     * 批量设置
     */
    fun setAll(settings: Map<String, Any>) {
        preferences.putAll(settings)
        logger.info { "批量设置偏好: ${settings.size} 条" }
    }
    
    /**
     * 删除设置
     */
    fun remove(key: String) {
        preferences.remove(key)
        logger.debug { "删除偏好: $key" }
    }
    
    /**
     * 重置为默认值
     */
    fun resetToDefaults() {
        preferences.clear()
        preferences.putAll(defaults)
        logger.info { "重置为默认值" }
    }
    
    /**
     * 导出为 Properties
     */
    fun exportToProperties(): Properties {
        return Properties().apply {
            preferences.forEach { (key, value) ->
                setProperty(key, value.toString())
            }
        }
    }
    
    /**
     * 从 Properties 导入
     */
    fun importFromProperties(props: Properties) {
        props.forEach { (key, value) ->
            preferences[key.toString()] = value
        }
        logger.info { "从 Properties 导入偏好: ${props.size} 条" }
    }
    
    /**
     * 获取所有设置
     */
    fun getAll(): Map<String, Any> {
        return preferences.toMap()
    }
}
