package com.moyue.source
 
import com.moyue.engine.RhinoEngine
import com.moyue.security.SafeJsExtensions
import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
 
/**
 * JavaScript 扩展函数提供者
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 提供书源规则所需的 JavaScript 扩展函数
 * - 兼容开源阅读（Legado）的扩展函数
 * - 支持 Android Compat 接口
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Component
class JsExtensions(
    private val rhinoEngine: RhinoEngine,
    private val securityPolicy: SecurityPolicy = SecurityPolicy.forLevel(SecurityLevel.STANDARD)
) {
    
    private val logger = LoggerFactory.getLogger(JsExtensions::class.java)
    
    /**
     * 获取所有扩展函数
     */
    fun getExtensions(): Map<String, Any> {
        return mapOf(
            // 字符串处理
            "base64Encode" to ::base64Encode,
            "base64Decode" to ::base64Decode,
            "md5" to ::md5,
            "sha1" to ::sha1,
            "sha256" to ::sha256,
            "urlEncode" to ::urlEncode,
            "urlDecode" to ::urlDecode,
            
            // 加密
            "aesEncode" to ::aesEncode,
            "aesDecode" to ::aesDecode,
            "desEncode" to ::desEncode,
            "desDecode" to ::desDecode,
            "rsaEncode" to ::rsaEncode,
            "rsaDecode" to ::rsaDecode,
            
            // 正则
            "regexMatch" to ::regexMatch,
            "regexMatchAll" to ::regexMatchAll,
            "regexReplace" to ::regexReplace,
            "regexSplit" to ::regexSplit,
            
            // 日期
            "formatDate" to ::formatDate,
            "parseDate" to ::parseDate,
            "currentTime" to ::currentTime,
            
            // 字符串
            "substring" to ::substring,
            "substringBefore" to ::substringBefore,
            "substringAfter" to ::substringAfter,
            "substringBetween" to ::substringBetween,
            "trim" to ::trim,
            "replaceAll" to ::replaceAll,
            "replaceFirst" to ::replaceFirst,
            "split" to ::split,
            "join" to ::join,
            
            // 集合
            "map" to ::map,
            "filter" to ::filter,
            "reduce" to ::reduce,
            "find" to ::find,
            "contains" to ::contains,
            "sortBy" to ::sortBy,
            "reverse" to ::reverse,
            "distinct" to ::distinct,
            
            // JSON
            "jsonParse" to ::jsonParse,
            "jsonStringify" to ::jsonStringify,
            
            // HTML 解析
            "parseHtml" to ::parseHtml,
            "selectElements" to ::selectElements,
            "getElementText" to ::getElementText,
            "getElementAttr" to ::getElementAttr,
            "getHtml" to ::getHtml,
            "getAttr" to ::getAttr,
            "getText" to ::getText,
            "getId" to ::getId,
            "getClass" to ::getClass,
            "hasClass" to ::hasClass,
            "removeAttr" to ::removeAttr,
            "setAttr" to ::setAttr,
            "html" to ::html,
            "text" to ::text,
            "attr" to ::attr,
            
            // 日志
            "log" to ::log,
            "error" to ::error,
            "warn" to ::warn,
            "debug" to ::debug,
            "info" to ::info
        )
    }
    
    // ==================== 字符串处理 ====================
    
    fun base64Encode(input: String): String {
        return String(java.util.Base64.getEncoder().encode(input.toByteArray()))
    }
    
    fun base64Decode(input: String): String {
        return String(java.util.Base64.getDecoder().decode(input))
    }
    
    fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun sha1(input: String): String {
        val sha = java.security.MessageDigest.getInstance("SHA-1")
        val bytes = sha.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun sha256(input: String): String {
        val sha = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = sha.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun urlEncode(input: String): String {
        return java.net.URLEncoder.encode(input, "UTF-8")
    }
    
    fun urlDecode(input: String): String {
        return java.net.URLDecoder.decode(input, "UTF-8")
    }
    
    // ==================== 加密 ====================
    
    fun aesEncode(data: String, key: String, iv: String? = null): String {
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "AES")
        val ivSpec = if (iv != null) {
            javax.crypto.spec.IvParameterSpec(iv.toByteArray())
        } else {
            javax.crypto.spec.IvParameterSpec(ByteArray(16))
        }
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(data.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(encrypted)
    }
    
    fun aesDecode(data: String, key: String, iv: String? = null): String {
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "AES")
        val ivSpec = if (iv != null) {
            javax.crypto.spec.IvParameterSpec(iv.toByteArray())
        } else {
            javax.crypto.spec.IvParameterSpec(ByteArray(16))
        }
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decoded = java.util.Base64.getDecoder().decode(data)
        return String(cipher.doFinal(decoded))
    }
    
    fun desEncode(data: String, key: String): String {
        val cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding")
        val keySpec = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "DES")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(data.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(encrypted)
    }
    
    fun desDecode(data: String, key: String): String {
        val cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding")
        val keySpec = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "DES")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec)
        val decoded = java.util.Base64.getDecoder().decode(data)
        return String(cipher.doFinal(decoded))
    }
    
    fun rsaEncode(data: String, key: String): String {
        val publicKey = java.security.KeyFactory.getInstance("RSA")
            .generatePublic(
                java.security.spec.X509EncodedKeySpec(
                    java.util.Base64.getDecoder().decode(key)
                )
            )
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(data.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(encrypted)
    }
    
    fun rsaDecode(data: String, key: String): String {
        val privateKey = java.security.KeyFactory.getInstance("RSA")
            .generatePrivate(
                java.security.spec.PKCS8EncodedKeySpec(
                    java.util.Base64.getDecoder().decode(key)
                )
            )
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
        val decoded = java.util.Base64.getDecoder().decode(data)
        return String(cipher.doFinal(decoded))
    }
    
    // ==================== 正则 ====================
    
    fun regexMatch(regex: String, input: String): Boolean {
        return Regex(regex).containsMatchIn(input)
    }
    
    fun regexMatchAll(regex: String, input: String): List<String> {
        return Regex(regex).findAll(input).map { it.value }.toList()
    }
    
    fun regexReplace(regex: String, replacement: String, input: String): String {
        return input.replace(Regex(regex), replacement)
    }
    
    fun regexSplit(regex: String, input: String): List<String> {
        return input.split(Regex(regex))
    }
    
    // ==================== 日期 ====================
    
    fun formatDate(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return java.text.SimpleDateFormat(pattern).format(java.util.Date(timestamp))
    }
    
    fun formatDate(timestamp: Number, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return formatDate(timestamp.toLong(), pattern)
    }
    
    fun formatDate(timestamp: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return try {
            formatDate(timestamp.toLong(), pattern)
        } catch (e: Exception) {
            ""
        }
    }
    
    fun parseDate(dateStr: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): Long {
        return java.text.SimpleDateFormat(pattern).parse(dateStr).time
    }
    
    fun currentTime(): Long {
        return System.currentTimeMillis()
    }
    
    // ==================== 字符串 ====================
    
    fun substring(str: String, start: Int, end: Int = str.length): String {
        return str.substring(start, end.coerceAtMost(str.length))
    }
    
    fun substringBefore(str: String, separator: String): String {
        return str.substringBefore(separator)
    }
    
    fun substringAfter(str: String, separator: String): String {
        return str.substringAfter(separator)
    }
    
    fun substringBetween(str: String, start: String, end: String): String {
        return str.substringAfter(start).substringBeforeLast(end)
    }
    
    fun trim(str: String): String {
        return str.trim()
    }
    
    fun replaceAll(str: String, target: String, replacement: String): String {
        return str.replace(target, replacement)
    }
    
    fun replaceFirst(str: String, target: String, replacement: String): String {
        return str.replaceFirst(target, replacement)
    }
    
    fun split(str: String, delimiter: String): List<String> {
        return str.split(delimiter)
    }
    
    fun join(list: List<String>, delimiter: String): String {
        return list.joinToString(delimiter)
    }
    
    // ==================== 集合 ====================
    
    fun <T, R> map(list: List<T>, func: (T) -> R): List<R> {
        return list.map(func)
    }
    
    fun <T> filter(list: List<T>, func: (T) -> Boolean): List<T> {
        return list.filter(func)
    }
    
    fun <T, R> reduce(list: List<T>, func: (R?, T) -> R, initial: R? = null): R? {
        var acc = initial
        for (item in list) {
            acc = func(acc, item)
        }
        return acc
    }
    
    fun <T> find(list: List<T>, func: (T) -> Boolean): T? {
        return list.find(func)
    }
    
    fun <T> contains(list: List<T>, item: T): Boolean {
        return list.contains(item)
    }
    
    fun <T> sortBy(list: List<T>, func: (T) -> Comparable<*>): List<T> {
        return list.sortedBy(func)
    }
    
    fun <T> reverse(list: List<T>): List<T> {
        return list.reversed()
    }
    
    fun <T> distinct(list: List<T>): List<T> {
        return list.distinct()
    }
    
    // ==================== JSON ====================
    
    fun jsonParse(json: String): Any {
        return com.google.gson.Gson().fromJson(json, Any::class.java)
    }
    
    fun jsonStringify(obj: Any): String {
        return com.google.gson.Gson().toJson(obj)
    }
    
    // ==================== HTML 解析 ====================
    
    fun parseHtml(html: String): Document {
        return Jsoup.parse(html)
    }
    
    fun selectElements(html: String, selector: String): Elements {
        return Jsoup.parse(html).select(selector)
    }
    
    fun getElementText(html: String, selector: String): String {
        return Jsoup.parse(html).select(selector).firstOrNull()?.text() ?: ""
    }
    
    fun getElementAttr(html: String, selector: String, attr: String): String {
        return Jsoup.parse(html).select(selector).firstOrNull()?.attr(attr) ?: ""
    }
    
    // ==================== 元素操作 ====================
    
    fun getHtml(element: Element): String {
        return element.html()
    }
    
    fun getText(element: Element): String {
        return element.text()
    }
    
    fun getAttr(element: Element, attr: String): String {
        return element.attr(attr)
    }
    
    fun getId(element: Element): String {
        return element.id()
    }
    
    fun getClass(element: Element): String {
        return element.className()
    }
    
    fun hasClass(element: Element, className: String): Boolean {
        return element.hasClass(className)
    }
    
    fun removeAttr(element: Element, attr: String): Element {
        return element.removeAttr(attr)
    }
    
    fun setAttr(element: Element, attr: String, value: String): Element {
        return element.attr(attr, value)
    }
    
    // ==================== 简化版 HTML 解析 ====================
    
    fun html(html: String): Document {
        return Jsoup.parse(html)
    }
    
    fun text(html: String): String {
        return Jsoup.parse(html).text()
    }
    
    fun attr(html: String, selector: String, attr: String): String {
        return Jsoup.parse(html).select(selector).firstOrNull()?.attr(attr) ?: ""
    }
    
    // ==================== 日志 ====================
    
    fun log(vararg messages: Any) {
        logger.info(messages.joinToString(" "))
    }
    
    fun error(vararg messages: Any) {
        logger.error(messages.joinToString(" "))
    }
    
    fun warn(vararg messages: Any) {
        logger.warn(messages.joinToString(" "))
    }
    
    fun debug(vararg messages: Any) {
        logger.debug(messages.joinToString(" "))
    }
    
    fun info(vararg messages: Any) {
        logger.info(messages.joinToString(" "))
    }
}
