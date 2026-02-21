package com.moyue.service

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class PreferenceService {
    
    // 配置存储文件路径
    private val configDir = System.getProperty("user.home") + "/.moyue-reader"
    private val configFile = File(configDir, "preferences.json")
    
    // 统一使用字符串存储，避免类型分散
    private val preferences = ConcurrentHashMap<String, String>()
    
    // 配置变更监听器
    private val listeners = ConcurrentHashMap<String, MutableList<(String, Any?) -> Unit>>()
    
    // 类型转换器
    private val converters = mapOf<String, (String) -> Any>(
        "string" to { it },
        "int" to { it.toIntOrNull() ?: 0 },
        "long" to { it.toLongOrNull() ?: 0L },
        "boolean" to { it.toBoolean() },
        "float" to { it.toFloatOrNull() ?: 0.0f },
        "stringset" to { it.split(",").map { s -> s.trim() }.toSet() }
    )
    
    /**
     * 初始化：加载持久化配置
     */
    @PostConstruct
    fun init() {
        loadFromFile()
    }
    
    /**
     * 销毁前：保存配置到文件
     */
    @PreDestroy
    fun destroy() {
        saveToFile()
    }
    
    // ==================== 基础存储方法 ====================
    
    /**
     * 存储值（自动类型转换）
     */
    fun put(key: String, value: Any?) {
        val oldValue = preferences[key]
        val newValue = when (value) {
            null -> ""
            is Set<*> -> value.joinToString(",")
            else -> value.toString()
        }
        
        preferences[key] = newValue
        
        // 通知监听器
        notifyListeners(key, oldValue, newValue)
    }
    
    /**
     * 获取字符串值
     */
    fun getString(key: String, default: String = ""): String {
        return preferences.getOrDefault(key, default)
    }
    
    /**
     * 获取整数值
     */
    fun getInt(key: String, default: Int = 0): Int {
        return preferences[key]?.toIntOrNull() ?: default
    }
    
    /**
     * 获取长整型值
     */
    fun getLong(key: String, default: Long = 0L): Long {
        return preferences[key]?.toLongOrNull() ?: default
    }
    
    /**
     * 获取布尔值
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return preferences[key]?.toBoolean() ?: default
    }
    
    /**
     * 获取浮点数值
     */
    fun getFloat(key: String, default: Float = 0f): Float {
        return preferences[key]?.toFloatOrNull() ?: default
    }
    
    /**
     * 获取字符串集合
     */
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> {
        val value = preferences[key]
        return if (value.isNullOrBlank()) {
            default
        } else {
            value.split(",").map { it.trim() }.toSet()
        }
    }
    
    // ==================== 兼容旧接口 ====================
    
    fun putString(key: String, value: String) = put(key, value)
    
    fun putInt(key: String, value: Int) = put(key, value)
    
    fun putLong(key: String, value: Long) = put(key, value)
    
    fun putBoolean(key: String, value: Boolean) = put(key, value)
    
    fun putFloat(key: String, value: Float) = put(key, value)
    
    fun putStringSet(key: String, value: Set<String>) = put(key, value)
    
    // ==================== 通用操作 ====================
    
    /**
     * 移除配置项
     */
    fun remove(key: String) {
        val oldValue = preferences.remove(key)
        notifyListeners(key, oldValue, null)
    }
    
    /**
     * 检查配置项是否存在
     */
    fun contains(key: String): Boolean {
        return preferences.containsKey(key) && !preferences[key].isNullOrBlank()
    }
    
    /**
     * 清空所有配置
     */
    fun clear() {
        val oldPreferences = preferences.toMap()
        preferences.clear()
        oldPreferences.forEach { (key, _) ->
            notifyListeners(key, oldPreferences[key], null)
        }
    }
    
    /**
     * 获取所有配置项
     */
    fun getAll(): Map<String, String> {
        return preferences.toMap()
    }
    
    // ==================== 配置监听 ====================
    
    /**
     * 注册配置变更监听器
     * @param key 配置键，null 表示监听所有变更
     * @param listener 监听器回调 (key, oldValue, newValue)
     */
    fun registerListener(key: String?, listener: (String, Any?, Any?) -> Unit) {
        val listenerKey = key ?: "*"
        listeners.getOrPut(listenerKey) { mutableListOf() }.add(listener)
    }
    
    /**
     * 移除监听器
     */
    fun unregisterListener(key: String?, listener: (String, Any?, Any?) -> Unit) {
        val listenerKey = key ?: "*"
        listeners[listenerKey]?.remove(listener)
    }
    
    private fun notifyListeners(key: String, oldValue: String?, newValue: String?) {
        // 通知特定键的监听器
        listeners[key]?.forEach { it(key, oldValue, newValue) }
        // 通知全局监听器
        listeners["*"]?.forEach { it(key, oldValue, newValue) }
    }
    
    // ==================== 持久化 ====================
    
    /**
     * 从文件加载配置
     */
    private fun loadFromFile() {
        try {
            if (configFile.exists()) {
                configFile.readText().lines().forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        preferences[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 保存配置到文件
     */
    fun saveToFile() {
        try {
            configFile.parentFile?.mkdirs()
            val content = preferences.map { "${it.key}=${it.value}" }.joinToString("\n")
            configFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 批量设置配置
     */
    fun putAll(configs: Map<String, Any?>) {
        configs.forEach { (key, value) -> put(key, value) }
        saveToFile()
    }
    
    /**
     * 重置为默认值
     */
    fun resetToDefaults(defaults: Map<String, Any>) {
        clear()
        putAll(defaults)
    }
}
