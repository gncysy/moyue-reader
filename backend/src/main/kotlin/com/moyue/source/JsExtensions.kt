package com.moyue.source

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.service.CacheService
import com.moyue.service.PreferenceService
import com.moyue.util.MD5Utils
import com.moyue.util.AESUtils
import com.moyue.util.RSAUtils
import com.moyue.util.DESUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import org.springframework.stereotype.Component
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 书源 JavaScript 扩展 API
 * 完全兼容 Legado 的 java.* 接口
 */
@Component
class JsExtensions(
    private val okHttpClient: OkHttpClient,
    private val cacheService: CacheService,
    private val preferenceService: PreferenceService
) {
    
    // 线程本地存储，保存当前执行上下文
    private val contextHolder = ThreadLocal<ExecutionContext>()
    
    data class ExecutionContext(
        var content: Any? = null,
        var baseUrl: String = "",
        var rule: Any? = null,
        var book: Book? = null,
        var chapter: BookChapter? = null,
        var source: BookSource? = null,
        var variable: String? = null
    )
    
    fun setContext(ctx: ExecutionContext) {
        contextHolder.set(ctx)
    }
    
    fun clearContext() {
        contextHolder.remove()
    }
    
    private fun getContext(): ExecutionContext {
        return contextHolder.get() ?: ExecutionContext()
    }
    
    // ==================== 网络请求 ====================
    
    @JvmOverloads
    fun ajax(url: String, headers: Map<String, String>? = null): String {
        return get(url, headers)
    }
    
    @JvmOverloads
    fun get(url: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url).get()
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // 默认 User-Agent
        if (headers?.containsKey("User-Agent") != true) {
            requestBuilder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        }
        
        val request = requestBuilder.build()
        return executeRequest(request)
    }
    
    @JvmOverloads
    fun post(url: String, body: String, headers: Map<String, String>? = null): String {
        val mediaType = "application/x-www-form-urlencoded; charset=utf-8".toMediaType()
        val requestBody = body.toRequestBody(mediaType)
        
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    fun postJson(url: String, json: String, headers: Map<String, String>? = null): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)
        
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    fun put(url: String, body: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url).put(body.toRequestBody())
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    fun delete(url: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url).delete()
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    private fun executeRequest(request: Request): String {
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("HTTP ${response.code}: ${response.message}")
                }
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            throw RuntimeException("Request failed: ${e.message}", e)
        }
    }
    
    fun getResponse(url: String, headers: Map<String, String>? = null): Response {
        val requestBuilder = Request.Builder().url(url)
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return okHttpClient.newCall(requestBuilder.build()).execute()
    }
    
    // ==================== 编码加密 ====================
    
    fun base64Encode(str: String): String {
        return Base64.getEncoder().encodeToString(str.toByteArray())
    }
    
    fun base64Decode(str: String): String {
        return String(Base64.getDecoder().decode(str))
    }
    
    fun md5Encode(str: String): String {
        return MD5Utils.md5Encode(str)
    }
    
    fun sha1Encode(str: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(str.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun aesDecodeToString(data: String, key: String, iv: String): String {
        return AESUtils.decodeToString(data, key, iv)
    }
    
    fun aesEncodeToString(data: String, key: String, iv: String): String {
        return AESUtils.encodeToString(data, key, iv)
    }
    
    fun rsaDecodeToString(data: String, key: String): String {
        return RSAUtils.decodeToString(data, key)
    }
    
    fun rsaEncodeToString(data: String, key: String): String {
        return RSAUtils.encodeToString(data, key)
    }
    
    fun desDecodeToString(data: String, key: String): String {
        return DESUtils.decodeToString(data, key)
    }
    
    fun desEncodeToString(data: String, key: String): String {
        return DESUtils.encodeToString(data, key)
    }
    
    // ==================== 文件操作 ====================
    
    private fun getWorkDir(): String {
        return System.getProperty("user.dir") + "/MoyueData"
    }
    
    fun getCacheDir(): String {
        val dir = File(getWorkDir(), "cache")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
    
    fun getFilesDir(): String {
        val dir = File(getWorkDir(), "files")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
    
    fun getExternalFilesDir(): String {
        return getFilesDir()
    }
    
    fun getFile(path: String): ByteArray? {
        return try {
            File(path).readBytes()
        } catch (e: Exception) {
            null
        }
    }
    
    fun putFile(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }
    
    fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }
    
    fun fileExist(path: String): Boolean {
        return File(path).exists()
    }
    
    fun getFiles(dir: String): List<String> {
        val directory = File(dir)
        return directory.list()?.toList() ?: emptyList()
    }
    
    fun mkdirs(path: String): Boolean {
        return File(path).mkdirs()
    }
    
    fun getFileSize(path: String): Long {
        return File(path).length()
    }
    
    fun getFileLastModified(path: String): Long {
        return File(path).lastModified()
    }
    
    // ==================== Cookie 管理 ====================
    
    fun getCookies(url: String): String {
        return cacheService.getCookies(url)
    }
    
    fun getCookie(url: String, name: String): String? {
        return cacheService.getCookie(url, name)
    }
    
    fun setCookie(url: String, cookie: String) {
        cacheService.setCookie(url, cookie)
    }
    
    fun removeCookie(url: String) {
        cacheService.removeCookie(url)
    }
    
    fun removeAllCookies() {
        cacheService.removeAllCookies()
    }
    
    // ==================== 上下文操作 ====================
    
    fun setContent(content: Any?, baseUrl: String) {
        val ctx = getContext()
        ctx.content = content
        ctx.baseUrl = baseUrl
        contextHolder.set(ctx)
    }
    
    fun getContent(): Any? {
        return getContext().content
    }
    
    fun getBaseUrl(): String {
        return getContext().baseUrl
    }
    
    fun setBaseUrl(baseUrl: String) {
        val ctx = getContext()
        ctx.baseUrl = baseUrl
        contextHolder.set(ctx)
    }
    
    fun getRule(): Any? {
        return getContext().rule
    }
    
    fun setRule(rule: Any?) {
        val ctx = getContext()
        ctx.rule = rule
        contextHolder.set(ctx)
    }
    
    fun getBook(): Book? {
        return getContext().book
    }
    
    fun setBook(book: Book?) {
        val ctx = getContext()
        ctx.book = book
        contextHolder.set(ctx)
    }
    
    fun getChapter(): BookChapter? {
        return getContext().chapter
    }
    
    fun setChapter(chapter: BookChapter?) {
        val ctx = getContext()
        ctx.chapter = chapter
        contextHolder.set(ctx)
    }
    
    fun getSource(): BookSource? {
        return getContext().source
    }
    
    fun setSource(source: BookSource?) {
        val ctx = getContext()
        ctx.source = source
        contextHolder.set(ctx)
    }
    
    fun getVariable(): String? {
        return getContext().variable
    }
    
    fun setVariable(variable: String?) {
        val ctx = getContext()
        ctx.variable = variable
        contextHolder.set(ctx)
    }
    
    // ==================== 数据存储 ====================
    
    fun get(key: String): Any? {
        return cacheService.get(key)
    }
    
    fun put(key: String, value: Any?) {
        if (value != null) {
            cacheService.put(key, value)
        }
    }
    
    fun remove(key: String) {
        cacheService.remove(key)
    }
    
    fun contains(key: String): Boolean {
        return cacheService.contains(key)
    }
    
    fun keys(): List<String> {
        return cacheService.keys()
    }
    
    fun clear() {
        cacheService.clear()
    }
    
    // ==================== 偏好设置 ====================
    
    fun getPrefString(key: String, default: String): String {
        return preferenceService.getString(key, default)
    }
    
    fun putPrefString(key: String, value: String) {
        preferenceService.putString(key, value)
    }
    
    fun getPrefInt(key: String, default: Int): Int {
        return preferenceService.getInt(key, default)
    }
    
    fun putPrefInt(key: String, value: Int) {
        preferenceService.putInt(key, value)
    }
    
    fun getPrefBoolean(key: String, default: Boolean): Boolean {
        return preferenceService.getBoolean(key, default)
    }
    
    fun putPrefBoolean(key: String, value: Boolean) {
        preferenceService.putBoolean(key, value)
    }
    
    fun getPrefLong(key: String, default: Long): Long {
        return preferenceService.getLong(key, default)
    }
    
    fun putPrefLong(key: String, value: Long) {
        preferenceService.putLong(key, value)
    }
    
    fun getPrefFloat(key: String, default: Float): Float {
        return preferenceService.getFloat(key, default)
    }
    
    fun putPrefFloat(key: String, value: Float) {
        preferenceService.putFloat(key, value)
    }
    
    fun getPrefStringSet(key: String, default: Set<String>): Set<String> {
        return preferenceService.getStringSet(key, default)
    }
    
    fun putPrefStringSet(key: String, value: Set<String>) {
        preferenceService.putStringSet(key, value)
    }
    
    fun removePref(key: String) {
        preferenceService.remove(key)
    }
    
    fun clearPref() {
        preferenceService.clear()
    }
    
    // ==================== 工具方法 ====================
    
    fun print(msg: String) {
        println(msg)
    }
    
    fun log(msg: String) {
        println("[LOG] $msg")
    }
    
    fun loge(msg: String) {
        System.err.println("[ERROR] $msg")
    }
    
    fun timeFormat(format: String): String {
        return java.text.SimpleDateFormat(format).format(java.util.Date())
    }
    
    fun timeFormat(timestamp: Long, format: String): String {
        return java.text.SimpleDateFormat(format).format(java.util.Date(timestamp))
    }
    
    fun parseTime(time: String, format: String): Long {
        return java.text.SimpleDateFormat(format).parse(time).time
    }
    
    fun getNetworkType(): String {
        return "WIFI"
    }
    
    fun startBrowser(url: String, title: String) {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("win") -> "rundll32 url.dll,FileProtocolHandler $url"
            os.contains("mac") -> "open $url"
            else -> "xdg-open $url"
        }
        try {
            Runtime.getRuntime().exec(command)
        } catch (e: Exception) {
            // ignore
        }
    }
    
    fun startBrowser(url: String, title: String, headers: Map<String, String>) {
        startBrowser(url, title)
    }
    
    fun startBrowser(url: String, title: String, headers: Map<String, String>, css: String) {
        startBrowser(url, title)
    }
}
