package com.moyue.security

import okhttp3.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

class SafeJsExtensions(
    private val policy: SecurityPolicy,
    private val okHttpClient: OkHttpClient? = null
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(SafeJsExtensions::class.java)
        
        // 内网 IP 地址段
        private val PRIVATE_IP_RANGES = listOf(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "fc00::/7"
        )
        
        // 允许的 HTTP 方法
        private val ALLOWED_HTTP_METHODS = setOf("GET", "POST", "HEAD", "OPTIONS")
        
        // Cookie 存储
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
        
        // 请求限流记录（按时间窗口）
        private val requestHistory = ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>>()
        
        // 请求 ID 生成器
        private val requestIdGenerator = AtomicLong(0)
    }
    
    private val sandboxRoot = Paths.get(policy.sandboxRoot).normalize()
    
    // 请求计数器（按会话）
    private val sessionRequestCount = AtomicInteger(0)
    
    // OkHttp 客户端（使用传入的或创建新的）
    private val httpClient = okHttpClient ?: createOkHttpClient()
    
    // 线程安全的 SimpleDateFormat 缓存
    private val dateFormatCache = ThreadLocal<SimpleDateFormat>()
    
    init {
        try {
            Files.createDirectories(sandboxRoot)
        } catch (e: Exception) {
            logger.error("沙箱目录创建失败: ${sandboxRoot}", e)
            throw SecurityException("沙箱目录初始化失败", e)
        }
        
        logger.info("安全扩展初始化完成，策略级别: ${policy.level.name}")
    }
    
    /**
     * HTTP 请求（增强版）
     */
    fun ajax(
        url: String,
        method: String = "GET",
        headers: Map<String, String>? = null,
        body: String? = null,
        timeout: Long? = null,
        allowRedirects: Boolean = policy.allowRedirects,
        validateSSL: Boolean = true
    ): String {
        val requestId = requestIdGenerator.incrementAndGet()
        logger.debug("请求开始 [$requestId]: $method $url")
        
        try {
            // 检查请求限制
            checkRequestLimit()
            
            // 验证 URL
            validateUrl(url)
            
            // 验证 HTTP 方法
            val upperMethod = method.uppercase()
            if (!ALLOWED_HTTP_METHODS.contains(upperMethod)) {
                throw SecurityException("不允许的 HTTP 方法: $method")
            }
            
            // 内网地址检查
            if (!policy.allowInternalNetwork) {
                checkInternalNetwork(url)
            }
            
            // 域名黑名单检查
            checkBlockedDomain(url)
            
            // 构建请求
            val requestBuilder = Request.Builder()
                .url(url)
                .method(upperMethod, body?.toRequestBody())
            
            // 添加默认头部
            requestBuilder.addHeader("User-Agent", "MoyueReader/1.0")
            
            // 添加自定义头部
            headers?.forEach { (k, v) ->
                if (!isHeaderBlocked(k)) {
                    requestBuilder.addHeader(k, v)
                }
            }
            
            // 添加 Cookie
            val domain = URL(url).host
            val cookies = cookieStore[domain]?.joinToString("; ") { "${it.name}=${it.value}" } ?: ""
            if (cookies.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", cookies)
            }
            
            // 构建客户端（控制重定向）
            val clientBuilder = httpClient.newBuilder()
                .connectTimeout(timeout ?: policy.timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeout ?: policy.timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout ?: policy.timeoutMs, TimeUnit.MILLISECONDS)
            
            if (!allowRedirects) {
                clientBuilder.followRedirects(false)
                clientBuilder.followSslRedirects(false)
            }
            
            if (!validateSSL) {
                clientBuilder.hostnameVerifier(TrustAllHostnameVerifier())
                clientBuilder.sslSocketFactory(
                    TrustAllSSLSocketFactory(),
                    TrustAllHostnameVerifier()
                )
            }
            
            val client = clientBuilder.build()
            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            
            // 保存 Cookie
            saveCookies(response, url)
            
            // 检查响应状态
            if (!response.isSuccessful) {
                logger.warn("请求失败 [$requestId]: ${response.code}")
                throw SecurityException("HTTP 请求失败: ${response.code}")
            }
            
            val responseBody = response.body
            if (responseBody == null) {
                throw SecurityException("响应体为空")
            }
            
            // 检查响应大小
            val contentLength = responseBody.contentLength()
            if (contentLength > 0 && contentLength > policy.maxFileSize) {
                throw SecurityException("响应超过大小限制: ${contentLength} > ${policy.maxFileSize}")
            }
            
            val result = responseBody.string()
            logger.debug("请求成功 [$requestId]: ${result.length} bytes")
            
            return result
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            logger.error("请求异常 [$requestId]: $url", e)
            throw SecurityException("HTTP 请求异常: ${e.message}", e)
        }
    }
    
    /**
     * 获取文件
     */
    fun getFile(path: String): ByteArray {
        if (!policy.allowFile) {
            throw SecurityException("当前安全模式不允许文件操作")
        }
        
        val target = sandboxRoot.resolve(path).normalize()
        
        // 路径越界检查
        if (!target.startsWith(sandboxRoot)) {
            logger.warn("路径越界尝试: $path -> $target")
            throw SecurityException("禁止访问沙箱外路径")
        }
        
        // 文件存在性检查
        if (!Files.exists(target)) {
            throw SecurityException("文件不存在: $path")
        }
        
        if (!Files.isRegularFile(target)) {
            throw SecurityException("不是常规文件: $path")
        }
        
        // 文件类型检查
        val filename = target.fileName.toString()
        if (policy.isFileBlocked(filename)) {
            throw SecurityException("禁止的文件类型: $filename")
        }
        
        try {
            return Files.readAllBytes(target)
        } catch (e: Exception) {
            logger.error("读取文件失败: $path", e)
            throw SecurityException("读取文件失败: ${e.message}")
        }
    }
    
    /**
     * 写入文件
     */
    fun putFile(path: String, data: ByteArray) {
        if (!policy.allowFile) {
            throw SecurityException("当前安全模式不允许文件操作")
        }
        
        // 数据大小检查
        if (data.size > policy.maxFileSize) {
            throw SecurityException("文件过大: ${data.size} > ${policy.maxFileSize}")
        }
        
        val target = sandboxRoot.resolve(path).normalize()
        
        // 路径越界检查
        if (!target.startsWith(sandboxRoot)) {
            throw SecurityException("禁止写入沙箱外路径")
        }
        
        // 文件名检查
        val filename = target.fileName.toString()
        if (policy.isFileBlocked(filename)) {
            throw SecurityException("禁止写入的文件类型: $filename")
        }
        
        // 文件扩展名检查
        if (!policy.isFileExtensionAllowed(filename)) {
            throw SecurityException("不允许的文件扩展名: $filename")
        }
        
        try {
            Files.createDirectories(target.parent)
            Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            logger.debug("写入文件: $path (${data.size} bytes)")
        } catch (e: Exception) {
            logger.error("写入文件失败: $path", e)
            throw SecurityException("写入文件失败: ${e.message}")
        }
    }
    
    /**
     * 删除文件
     */
    fun deleteFile(path: String) {
        if (!policy.allowFile) {
            throw SecurityException("当前安全模式不允许文件操作")
        }
        
        val target = sandboxRoot.resolve(path).normalize()
        
        if (!target.startsWith(sandboxRoot)) {
            throw SecurityException("禁止删除沙箱外文件")
        }
        
        try {
            Files.deleteIfExists(target)
            logger.debug("删除文件: $path")
        } catch (e: Exception) {
            logger.error("删除文件失败: $path", e)
            throw SecurityException("删除文件失败: ${e.message}")
        }
    }
    
    /**
     * 列出文件
     */
    fun listFiles(path: String = ""): List<String> {
        if (!policy.allowFile) {
            throw SecurityException("当前安全模式不允许文件操作")
        }
        
        val target = if (path.isBlank()) sandboxRoot else sandboxRoot.resolve(path).normalize()
        
        if (!target.startsWith(sandboxRoot)) {
            throw SecurityException("禁止列出沙箱外路径")
        }
        
        if (!Files.exists(target) || !Files.isDirectory(target)) {
            return emptyList()
        }
        
        return try {
            Files.list(target)
                .filter { Files.isRegularFile(it) }
                .map { it.fileName.toString() }
                .toList()
        } catch (e: Exception) {
            logger.error("列出文件失败: $path", e)
            throw SecurityException("列出文件失败: ${e.message}")
        }
    }
    
    /**
     * MD5 编码
     */
    fun md5Encode(text: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(text.toByteArray(StandardCharsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("MD5 编码失败", e)
            throw SecurityException("MD5 编码失败")
        }
    }
    
    /**
     * SHA256 编码
     */
    fun sha256Encode(text: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(text.toByteArray(StandardCharsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("SHA256 编码失败", e)
            throw SecurityException("SHA256 编码失败")
        }
    }
    
    /**
     * Base64 编码
     */
    fun base64Encode(text: String): String {
        return Base64.getEncoder().encodeToString(text.toByteArray(StandardCharsets.UTF_8))
    }
    
    /**
     * Base64 解码
     */
    fun base64Decode(text: String): String {
        return try {
            String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Base64 解码失败", e)
            throw SecurityException("Base64 解码失败: ${e.message}")
        }
    }
    
    /**
     * URL 编码
     */
    fun urlEncode(text: String): String {
        return java.net.URLEncoder.encode(text, StandardCharsets.UTF_8.name())
    }
    
    /**
     * URL 解码
     */
    fun urlDecode(text: String): String {
        return try {
            java.net.URLDecoder.decode(text, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            logger.error("URL 解码失败", e)
            throw SecurityException("URL 解码失败")
        }
    }
    
    /**
     * 时间格式化（线程安全）
     */
    fun timeFormat(format: String, timestamp: Long): String {
        return try {
            val sdf = dateFormatCache.get() ?: SimpleDateFormat(format).also {
                dateFormatCache.set(it)
            }
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            logger.error("时间格式化失败: $format, $timestamp", e)
            throw SecurityException("时间格式化失败")
        }
    }
    
    /**
     * 获取当前时间戳
     */
    fun currentTimeMillis(): Long = System.currentTimeMillis()
    
    /**
     * 生成随机数
     */
    fun randomInt(min: Int = 0, max: Int = Int.MAX_VALUE): Int {
        return ThreadLocalRandom.current().nextInt(min, max)
    }
    
    /**
     * 日志
     */
    fun log(tag: String, message: String) {
        logger.info("[$tag] $message")
    }
    
    /**
     * 休眠
     */
    fun sleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
    
    /**
     * 获取 Cookie
     */
    fun getCookies(url: String): String {
        val domain = URL(url).host
        return cookieStore[domain]?.joinToString("; ") { "${it.name}=${it.value}" } ?: ""
    }
    
    /**
     * 设置 Cookie
     */
    fun setCookies(url: String, cookieString: String) {
        val domain = URL(url).host
        val cookies = cookieStore.computeIfAbsent(domain) { mutableListOf() }
        
        try {
            val httpCookies = HttpCookie.parse(cookieString)
            cookies.addAll(httpCookies)
        } catch (e: Exception) {
            logger.warn("解析 Cookie 失败: $cookieString")
        }
    }
    
    /**
     * 清除 Cookie
     */
    fun clearCookies(url: String? = null) {
        if (url != null) {
            val domain = URL(url).host
            cookieStore.remove(domain)
        } else {
            cookieStore.clear()
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "sessionRequestCount" to sessionRequestCount.get(),
            "sandboxRoot" to sandboxRoot.toString(),
            "policyLevel" to policy.level.name,
            "cookieStoreSize" to cookieStore.size
        )
    }
    
    // ==================== 私有方法 ====================
    
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private fun checkRequestLimit() {
        val count = sessionRequestCount.incrementAndGet()
        if (count > policy.maxHttpRequests) {
            throw SecurityException("请求次数超过限制: $count > ${policy.maxHttpRequests}")
        }
    }
    
    private fun validateUrl(url: String) {
        val lowerUrl = url.lowercase()
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://") &&
            !lowerUrl.startsWith("ftp://")) {
            throw SecurityException("禁止的协议: ${url.substringBefore("://")}")
        }
    }
    
    private fun checkInternalNetwork(url: String) {
        try {
            val parsedUrl = URL(url)
            val host = parsedUrl.host
            
            // 检查 localhost
            if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1" || host == "::1") {
                throw SecurityException("禁止访问本地地址: $host")
            }
            
            // 检查 IP 地址
            val addr = InetAddress.getByName(host)
            if (isPrivateIP(addr)) {
                logger.warn("拦截内网请求: $host -> $addr")
                throw SecurityException("禁止访问内网地址: $host")
            }
            
            // 检查端口
            val port = parsedUrl.port
            if (port > 0 && (port < 1024 || port in listOf(3306, 3389, 6379, 27017, 5432))) {
                logger.warn("拦截敏感端口请求: $port")
                throw SecurityException("禁止访问敏感端口: $port")
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            logger.warn("内网地址检查失败: $url", e)
            throw SecurityException("内网地址检查失败")
        }
    }
    
    private fun isPrivateIP(addr: InetAddress): Boolean {
        return when (addr) {
            is Inet4Address -> isPrivateIPv4(addr)
            else -> addr.hostAddress?.contains(":") == true // 简化 IPv6 检查
        }
    }
    
    private fun isPrivateIPv4(addr: Inet4Address): Boolean {
        val bytes = addr.address
        return when (bytes[0].toInt() and 0xFF) {
            10 -> true
            172 -> bytes[1].toInt() and 0xFF in 16..31
            192 -> bytes[1].toInt() and 0xFF == 168
            127 -> true
            169 -> bytes[1].toInt() and 0xFF == 254
            else -> false
        }
    }
    
    private fun checkBlockedDomain(url: String) {
        val host = URL(url).host
        if (policy.isDomainBlocked(host)) {
            throw SecurityException("域名已被封禁: $host")
        }
    }
    
    private fun isHeaderBlocked(name: String): Boolean {
        val lowerName = name.lowercase()
        return lowerName.startsWith("host-") || lowerName == "x-forwarded-for"
    }
    
    private fun saveCookies(response: Response, url: String) {
        val cookies = response.headers("Set-Cookie")
        if (cookies.isNotEmpty()) {
            val domain = URL(url).host
            val cookieList = cookieStore.computeIfAbsent(domain) { mutableListOf() }
            
            cookies.forEach { cookieHeader ->
                try {
                    val httpCookies = HttpCookie.parse(cookieHeader)
                    cookieList.addAll(httpCookies)
                } catch (e: Exception) {
                    logger.warn("解析响应 Cookie 失败: $cookieHeader")
                }
            }
        }
    }
    
    private fun String.toRequestBody(): RequestBody {
        return RequestBody.create(
            MediaType.parse("application/json; charset=utf-8") ?: MediaType.parse("text/plain"),
            this
        )
    }
    
    // ==================== SSL 信任所有（仅用于测试） ====================
    
    private class TrustAllSSLSocketFactory : javax.net.ssl.SSLSocketFactory() {
        private val delegate = (javax.net.ssl.SSLContext.getDefault()).socketFactory
        
        override fun createSocket(): java.net.Socket = delegate.createSocket()
        override fun createSocket(host: String, port: Int): java.net.Socket = delegate.createSocket(host, port)
        override fun createSocket(host: String, port: Int, localHost: java.net.InetAddress, localPort: Int): java.net.Socket = 
            delegate.createSocket(host, port, localHost, localPort)
        override fun createSocket(host: java.net.InetAddress, port: Int): java.net.Socket = delegate.createSocket(host, port)
        override fun createSocket(host: java.net.InetAddress, port: Int, localHost: java.net.InetAddress, localPort: Int): java.net.Socket = 
            delegate.createSocket(host, port, localHost, localPort)
        override fun createSocket(s: java.net.Socket, host: String, port: Int, autoClose: Boolean): java.net.Socket = 
            delegate.createSocket(s, host, port, autoClose)
        
        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
    }
    
    private class TrustAllHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean = true
    }
}
