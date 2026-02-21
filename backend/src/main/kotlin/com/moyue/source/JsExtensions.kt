package com.moyue.source

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.service.CacheService
import com.moyue.service.PreferenceService
import com.moyue.util.*
import okhttp3.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 书源 JavaScript 扩展 API
 * 
 * 完全兼容 Legado 的 java.* 接口
 * 提供网络请求、加密解密、文件操作、Cookie 管理等功能
 * 
 * @author Moyue
 * @since 1.0.0
 */
@Component
class JsExtensions(
    private val okHttpClient: OkHttpClient,
    private val cacheService: CacheService,
    private val preferenceService: PreferenceService
) {
    
    private val logger = LoggerFactory.getLogger(JsExtensions::class.java)
    
    // 线程本地存储，保存当前执行上下文
    private val contextHolder = ThreadLocal<ExecutionContext>()
    
    /**
     * 执行上下文
     */
    data class ExecutionContext(
        var content: Any? = null,
        var baseUrl: String = "",
        var rule: Any? = null,
        var book: Book? = null,
        var chapter: BookChapter? = null,
        var source: BookSource? = null,
        var variable: String? = null
    )
    
    /**
     * 设置执行上下文
     */
    fun setContext(ctx: ExecutionContext) {
        contextHolder.set(ctx)
    }
    
    /**
     * 清除执行上下文
     */
    fun clearContext() {
        contextHolder.remove()
    }
    
    /**
     * 获取当前执行上下文
     */
    private fun getContext(): ExecutionContext {
        return contextHolder.get() ?: ExecutionContext()
    }
    
    // ==================== 网络请求 ====================
    
    /**
     * AJAX 请求（默认 GET）
     */
    @JvmOverloads
    fun ajax(url: String, headers: Map<String, String>? = null): String {
        return get(url, headers)
    }
    
    /**
     * HTTP GET 请求
     */
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
    
    /**
     * HTTP POST 请求（表单）
     */
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
    
    /**
     * HTTP POST 请求（JSON）
     */
    fun postJson(url: String, json: String, headers: Map<String, String>? = null): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)
        
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    /**
     * HTTP PUT 请求
     */
    fun put(url: String, body: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url).put(body.toRequestBody())
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    /**
     * HTTP DELETE 请求
     */
    fun delete(url: String, headers: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url).delete()
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return executeRequest(requestBuilder.build())
    }
    
    /**
     * 执行 HTTP 请求
     */
    private fun executeRequest(request: Request): String {
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.warn("HTTP 请求失败: ${request.url} - ${response.code}")
                    throw RuntimeException("HTTP ${response.code}: ${response.message}")
                }
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            logger.error("HTTP 请求失败: ${request.url}", e)
            throw RuntimeException("Request failed: ${e.message}", e)
        }
    }
    
    /**
     * 获取原始 Response 对象（需要手动关闭）
     * 
     * 警告：调用者必须手动关闭 Response
     */
    fun getResponse(url: String, headers: Map<String, String>? = null): Response {
        val requestBuilder = Request.Builder().url(url)
        
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        logger.warn("使用了 getResponse，请确保手动关闭 Response")
        return okHttpClient.newCall(requestBuilder.build()).execute()
    }
    
    // ==================== 编码加密 ====================
    
    /**
     * Base64 编码
     */
    fun base64Encode(str: String): String {
        return try {
            Base64.getEncoder().encodeToString(str.toByteArray())
        } catch (e: Exception) {
            logger.error("Base64 编码失败", e)
            ""
        }
    }
    
    /**
     * Base64 解码
     */
    fun base64Decode(str: String): String {
        return try {
            String(Base64.getDecoder().decode(str))
        } catch (e: Exception) {
            logger.error("Base64 解码失败", e)
            ""
        }
    }
    
    /**
     * MD5 加密
     */
    fun md5Encode(str: String): String {
        return MD5Utils.md5Encode(str)
    }
    
    /**
     * SHA1 加密
     */
    fun sha1Encode(str: String): String {
        return MD5Utils.sha1Encode(str)
    }
    
    /**
     * AES 解密
     */
    fun aesDecodeToString(data: String, key: String, iv: String): String {
        return AESUtils.decodeToString(data, key, iv)
    }
    
    /**
     * AES 加密
     */
    fun aesEncodeToString(data: String, key: String, iv: String): String {
        return AESUtils.encodeToString(data, key, iv)
    }
    
    /**
     * RSA 解密
     */
    fun rsaDecodeToString(data: String, key: String): String {
        return RSAUtils.decodeToString(data, key)
    }
    
    /**
     * RSA 加密
     */
    fun rsaEncodeToString(data: String, key: String): String {
        return RSAUtils.encodeToString(data, key)
    }
    
    /**
     * 3DES 解密
     */
    fun desDecodeToString(data: String, key: String): String {
        return DESUtils.decodeToString(data, key)
    }
    
    /**
     * 3DES 加密
     */
    fun desEncodeToString(data: String, key: String): String {
        return DESUtils.encodeToString(data, key)
    }
    
    // ==================== 文件操作 ====================
    
    /**
     * 获取工作目录
     */
    private fun getWorkDir(): String {
        val homeDir = System.getProperty("user.home")
        return File(homeDir, "MoyueData").absolutePath
    }
    
    /**
     * 获取缓存目录
     */
    fun getCacheDir(): String {
        val dir = File(getWorkDir(), "cache")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
    
    /**
     * 获取文件目录
     */
    fun getFilesDir(): String {
        val dir = File(getWorkDir(), "files")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
    
    /**
     * 获取外部文件目录
     */
    fun getExternalFilesDir(): String {
        return getFilesDir()
    }
    
    /**
     * 读取文件
     * 
     * 限制：只能在 MoyueData 目录下操作
     */
    fun getFile(path: String): ByteArray? {
        return try {
            validatePath(path)
            File(path).readBytes()
        } catch (e: Exception) {
            logger.error("读取文件失败: $path", e)
            null
        }
    }
    
    /**
     * 写入文件
     * 
     * 限制：只能在 MoyueData 目录下操作
     */
    fun putFile(path: String, data: ByteArray) {
        try {
            validatePath(path)
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
        } catch (e: Exception) {
            logger.error("写入文件失败: $path", e)
            throw RuntimeException("写入文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 删除文件
     * 
     * 限制：只能在 MoyueData 目录下操作
     */
    fun deleteFile(path: String): Boolean {
        return try {
            validatePath(path)
            File(path).delete()
        } catch (e: Exception) {
            logger.error("删除文件失败: $path", e)
            false
        }
    }
    
    /**
     * 检查文件是否存在
     * 
     * 限制：只能在 MoyueData 目录下操作
     */
    fun fileExist(path: String): Boolean {
        return try {
            validatePath(path)
            File(path).exists()
        } catch (e: Exception) {
            logger.error("检查文件失败: $path", e)
            false
        }
    }
    
    /**
     * 获取目录下的文件列表
     * 
     * 限制：只能在 MoyueData 目录下操作
     */
    fun getFiles(dir: String): List<String> {
        return try {
            validatePath(dir)
            val directory = File(dir)
            directory.list()?.toList() ?: emptyList()
        } catch (e: Exception) {
            logger.error("获取文件列表失败: $dir", e)
            emptyList()
        }
    }
    
    /**
     * 创建目录
     * 
     * 限制：只能在 MoyueData 目录下操作
     */
    fun mkdirs(path: String): Boolean {
        return try {
            validatePath(path)
            File(path).mkdirs()
        } catch (e: Exception) {
            logger.error("创建目录失败: $path", e)
            false
        }
    }
    
    /**
     * 获取文件大小
     */
    fun getFileSize(path: String): Long {
        return try {
            validatePath(path)
            File(path).length()
        } catch (e: Exception) {
            logger.error("获取文件大小失败: $path", e)
            0L
        }
    }
    
    /**
     * 获取文件最后修改时间
     */
    fun getFileLastModified(path: String): Long {
        return try {
            validatePath(path)
            File(path).lastModified()
        } catch (e: Exception) {
            logger.error("获取文件修改时间失败: $path", e)
            0L
        }
    }
    
    /**
     * 验证文件路径安全性
     * 防止路径遍历攻击
     */
    private fun validatePath(path: String) {
        val file = File(path).canonicalFile
        val workDir = File(getWorkDir()).canonicalFile
        
        if (!file.absolutePath.startsWith(workDir.absolutePath)) {
            throw SecurityException("不允许访问工作目录之外的文件: $path")
        }
    }
    
    // ==================== Cookie 管理 ====================
    
    /**
     * 获取所有 Cookie
     */
    fun getCookies(url: String): String {
        return cacheService.getCookies(url)
    }
    
    /**
     * 获取指定 Cookie
     */
    fun getCookie(url: String, name: String): String? {
        return cacheService.getCookie(url, name)
    }
    
    /**
     * 设置 Cookie
     */
    fun setCookie(url: String, cookie: String) {
        cacheService.setCookie(url, cookie)
    }
    
    /**
     * 移除 Cookie
     */
    fun removeCookie(url: String) {
        cacheService.removeCookie(url)
    }
    
    /**
     * 移除所有 Cookie
     */
    fun removeAllCookies() {
        cacheService.removeAllCookies()
    }
    
    // ==================== 上下文操作 ====================
    
    /**
     * 设置内容和 Base URL
     */
    fun setContent(content: Any?, baseUrl: String) {
        val ctx = getContext()
        ctx.content = content
        ctx.baseUrl = baseUrl
        contextHolder.set(ctx)
    }
    
    /**
     * 获取内容
     */
    fun getContent(): Any? {
        return getContext().content
    }
    
    /**
     * 获取 Base URL
     */
    fun getBaseUrl(): String {
        return getContext().baseUrl
    }
    
    /**
     * 设置 Base URL
     */
    fun setBaseUrl(baseUrl: String) {
        val ctx = getContext()
        ctx.baseUrl = baseUrl
        contextHolder.set(ctx)
    }
    
    /**
     * 获取规则
     */
    fun getRule(): Any? {
        return getContext().rule
    }
    
    /**
     * 设置规则
     */
    fun setRule(rule: Any?) {
        val ctx = getContext()
        ctx.rule = rule
        contextHolder.set(ctx)
    }
    
    /**
     * 获取书籍
     */
    fun getBook(): Book? {
        return getContext().book
    }
    
    /**
     * 设置书籍
     */
    fun setBook(book: Book?) {
        val ctx = getContext()
        ctx.book = book
        contextHolder.set(ctx)
    }
    
    /**
     * 获取章节
     */
    fun getChapter(): BookChapter? {
        return getContext().chapter
    }
    
    /**
     * 设置章节
     */
    fun setChapter(chapter: BookChapter?) {
        val ctx = getContext()
        ctx.chapter = chapter
        contextHolder.set(ctx)
    }
    
    /**
     * 获取书源
     */
    fun getSource(): BookSource? {
        return getContext().source
    }
    
    /**
     * 设置书源
     */
    fun setSource(source: BookSource?) {
        val ctx = getContext()
        ctx.source = source
        contextHolder.set(ctx)
    }
    
    /**
     * 获取变量
     */
    fun getVariable(): String? {
        return getContext().variable
    }
    
    /**
     * 设置变量
     */
    fun setVariable(variable: String?) {
        val ctx = getContext()
        ctx.variable = variable
        contextHolder.set(ctx)
    }
    
    // ==================== 数据存储 ====================
    
    /**
     * 获取缓存
     */
    fun get(key: String): Any? {
        return cacheService.get(key)
    }
    
    /**
     * 设置缓存
     */
    fun put(key: String, value: Any?) {
        if (value != null) {
            cacheService.put(key, value)
        }
    }
    
    /**
     * 移除缓存
     */
    fun remove(key: String) {
        cacheService.remove(key)
    }
    
    /**
     * 检查缓存是否存在
     */
    fun contains(key: String): Boolean {
        return cacheService.contains(key)
    }
    
    /**
     * 获取所有缓存 key
     */
    fun keys(): List<String> {
        return cacheService.keys()
    }
    
    /**
     * 清空所有缓存
     */
    fun clear() {
        cacheService.clear()
    }
    
    // ==================== 偏好设置 ====================
    
    /**
     * 获取字符串偏好设置
     */
    fun getPrefString(key: String, default: String): String {
        return preferenceService.getString(key, default)
    }
    
    /**
     * 设置字符串偏好设置
     */
    fun putPrefString(key: String, value: String) {
        preferenceService.putString(key, value)
    }
    
    /**
     * 获取整数偏好设置
     */
    fun getPrefInt(key: String, default: Int): Int {
        return preferenceService.getInt(key, default)
    }
    
    /**
     * 设置整数偏好设置
     */
    fun putPrefInt(key: String, value: Int) {
        preferenceService.putInt(key, value)
    }
    
    /**
     * 获取布尔偏好设置
     */
    fun getPrefBoolean(key: String, default: Boolean): Boolean {
        return preferenceService.getBoolean(key, default)
    }
    
    /**
     * 设置布尔偏好设置
     */
    fun putPrefBoolean(key: String, value: Boolean) {
        preferenceService.putBoolean(key, value)
    }
    
    /**
     * 获取长整数偏好设置
     */
    fun getPrefLong(key: String, default: Long): Long {
        return preferenceService.getLong(key, default)
    }
    
    /**
     * 设置长整数偏好设置
     */
    fun putPrefLong(key: String, value: Long) {
        preferenceService.putLong(key, value)
    }
    
    /**
     * 获取浮点数偏好设置
     */
    fun getPrefFloat(key: String, default: Float): Float {
        return preferenceService.getFloat(key, default)
    }
    
    /**
     * 设置浮点数偏好设置
     */
    fun putPrefFloat(key: String, value: Float) {
        preferenceService.putFloat(key, value)
    }
    
    /**
     * 获取字符串集合偏好设置
     */
    fun getPrefStringSet(key: String, default: Set<String>): Set<String> {
        return preferenceService.getStringSet(key, default)
    }
    
    /**
     * 设置字符串集合偏好设置
     */
    fun putPrefStringSet(key: String, value: Set<String>) {
        preferenceService.putStringSet(key, value)
    }
    
    /**
     * 移除偏好设置
     */
    fun removePref(key: String) {
        preferenceService.remove(key)
    }
    
    /**
     * 清空所有偏好设置
     */
    fun clearPref() {
        preferenceService.clear()
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 打印消息
     */
    fun print(msg: String) {
        logger.info(msg)
    }
    
    /**
     * 记录日志
     */
    fun log(msg: String) {
        logger.info("[LOG] $msg")
    }
    
    /**
     * 记录错误日志
     */
    fun loge(msg: String) {
        logger.error("[ERROR] $msg")
    }
    
    /**
     * 格式化当前时间
     */
    fun timeFormat(format: String): String {
        return SimpleDateFormat(format).format(Date())
    }
    
    /**
     * 格式化指定时间戳
     */
    fun timeFormat(timestamp: Long, format: String): String {
        return SimpleDateFormat(format).format(Date(timestamp))
    }
    
    /**
     * 解析时间字符串
     */
    fun parseTime(time: String, format: String): Long {
        return try {
            SimpleDateFormat(format).parse(time).time
        } catch (e: Exception) {
            logger.error("解析时间失败: $time", e)
            0L
        }
    }
    
    /**
     * 获取网络类型
     */
    fun getNetworkType(): String {
        return "WIFI"
    }
    
    /**
     * 打开浏览器
     */
    fun startBrowser(url: String, title: String) {
        try {
            val os = System.getProperty("os.name").lowercase()
            val command = when {
                os.contains("win") -> "rundll32 url.dll,FileProtocolHandler $url"
                os.contains("mac") -> "open $url"
                else -> "xdg-open $url"
            }
            Runtime.getRuntime().exec(command)
        } catch (e: Exception) {
            logger.error("打开浏览器失败: $url", e)
        }
    }
    
    /**
     * 打开浏览器（带 headers）
     */
    fun startBrowser(url: String, title: String, headers: Map<String, String>) {
        startBrowser(url, title)
    }
    
    /**
     * 打开浏览器（带 headers 和 css）
     */
    fun startBrowser(url: String, title: String, headers: Map<String, String>, css: String) {
        startBrowser(url, title)
    }
}
