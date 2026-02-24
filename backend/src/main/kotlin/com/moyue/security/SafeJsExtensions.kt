package com.moyue.security
 
import org.slf4j.LoggerFactory
import org.mozilla.javascript.Context
import org.mozilla.javascript.FunctionObject
import org.mozilla.javascript.Scriptable
import java.net.URL
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
 
/**
 * 安全的 JavaScript 扩展
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 提供书源规则所需的 JavaScript 扩展函数
 * - 实现安全沙箱，防止恶意代码执行
 * - 支持常用工具函数：Base64、AES、MD5、正则等
 *
 * @author Moyue Team
 * @since 4.0.3
 */
class SafeJsExtensions(
    private val securityPolicy: SecurityPolicy = SecurityPolicy.forLevel(SecurityLevel.STANDARD)
) {
    
    private val logger = LoggerFactory.getLogger(SafeJsExtensions::class.java)
    
    /**
     * 注册安全扩展函数到 JavaScript 作用域
     */
    fun registerExtensions(scope: Scriptable) {
        logger.debug("注册安全 JavaScript 扩展函数")
        
        // 字符串处理
        registerFunction(scope, "base64Encode", ::base64Encode)
        registerFunction(scope, "base64Decode", ::base64Decode)
        registerFunction(scope, "md5", ::md5)
        registerFunction(scope, "sha1", ::sha1)
        registerFunction(scope, "sha256", ::sha256)
        registerFunction(scope, "urlEncode", ::urlEncode)
        registerFunction(scope, "urlDecode", ::urlDecode)
        
        // 加密
        registerFunction(scope, "aesEncode", ::aesEncode)
        registerFunction(scope, "aesDecode", ::aesDecode)
        registerFunction(scope, "desEncode", ::desEncode)
        registerFunction(scope, "desDecode", ::desDecode)
        registerFunction(scope, "rsaEncode", ::rsaEncode)
        registerFunction(scope, "rsaDecode", ::rsaDecode)
        
        // 正则
        registerFunction(scope, "regexMatch", ::regexMatch)
        registerFunction(scope, "regexMatchAll", ::regexMatchAll)
        registerFunction(scope, "regexReplace", ::regexReplace)
        registerFunction(scope, "regexSplit", ::regexSplit)
        
        // 日期
        registerFunction(scope, "formatDate", ::formatDate)
        registerFunction(scope, "parseDate", ::parseDate)
        registerFunction(scope, "currentTime", ::currentTime)
        
        // 字符串
        registerFunction(scope, "substring", ::substring)
        registerFunction(scope, "substringBefore", ::substringBefore)
        registerFunction(scope, "substringAfter", ::substringAfter)
        registerFunction(scope, "substringBetween", ::substringBetween)
        registerFunction(scope, "trim", ::trim)
        registerFunction(scope, "replaceAll", ::replaceAll)
        registerFunction(scope, "replaceFirst", ::replaceFirst)
        registerFunction(scope, "split", ::split)
        registerFunction(scope, "join", ::join)
        
        // 集合
        registerFunction(scope, "map", ::map)
        registerFunction(scope, "filter", ::filter)
        registerFunction(scope, "reduce", ::reduce)
        registerFunction(scope, "find", ::find)
        registerFunction(scope, "contains", ::contains)
        registerFunction(scope, "sortBy", ::sortBy)
        registerFunction(scope, "reverse", ::reverse)
        registerFunction(scope, "distinct", ::distinct)
        
        // JSON
        registerFunction(scope, "jsonParse", ::jsonParse)
        registerFunction(scope, "jsonStringify", ::jsonStringify)
        
        // 网络（受限制）
        if (securityPolicy.allowsNetwork) {
            registerFunction(scope, "httpGet", ::httpGet)
            registerFunction(scope, "httpPost", ::httpPost)
        }
        
        // 日志
        registerFunction(scope, "log", ::log)
        registerFunction(scope, "error", ::error)
        registerFunction(scope, "warn", ::warn)
    }
    
    // ==================== 辅助方法 ====================
    
    private fun registerFunction(scope: Scriptable, name: String, func: (Array<Any>, Scriptable) -> Any?) {
        try {
            FunctionObject(name, func, scope).apply {
                scope.put(this, scope, this)
            }
        } catch (e: Exception) {
            logger.error("注册函数失败: $name", e)
        }
    }
    
    // ==================== 字符串处理 ====================
    
    private fun base64Encode(args: Array<Any>, scope: Scriptable): String? {
        return args.getOrNull(0)?.toString()?.let {
            String(Base64.getEncoder().encode(it.toByteArray()))
        }
    }
    
    private fun base64Decode(args: Array<Any>, scope: Scriptable): String? {
        return args.getOrNull(0)?.toString()?.let {
            String(Base64.getDecoder().decode(it))
        }
    }
    
    private fun md5(args: Array<Any>, scope: Scriptable): String? {
        val input = args.getOrNull(0)?.toString() ?: return null
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val bytes = md.digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("MD5 加密失败", e)
            null
        }
    }
    
    private fun sha1(args: Array<Any>, scope: Scriptable): String? {
        val input = args.getOrNull(0)?.toString() ?: return null
        return try {
            val sha = java.security.MessageDigest.getInstance("SHA-1")
            val bytes = sha.digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("SHA-1 加密失败", e)
            null
        }
    }
    
    private fun sha256(args: Array<Any>, scope: Scriptable): String? {
        val input = args.getOrNull(0)?.toString() ?: return null
        return try {
            val sha = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = sha.digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("SHA-256 加密失败", e)
            null
        }
    }
    
    private fun urlEncode(args: Array<Any>, scope: Scriptable): String? {
        val input = args.getOrNull(0)?.toString() ?: return null
        return try {
            URLEncoder.encode(input, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            logger.error("URL 编码失败", e)
            null
        }
    }
    
    private fun urlDecode(args: Array<Any>, scope: Scriptable): String? {
        val input = args.getOrNull(0)?.toString() ?: return null
        return try {
            URLDecoder.decode(input, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            logger.error("URL 解码失败", e)
            null
        }
    }
    
    // ==================== 加密 ====================
    
    private fun aesEncode(args: Array<Any>, scope: Scriptable): String? {
        val data = args.getOrNull(0)?.toString() ?: return null
        val key = args.getOrNull(1)?.toString() ?: return null
        val iv = args.getOrNull(2)?.toString()
        
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(key.toByteArray(), "AES")
            val ivSpec = if (iv != null) {
                IvParameterSpec(iv.toByteArray())
            } else {
                IvParameterSpec(ByteArray(16))
            }
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logger.error("AES 加密失败", e)
            null
        }
    }
    
    private fun aesDecode(args: Array<Any>, scope: Scriptable): String? {
        val data = args.getOrNull(0)?.toString() ?: return null
        val key = args.getOrNull(1)?.toString() ?: return null
        val iv = args.getOrNull(2)?.toString()
        
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(key.toByteArray(), "AES")
            val ivSpec = if (iv != null) {
                IvParameterSpec(iv.toByteArray())
            } else {
                IvParameterSpec(ByteArray(16))
            }
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decoded = Base64.getDecoder().decode(data)
            String(cipher.doFinal(decoded))
        } catch (e: Exception) {
            logger.error("AES 解密失败", e)
            null
        }
    }
    
    private fun desEncode(args: Array<Any>, scope: Scriptable): String? {
        val data = args.getOrNull(0)?.toString() ?: return null
        val key = args.getOrNull(1)?.toString() ?: return null
        
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val keySpec = SecretKeySpec(key.toByteArray(), "DES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logger.error("DES 加密失败", e)
            null
        }
    }
    
    private fun desDecode(args: Array<Any>, scope: Scriptable): String? {
        val data = args.getOrNull(0)?.toString() ?: return null
        val key = args.getOrNull(1)?.toString() ?: return null
        
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val keySpec = SecretKeySpec(key.toByteArray(), "DES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decoded = Base64.getDecoder().decode(data)
            String(cipher.doFinal(decoded))
        } catch (e: Exception) {
            logger.error("DES 解密失败", e)
            null
        }
    }
    
    private fun rsaEncode(args: Array<Any>, scope: Scriptable): String? {
        val data = args.getOrNull(0)?.toString() ?: return null
        val key = args.getOrNull(1)?.toString() ?: return null
        
        // 简化实现：实际应使用完整的 RSA 加密
        return try {
            val publicKey = java.security.KeyFactory.getInstance("RSA")
                .generatePublic(
                    java.security.spec.X509EncodedKeySpec(
                        Base64.getDecoder().decode(key)
                    )
                )
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logger.error("RSA 加密失败", e)
            null
        }
    }
    
    private fun rsaDecode(args: Array<Any>, scope: Scriptable): String? {
        val data = args.getOrNull(0)?.toString() ?: return null
        val key = args.getOrNull(1)?.toString() ?: return null
        
        // 简化实现：实际应使用完整的 RSA 解密
        return try {
            val privateKey = java.security.KeyFactory.getInstance("RSA")
                .generatePrivate(
                    java.security.spec.PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode(key)
                    )
                )
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val decoded = Base64.getDecoder().decode(data)
            String(cipher.doFinal(decoded))
        } catch (e: Exception) {
            logger.error("RSA 解密失败", e)
            null
        }
    }
    
    // ==================== 正则 ====================
    
    private fun regexMatch(args: Array<Any>, scope: Scriptable): Boolean {
        val regex = args.getOrNull(0)?.toString() ?: return false
        val input = args.getOrNull(1)?.toString() ?: return false
        return try {
            Pattern.compile(regex).matcher(input).find()
        } catch (e: Exception) {
            logger.error("正则匹配失败", e)
            false
        }
    }
    
    private fun regexMatchAll(args: Array<Any>, scope: Scriptable): List<String> {
        val regex = args.getOrNull(0)?.toString() ?: return emptyList()
        val input = args.getOrNull(1)?.toString() ?: return emptyList()
        return try {
            val matcher = Pattern.compile(regex).matcher(input)
            val results = mutableListOf<String>()
            while (matcher.find()) {
                results.add(matcher.group())
            }
            results
        } catch (e: Exception) {
            logger.error("正则匹配失败", e)
            emptyList()
        }
    }
    
    private fun regexReplace(args: Array<Any>, scope: Scriptable): String? {
        val regex = args.getOrNull(0)?.toString() ?: return null
        val replacement = args.getOrNull(1)?.toString() ?: return null
        val input = args.getOrNull(2)?.toString() ?: return null
        return try {
            Pattern.compile(regex).matcher(input).replaceAll(replacement)
        } catch (e: Exception) {
            logger.error("正则替换失败", e)
            null
        }
    }
    
    private fun regexSplit(args: Array<Any>, scope: Scriptable): List<String> {
        val regex = args.getOrNull(0)?.toString() ?: return emptyList()
        val input = args.getOrNull(1)?.toString() ?: return emptyList()
        return try {
            input.split(Regex(regex))
        } catch (e: Exception) {
            logger.error("正则分割失败", e)
            emptyList()
        }
    }
    
    // ==================== 日期 ====================
    
    private fun formatDate(args: Array<Any>, scope: Scriptable): String? {
        val timestamp = args.getOrNull(0)
        val pattern = args.getOrNull(1)?.toString() ?: "yyyy-MM-dd HH:mm:ss"
        
        return try {
            val date = when (timestamp) {
                is Number -> Date(timestamp.toLong())
                is String -> SimpleDateFormat(pattern).parse(timestamp)
                else -> Date()
            }
            SimpleDateFormat(pattern).format(date)
        } catch (e: Exception) {
            logger.error("日期格式化失败", e)
            null
        }
    }
    
    private fun parseDate(args: Array<Any>, scope: Scriptable): Long? {
        val dateStr = args.getOrNull(0)?.toString() ?: return null
        val pattern = args.getOrNull(1)?.toString() ?: "yyyy-MM-dd HH:mm:ss"
        
        return try {
            SimpleDateFormat(pattern).parse(dateStr).time
        } catch (e: Exception) {
            logger.error("日期解析失败", e)
            null
        }
    }
    
    private fun currentTime(args: Array<Any>, scope: Scriptable): Long {
        return System.currentTimeMillis()
    }
    
    // ==================== 字符串 ====================
    
    private fun substring(args: Array<Any>, scope: Scriptable): String? {
        val str = args.getOrNull(0)?.toString() ?: return null
        val start = (args.getOrNull(1) as? Number)?.toInt() ?: 0
        val end = (args.getOrNull(2) as? Number)?.toInt() ?: str.length
        
        return try {
            str.substring(start, end.coerceAtMost(str.length))
        } catch (e: Exception) {
            logger.error("字符串截取失败", e)
            null
        }
    }
    
    private fun substringBefore(args: Array<Any>, scope: Scriptable): String? {
        val str = args.getOrNull(0)?.toString() ?: return null
        val separator = args.getOrNull(1)?.toString() ?: return null
        
        return try {
            str.substringBefore(separator)
        } catch (e: Exception) {
            logger.error("字符串截取失败", e)
            null
        }
    }
    
    private fun substringAfter(args: Array<Any>, scope: Scriptable): String? {
        val str = args.getOrNull(0)?.toString() ?: return null
        val separator = args.getOrNull(1)?.toString() ?: return null
        
        return try {
            str.substringAfter(separator)
        } catch (e: Exception) {
            logger.error("字符串截取失败", e)
            null
        }
    }
    
    private fun substringBetween(args: Array<Any>, scope: Scriptable): String? {
        val str = args.getOrNull(0)?.toString() ?: return null
        val start = args.getOrNull(1)?.toString() ?: return null
        val end = args.getOrNull(2)?.toString() ?: return null
        
        return try {
            str.substringBetween(start, end)
        } catch (e: Exception) {
            logger.error("字符串截取失败", e)
            null
        }
    }
    
    private fun trim(args: Array<Any>, scope: Scriptable): String? {
        return args.getOrNull(0)?.toString()?.trim()
    }
    
    private fun replaceAll(args: Array<Any>, scope: Scriptable): String? {
        val str = args.getOrNull(0)?.toString() ?: return null
        val target = args.getOrNull(1)?.toString() ?: return null
        val replacement = args.getOrNull(2)?.toString() ?: return null
        
        return try {
            str.replace(target, replacement)
        } catch (e: Exception) {
            logger.error("字符串替换失败", e)
            null
        }
    }
    
    private fun replaceFirst(args: Array<Any>, scope: Scriptable): String? {
        val str = args.getOrNull(0)?.toString() ?: return null
        val target = args.getOrNull(1)?.toString() ?: return null
        val replacement = args.getOrNull(2)?.toString() ?: return null
        
        return try {
            str.replaceFirst(target, replacement)
        } catch (e: Exception) {
            logger.error("字符串替换失败", e)
            null
        }
    }
    
    private fun split(args: Array<Any>, scope: Scriptable): List<String> {
        val str = args.getOrNull(0)?.toString() ?: return emptyList()
        val delimiter = args.getOrNull(1)?.toString() ?: return emptyList()
        
        return try {
            str.split(delimiter)
        } catch (e: Exception) {
            logger.error("字符串分割失败", e)
            emptyList()
        }
    }
    
    private fun join(args: Array<Any>, scope: Scriptable): String? {
        val list = args.getOrNull(0) as? List<*> ?: return null
        val delimiter = args.getOrNull(1)?.toString() ?: ""
        
        return try {
            @Suppress("UNCHECKED_CAST")
            (list as List<String>).joinToString(delimiter)
        } catch (e: Exception) {
            logger.error("字符串连接失败", e)
            null
        }
    }
    
    // ==================== 集合 ====================
    
    private fun map(args: Array<Any>, scope: Scriptable): List<Any> {
        val list = args.getOrNull(0) as? List<*> ?: return emptyList()
        val func = args.getOrNull(1) as? Function ?: return emptyList()
        
        return try {
            list.map { item ->
                val jsArray = Context.getCurrentContext().newArray(scope, arrayOf(item))
                func.call(Context.getCurrentContext(), scope, scope, arrayOf(jsArray))
            }
        } catch (e: Exception) {
            logger.error("map 操作失败", e)
            emptyList()
        }
    }
    
    private fun filter(args: Array<Any>, scope: Scriptable): List<Any> {
        val list = args.getOrNull(0) as? List<*> ?: return emptyList()
        val func = args.getOrNull(1) as? Function ?: return emptyList()
        
        return try {
            list.filter { item ->
                val jsArray = Context.getCurrentContext().newArray(scope, arrayOf(item))
                val result = func.call(Context.getCurrentContext(), scope, scope, arrayOf(jsArray))
                result == true
            }
        } catch (e: Exception) {
            logger.error("filter 操作失败", e)
            emptyList()
        }
    }
    
    private fun reduce(args: Array<Any>, scope: Scriptable): Any? {
        val list = args.getOrNull(0) as? List<*> ?: return null
        val func = args.getOrNull(1) as? Function ?: return null
        val initial = args.getOrNull(2)
        
        return try {
            var acc = initial
            for (item in list) {
                val jsArray = Context.getCurrentContext().newArray(scope, arrayOf(acc, item))
                acc = func.call(Context.getCurrentContext(), scope, scope, arrayOf(jsArray))
            }
            acc
        } catch (e: Exception) {
            logger.error("reduce 操作失败", e)
            null
        }
    }
    
    private fun find(args: Array<Any>, scope: Scriptable): Any? {
        val list = args.getOrNull(0) as? List<*> ?: return null
        val func = args.getOrNull(1) as? Function ?: return null
        
        return try {
            list.firstOrNull { item ->
                val jsArray = Context.getCurrentContext().newArray(scope, arrayOf(item))
                val result = func.call(Context.getCurrentContext(), scope, scope, arrayOf(jsArray))
                result == true
            }
        } catch (e: Exception) {
            logger.error("find 操作失败", e)
            null
        }
    }
    
    private fun contains(args: Array<Any>, scope: Scriptable): Boolean {
        val list = args.getOrNull(0) as? List<*> ?: return false
        val item = args.getOrNull(1) ?: return false
        
        return try {
            list.contains(item)
        } catch (e: Exception) {
            logger.error("contains 操作失败", e)
            false
        }
    }
    
    private fun sortBy(args: Array<Any>, scope: Scriptable): List<Any> {
        val list = args.getOrNull(0) as? List<*> ?: return emptyList()
        val func = args.getOrNull(1) as? Function
        
        return try {
            if (func != null) {
                @Suppress("UNCHECKED_CAST")
                (list as List<Any>).sortedWith { a, b ->
                    val jsArray = Context.getCurrentContext().newArray(scope, arrayOf(a, b))
                    val result = func.call(Context.getCurrentContext(), scope, scope, arrayOf(jsArray))
                    (result as? Number)?.toInt() ?: 0
                }
            } else {
                list.sorted()
            }
        } catch (e: Exception) {
            logger.error("sortBy 操作失败", e)
            emptyList()
        }
    }
    
    private fun reverse(args: Array<Any>, scope: Scriptable): List<Any> {
        val list = args.getOrNull(0) as? List<*> ?: return emptyList()
        
        return try {
            list.reversed()
        } catch (e: Exception) {
            logger.error("reverse 操作失败", e)
            emptyList()
        }
    }
    
    private fun distinct(args: Array<Any>, scope: Scriptable): List<Any> {
        val list = args.getOrNull(0) as? List<*> ?: return emptyList()
        
        return try {
            list.distinct()
        } catch (e: Exception) {
            logger.error("distinct 操作失败", e)
            emptyList()
        }
    }
    
    // ==================== JSON ====================
    
    private fun jsonParse(args: Array<Any>, scope: Scriptable): Any? {
        val json = args.getOrNull(0)?.toString() ?: return null
        
        return try {
            com.google.gson.Gson().fromJson(json, Any::class.java)
        } catch (e: Exception) {
            logger.error("JSON 解析失败", e)
            null
        }
    }
    
    private fun jsonStringify(args: Array<Any>, scope: Scriptable): String? {
        val obj = args.getOrNull(0) ?: return null
        
        return try {
            com.google.gson.Gson().toJson(obj)
        } catch (e: Exception) {
            logger.error("JSON 序列化失败", e)
            null
        }
    }
    
    // ==================== 网络（受限制） ====================
    
    private fun httpGet(args: Array<Any>, scope: Scriptable): String? {
        val url = args.getOrNull(0)?.toString() ?: return null
        
        // 检查安全策略
        if (!securityPolicy.allows("network", url)) {
            logger.warn("HTTP GET 被拒绝: $url")
            return null
        }
        
        return try {
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                logger.warn("HTTP GET 失败: $url, code=$responseCode")
                null
            }
        } catch (e: Exception) {
            logger.error("HTTP GET 失败: $url", e)
            null
        }
    }
    
    private fun httpPost(args: Array<Any>, scope: Scriptable): String? {
        val url = args.getOrNull(0)?.toString() ?: return null
        val data = args.getOrNull(1)?.toString() ?: return null
        
        // 检查安全策略
        if (!securityPolicy.allows("network", url)) {
            logger.warn("HTTP POST 被拒绝: $url")
            return null
        }
        
        return try {
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            connection.outputStream.use { it.write(data.toByteArray()) }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                logger.warn("HTTP POST 失败: $url, code=$responseCode")
                null
            }
        } catch (e: Exception) {
            logger.error("HTTP POST 失败: $url", e)
            null
        }
    }
    
    // ==================== 日志 ====================
    
    private fun log(args: Array<Any>, scope: Scriptable) {
        logger.info(args.joinToString(" "))
    }
    
    private fun error(args: Array<Any>, scope: Scriptable) {
        logger.error(args.joinToString(" "))
    }
    
    private fun warn(args: Array<Any>, scope: Scriptable) {
        logger.warn(args.joinToString(" "))
    }
}
