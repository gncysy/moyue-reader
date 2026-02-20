package com.moyue.compat

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AndroidCompat(
    private val okHttpClient: OkHttpClient
) {
    
    private val stringPrefs = ConcurrentHashMap<String, String>()
    private val intPrefs = ConcurrentHashMap<String, Int>()
    private val booleanPrefs = ConcurrentHashMap<String, Boolean>()
    private val cookies = ConcurrentHashMap<String, MutableMap<String, String>>()
    
    // ==================== Base64 兼容 ====================
    
    fun base64Encode(data: ByteArray, flags: Int): String {
        val encoder = when (flags) {
            2 -> Base64.getUrlEncoder().withoutPadding()
            8 -> Base64.getEncoder().withoutPadding()
            else -> Base64.getEncoder()
        }
        return encoder.encodeToString(data)
    }
    
    fun base64Decode(str: String, flags: Int): ByteArray {
        val decoder = when (flags) {
            2 -> Base64.getUrlDecoder()
            else -> Base64.getDecoder()
        }
        return decoder.decode(str)
    }
    
    // ==================== SharedPreferences ====================
    
    fun getString(key: String, defValue: String): String = stringPrefs[key] ?: defValue
    fun putString(key: String, value: String) { stringPrefs[key] = value }
    fun getInt(key: String, defValue: Int): Int = intPrefs[key] ?: defValue
    fun putInt(key: String, value: Int) { intPrefs[key] = value }
    fun getBoolean(key: String, defValue: Boolean): Boolean = booleanPrefs[key] ?: defValue
    fun putBoolean(key: String, value: Boolean) { booleanPrefs[key] = value }
    
    // ==================== Cookie ====================
    
    fun getCookie(url: String, name: String): String? {
        val host = extractHost(url)
        return cookies[host]?.get(name)
    }
    
    fun setCookie(url: String, name: String, value: String) {
        val host = extractHost(url)
        cookies.computeIfAbsent(host) { mutableMapOf() }[name] = value
    }
    
    fun getCookieString(url: String): String {
        val host = extractHost(url)
        return cookies[host]?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""
    }
    
    private fun extractHost(url: String): String = try { java.net.URL(url).host } catch (e: Exception) { url }
    
    // ==================== 加密 ====================
    
    fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    fun sha1(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-1")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }
    
    fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }
    
    // ==================== 网络请求 ====================
    
    fun httpGet(url: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url).get()
        headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        getCookieString(url).takeIf { it.isNotEmpty() }?.let { requestBuilder.addHeader("Cookie", it) }
        
        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        response.headers("Set-Cookie").forEach { cookie ->
            val parts = cookie.split(";")[0].split("=", limit = 2)
            if (parts.size == 2) setCookie(url, parts[0], parts[1])
        }
        return response.body?.string() ?: ""
    }
    
    fun httpPost(url: String, body: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body.toRequestBody())
        headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        getCookieString(url).takeIf { it.isNotEmpty() }?.let { requestBuilder.addHeader("Cookie", it) }
        
        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        response.headers("Set-Cookie").forEach { cookie ->
            val parts = cookie.split(";")[0].split("=", limit = 2)
            if (parts.size == 2) setCookie(url, parts[0], parts[1])
        }
        return response.body?.string() ?: ""
    }
    
    private fun String.toRequestBody(): okhttp3.RequestBody {
        return okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("application/x-www-form-urlencoded"),
            this
        )
    }
}
