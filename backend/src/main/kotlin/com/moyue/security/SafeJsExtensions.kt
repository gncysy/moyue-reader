package com.moyue.security

import okhttp3.*
import java.net.InetAddress
import java.net.URL
import java.nio.file.*
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory

class SafeJsExtensions(private val policy: SecurityPolicy) {
    private val logger = LoggerFactory.getLogger(SafeJsExtensions::class.java)
    private val sandboxRoot = Paths.get(policy.sandboxRoot).normalize()
    
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    init {
        Files.createDirectories(sandboxRoot)
    }
    
    fun ajax(url: String, method: String = "GET", headers: Map<String, String>? = null, body: String? = null): String {
        val count = requestCounts.computeIfAbsent("http") { AtomicInteger(0) }.incrementAndGet()
        if (count > 30) {
            throw SecurityException("请求过于频繁，请稍后再试")
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw SecurityException("禁止的协议: ${url.substringBefore("://")}")
        }
        
        if (policy.level != SecurityLevel.TRUSTED) {
            try {
                val host = URL(url).host
                val addr = InetAddress.getByName(host)
                if (addr.isLoopbackAddress || addr.isSiteLocalAddress) {
                    logger.warn("拦截内网请求: $host")
                    throw SecurityException("禁止访问内网地址: $host")
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        val requestBuilder = Request.Builder().url(url).method(method, body?.toRequestBody())
        headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        
        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body ?: return ""
        
        if (responseBody.contentLength() > policy.maxFileSize) {
            throw SecurityException("响应超过大小限制: ${responseBody.contentLength()}")
        }
        
        return responseBody.string()
    }
    
    fun getFile(path: String): ByteArray {
        if (!policy.allowFile) {
            throw SecurityException("当前安全模式不允许文件操作")
        }
        
        val target = sandboxRoot.resolve(path).normalize()
        if (!target.startsWith(sandboxRoot)) {
            logger.warn("路径越界尝试: $path")
            throw SecurityException("禁止访问沙箱外路径")
        }
        
        val ext = target.fileName.toString().substringAfterLast(".", "").lowercase()
        if (ext in listOf("exe", "dll", "so", "dylib", "sh", "bat", "cmd", "ps1")) {
            throw SecurityException("禁止读取可执行文件: $ext")
        }
        
        return Files.readAllBytes(target)
    }
    
    fun putFile(path: String, data: ByteArray) {
        if (!policy.allowFile) {
            throw SecurityException("当前安全模式不允许文件操作")
        }
        
        val target = sandboxRoot.resolve(path).normalize()
        if (!target.startsWith(sandboxRoot)) {
            throw SecurityException("禁止写入沙箱外路径")
        }
        
        if (data.size > policy.maxFileSize) {
            throw SecurityException("文件过大: ${data.size} > ${policy.maxFileSize}")
        }
        
        Files.createDirectories(target.parent)
        Files.write(target, data)
    }
    
    fun md5Encode(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun base64Encode(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray())
    fun base64Decode(text: String): String = String(Base64.getDecoder().decode(text))
    
    fun timeFormat(format: String, timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat(format)
        return sdf.format(Date(timestamp))
    }
    
    fun log(tag: String, message: String) {
        logger.info("[$tag] $message")
    }
    
    private fun String.toRequestBody(): RequestBody {
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), this)
    }
}
