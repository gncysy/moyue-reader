package com.moyue.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class PreferenceService {
    
    private val stringPrefs = ConcurrentHashMap<String, String>()
    private val intPrefs = ConcurrentHashMap<String, Int>()
    private val booleanPrefs = ConcurrentHashMap<String, Boolean>()
    private val longPrefs = ConcurrentHashMap<String, Long>()
    private val floatPrefs = ConcurrentHashMap<String, Float>()
    private val stringSetPrefs = ConcurrentHashMap<String, Set<String>>()
    
    // String
    fun getString(key: String, default: String): String {
        return stringPrefs.getOrDefault(key, default)
    }
    
    fun putString(key: String, value: String) {
        stringPrefs[key] = value
    }
    
    // Int
    fun getInt(key: String, default: Int): Int {
        return intPrefs.getOrDefault(key, default)
    }
    
    fun putInt(key: String, value: Int) {
        intPrefs[key] = value
    }
    
    // Boolean
    fun getBoolean(key: String, default: Boolean): Boolean {
        return booleanPrefs.getOrDefault(key, default)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        booleanPrefs[key] = value
    }
    
    // Long
    fun getLong(key: String, default: Long): Long {
        return longPrefs.getOrDefault(key, default)
    }
    
    fun putLong(key: String, value: Long) {
        longPrefs[key] = value
    }
    
    // Float
    fun getFloat(key: String, default: Float): Float {
        return floatPrefs.getOrDefault(key, default)
    }
    
    fun putFloat(key: String, value: Float) {
        floatPrefs[key] = value
    }
    
    // String Set
    fun getStringSet(key: String, default: Set<String>): Set<String> {
        return stringSetPrefs.getOrDefault(key, default)
    }
    
    fun putStringSet(key: String, value: Set<String>) {
        stringSetPrefs[key] = value
    }
    
    // 通用操作
    fun remove(key: String) {
        stringPrefs.remove(key)
        intPrefs.remove(key)
        booleanPrefs.remove(key)
        longPrefs.remove(key)
        floatPrefs.remove(key)
        stringSetPrefs.remove(key)
    }
    
    fun contains(key: String): Boolean {
        return stringPrefs.containsKey(key) ||
                intPrefs.containsKey(key) ||
                booleanPrefs.containsKey(key) ||
                longPrefs.containsKey(key) ||
                floatPrefs.containsKey(key) ||
                stringSetPrefs.containsKey(key)
    }
    
    fun clear() {
        stringPrefs.clear()
        intPrefs.clear()
        booleanPrefs.clear()
        longPrefs.clear()
        floatPrefs.clear()
        stringSetPrefs.clear()
    }
}
