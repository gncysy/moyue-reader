package com.moyue.compat

import okhttp3.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android API 兼容层
 * 
 * 在服务端模拟 Android 环境，提供书源脚本常用的 API：
 * - SharedPreferences 存储
 * - Cookie 管理
 * - Base64 编解码
 * - 加密/解密（MD5/SHA1/AES）
 * - 网络请求（GET/POST）
 * 
 * @author Moyue
 * @since 1.0.0
 */
@Component
class AndroidCompat(
    private val okHttpClient: OkHttpClient
) {
    
    private val logger = LoggerFactory.getLogger(AndroidCompat::class.java)
    
    // ==================== SharedPreferences ====================
    // 注意：当前数据仅存储在内存中，重启后丢失
    // TODO: 实现持久化机制
    
    private val stringPrefs = ConcurrentHashMap<String, String>()
    private val intPrefs = ConcurrentHashMap<String, Int>()
    private val booleanPrefs = ConcurrentHashMap<String, Boolean>()
    private val cookies = ConcurrentHashMap<String, MutableList<CookieItem>>()
    
    /**
     * Cookie 数据项
     */
    private data class CookieItem(
        val name: String,
        val value: String,
        val expires: Long = Long.MAX_VALUE,
        val path: String = "/",
        val domain: String = "",
        val secure: Boolean = false,
        val httpOnly: Boolean = false
    ) {
        /** 判断 Cookie 是否过期 */
        fun isExpired(): Boolean = System.currentTimeMillis() > expires
    }
    
    // ==================== Base64 编解码 ====================
    /**
     * Base64 编码
     * @param data 待编码数据
     * @param flags 编码选项：
     *              0 - 标准 Base64
     *              2 - URL 安全 Base64（无填充）
     *              8 - 标准 Base64（无填充）
     * @return 编码后的字符串
     */
    fun base64Encode(data: ByteArray, flags: Int): String {
        return try {
            val encoder = when (flags) {
                2 -> Base64.getUrlEncoder().withoutPadding()
                8 -> Base64.getEncoder().withoutPadding()
                else -> Base64.getEncoder()
            }
            encoder.encodeToString(data)
        } catch (e: Exception) {
            logger.error("Base64 编码失败", e)
            ""
        }
    }
    
    /**
     * Base64 解码
     * @param str 待解码字符串
     * @param flags 解码选项：
     *              0 - 标准 Base64
     *              2 - URL 安全 Base64
     * @return 解码后的字节数组
     */
    fun base64Decode(str: String, flags: Int): ByteArray {
        return try {
            val decoder = when (flags) {
                2 -> Base64.getUrlDecoder()
                else -> Base64.getDecoder()
            }
            decoder.decode(str)
        } catch (e: Exception) {
            logger.error("Base64 解码失败", e)
            ByteArray(0)
        }
    }
    
    // ==================== SharedPreferences ====================
    
    /**
     * 获取 String 类型配置
     */
    fun getString(key: String, defValue: String): String = stringPrefs[key] ?: defValue
    
    /**
     * 保存 String 类型配置
     */
    fun putString(key: String, value: String) { stringPrefs[key] = value }
    
    /**
     * 获取 Int 类型配置
     */
    fun getInt(key: String, defValue: Int): Int = intPrefs[key] ?: defValue
    
    /**
     * 保存 Int 类型配置
     */
    fun putInt(key: String, value: Int) { intPrefs[key] = value }
    
    /**
     * 获取 Boolean 类型配置
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean = booleanPrefs[key] ?: defValue
    
    /**
     * 保存 Boolean 类型配置
     */
    fun putBoolean(key: String, value: Boolean) { booleanPrefs[key] = value }
    
    /**
     * 清除指定 key 的配置
     */
    fun remove(key: String) {
        stringPrefs.remove(key)
        intPrefs.remove(key)
        booleanPrefs.remove(key)
    }
    
    /**
     * 清除所有配置
     */
    fun clear() {
        stringPrefs.clear()
        intPrefs.clear()
        booleanPrefs.clear()
    }
    
    // ==================== Cookie ====================
    
    /**
     * 获取指定 Cookie
     * @param url 关联 URL
     * @param name Cookie 名称
     * @return Cookie 值，不存在或已过期返回 null
     */
    fun getCookie(url: String, name: String): String? {
        val host = extractHost(url)
        val now = System.currentTimeMillis()
        return cookies[host]?.find { 
            it.name == name && !it.isExpired() 
        }?.value
    }
    
    /**
     * 设置 Cookie
     */
    fun setCookie(url: String, name: String, value: String) {
        setCookie(url, name, value, Long.MAX_VALUE, "/", extractHost(url))
    }
    
    /**
     * 设置完整属性的 Cookie
     */
    fun setCookie(
        url: String,
        name: String,
        value: String,
        expires: Long = Long.MAX_VALUE,
        path: String = "/",
        domain: String = ""
    ) {
        val host = if (domain.isEmpty()) extractHost(url) else domain
        val cookieList = cookies.computeIfAbsent(host) { mutableListOf() }
        
        // 移除同名 Cookie
        cookieList.removeIf { it.name == name }
        
        // 添加新 Cookie
        cookieList.add(CookieItem(name, value, expires, path, domain))
        
        logger.debug("设置 Cookie: $name=$value (domain=$host)")
    }
    
    /**
     * 获取所有 Cookie 字符串（用于请求头）
     */
    fun getCookieString(url: String): String {
        val host = extractHost(url)
        val now = System.currentTimeMillis()
        return cookies[host]
            ?.filter { !it.isExpired() }
            ?.joinToString("; ") { "${it.name}=${it.value}" }
            ?: ""
    }
    
    /**
     * 解析 Set-Cookie 响应头并保存
     */
    private fun parseSetCookies(url: String, setCookieHeaders: List<String>) {
        val host = extractHost(url)
        val cookieList = cookies.computeIfAbsent(host) { mutableListOf() }
        
        setCookieHeaders.forEach { setCookie ->
            try {
                val cookie = parseSetCookie(setCookie, host)
                if (cookie != null) {
                    // 移除同名 Cookie
                    cookieList.removeIf { it.name == cookie.name }
                    cookieList.add(cookie)
                    logger.debug("从 Set-Cookie 解析: ${cookie.name}=${cookie.value}")
                }
            } catch (e: Exception) {
                logger.warn("解析 Set-Cookie 失败: $setCookie", e)
            }
        }
    }
    
    /**
     * 解析单个 Set-Cookie 头
     */
    private fun parseSetCookie(setCookie: String, defaultDomain: String): CookieItem? {
        val parts = setCookie.split(";").map { it.trim() }
        if (parts.isEmpty()) return null
        
        // 解析 name=value（第一个部分）
        val firstPart = parts[0]
        val equalIndex = firstPart.indexOf('=')
        if (equalIndex == -1) return null
        
        val name = firstPart.substring(0, equalIndex)
        var value = if (equalIndex < firstPart.length - 1) {
            firstPart.substring(equalIndex + 1)
        } else {
            ""
        }
        
        // 解析属性
        var expires = Long.MAX_VALUE
        var path = "/"
        var domain = defaultDomain
        var secure = false
        var httpOnly = false
        
        for (i in 1 until parts.size) {
            val part = parts[i].lowercase()
            when {
                part.startsWith("expires=") -> {
                    try {
                        val expiresStr = part.substring(8)
                        val date = Date.parse(expiresStr)
                        if (date > 0) expires = date
                    } catch (e: Exception) {
                        logger.debug("解析 Expires 失败", e)
                    }
                }
                part.startsWith("path=") -> {
                    path = parts[i].substring(5)
                }
                part.startsWith("domain=") -> {
                    domain = parts[i].substring(7)
                }
                part == "secure" -> {
                    secure = true
                }
                part == "httponly" -> {
                    httpOnly = true
                }
            }
        }
        
        return CookieItem(name, value, expires, path, domain, secure, httpOnly)
    }
    
    private fun extractHost(url: String): String = try {
        java.net.URL(url).host
    } catch (e: Exception) {
        logger.warn("提取 URL host 失败: $url", e)
        url
    }
    
    // ==================== 加密/解密 ====================
    
    /**
     * MD5 加密
     * @param toLowerCase 是否转小写（默认 true）
     * @return 32 位 MD5 字符串
     */
    fun md5(input: String, toLowerCase: Boolean = true): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val hash = md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
            if (toLowerCase) hash.lowercase() else hash.uppercase()
        } catch (e: Exception) {
            logger.error("MD5 加密失败", e)
            ""
        }
    }
    
    /**
     * SHA1 加密
     * @param toLowerCase 是否转小写（默认 true）
     * @return 40 位 SHA1 字符串
     */
    fun sha1(input: String, toLowerCase: Boolean = true): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val hash = md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
            if (toLowerCase) hash.lowercase() else hash.uppercase()
        } catch (e: Exception) {
            logger.error("SHA1 加密失败", e)
            ""
        }
    }
    
    /**
     * AES 解密（CBC 模式）
     * @param data 密文
     * @param key 密钥（16/24/32 字节）
     * @param iv 初始化向量（16 字节）
     */
    fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            cipher.doFinal(data)
        } catch (e: Exception) {
            logger.error("AES 解密失败", e)
            ByteArray(0)
        }
    }
    
    /**
     * AES 加密（CBC 模式）
     * @param data 明文
     * @param key 密钥（16/24/32 字节）
     * @param iv 初始化向量（16 字节）
     */
    fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            cipher.doFinal(data)
        } catch (e: Exception) {
            logger.error("AES 加密失败", e)
            ByteArray(0)
        }
    }
    
    // ==================== 网络请求 ====================
    
    /**
     * HTTP GET 请求
     * @param url 请求地址
     * @param headers 请求头
     * @return 响应内容，失败返回空字符串
     */
    fun httpGet(url: String, headers: Map<String, String>? = null): String {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()
            
            // 添加自定义请求头
            headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            
            // 自动添加 Cookie
            getCookieString(url).takeIf { it.isNotEmpty() }?.let {
                requestBuilder.addHeader("Cookie", it)
            }
            
            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            
            // 检查响应码
            if (!response.isSuccessful) {
                logger.warn("HTTP GET 失败: $url - ${response.code}")
                return ""
            }
            
            // 解析 Set-Cookie
            parseSetCookies(url, response.headers("Set-Cookie"))
            
            response.body?.string() ?: ""
        } catch (e: Exception) {
            logger.error("HTTP GET 请求失败: $url", e)
            ""
        }
    }
    
    /**
     * HTTP POST 请求
     * @param url 请求地址
     * @param body 请求体
     * @param headers 请求头
     * @return 响应内容，失败返回空字符串
     */
    fun httpPost(url: String, body: String, headers: Map<String, String>? = null): String {
        return try {
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val requestBody = body.toRequestBody(mediaType)
            
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
            
            // 添加自定义请求头
            headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            
            // 自动添加 Cookie
            getCookieString(url).takeIf { it.isNotEmpty() }?.let {
                requestBuilder.addHeader("Cookie", it)
            }
            
            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            
            // 检查响应码
            if (!response.isSuccessful) {
                logger.warn("HTTP POST 失败: $url - ${response.code}")
                return ""
            }
            
            // 解析 Set-Cookie
            parseSetCookies(url, response.headers("Set-Cookie"))
            
            response.body?.string() ?: ""
        } catch (e: Exception) {
            logger.error("HTTP POST 请求失败: $url", e)
            ""
        }
    }
    
    /**
     * HTTP POST 请求（JSON）
     * @param url 请求地址
     * @param jsonBody JSON 请求体
     * @param headers 请求头
     * @return 响应内容
     */
    fun httpPostJson(url: String, jsonBody: String, headers: Map<String, String>? = null): String {
        return try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)
            
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
            
            headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            getCookieString(url).takeIf { it.isNotEmpty() }?.let {
                requestBuilder.addHeader("Cookie", it)
            }
            
            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                logger.warn("HTTP POST JSON 失败: $url - ${response.code}")
                return ""
            }
            
            parseSetCookies(url, response.headers("Set-Cookie"))
            
            response.body?.string() ?: ""
        } catch (e: Exception) {
            logger.error("HTTP POST JSON 请求失败: $url", e)
            ""
        }
    }
    
    private fun String.toRequestBody(mediaType: MediaType): okhttp3.RequestBody {
        return okhttp3.RequestBody.create(mediaType, this)
    }
    
    private fun String.toMediaType(): MediaType {
        return this.toMediaTypeOrNull() ?: "text/plain".toMediaType()
    }
}
