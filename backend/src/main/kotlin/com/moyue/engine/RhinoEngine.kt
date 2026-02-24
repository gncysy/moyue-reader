package com.moyue.engine
 
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.model.BookSourceRules
import com.moyue.security.SafeJsExtensions
import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
 
/**
 * Rhino JavaScript 引擎
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 执行书源规则的 JavaScript 代码
 * - 安全沙箱环境
 * - 支持常用扩展函数
 * - 限制执行时间和资源使用
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Component
class RhinoEngine(
    private val okHttpClient: OkHttpClient,
    @Value("\${moyue.security.default-level:standard}")
    private var defaultSecurityLevel: String
) {
    
    private val logger = LoggerFactory.getLogger(RhinoEngine::class.java)
    private val securityLevel = SecurityLevel.fromName(defaultSecurityLevel)
    private val securityPolicy = SecurityPolicy.forLevel(securityLevel)
    private val safeJsExtensions = SafeJsExtensions(securityPolicy)
    
    // 初始化 JavaScript 上下文
    private val context = Context.enter()
    private val scope: Scriptable = context.initStandardObjects()
    
    init {
        logger.info("初始化 Rhino 引擎，安全等级: $securityLevel")
        
        // 注册安全扩展函数
        safeJsExtensions.registerExtensions(scope)
        
        // 配置安全限制
        context.optimizationLevel = -1  // 解释模式，提高安全性
        context.securityController = CustomSecurityController(securityPolicy)
        
        // 设置执行时间限制
        context.observeInstructionCount = 1000
        context.instructionCountThreshold = securityPolicy.maxExecutionTime * 1000
    }
    
    /**
     * 执行 JavaScript 代码
     */
    fun executeCode(code: String, contextVars: Map<String, Any> = emptyMap()): Any? {
        return try {
            // 设置上下文变量
            contextVars.forEach { (key, value) ->
                scope.put(key, scope, value)
            }
            
            // 执行代码
            context.evaluateString(scope, code, "source", 1, null)
        } catch (e: Exception) {
            logger.error("执行 JavaScript 代码失败", e)
            throw RuntimeException("执行失败: ${e.message}", e)
        }
    }
    
    /**
     * 验证 JavaScript 代码语法
     */
    fun validateCode(code: String): Pair<Boolean, String?> {
        return try {
            context.compileString(code, "source", 1, null)
            true to null
        } catch (e: Exception) {
            false to e.message
        }
    }
    
    /**
     * 检查 URL 是否可访问
     */
    fun checkUrl(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response: Response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            logger.error("检查 URL 失败: $url", e)
            false
        }
    }
    
    /**
     * 执行搜索规则
     */
    fun executeSearchRule(
        source: BookSource,
        rules: BookSourceRules,
        keyword: String
    ): List<Map<String, Any>> {
        return try {
            logger.debug("执行搜索规则: ${source.name}, keyword=$keyword")
            
            // 构建搜索 URL
            val searchUrl = buildUrl(rules.searchUrl, mapOf("key" to keyword))
            
            // 获取搜索结果页面
            val html = fetchHtml(searchUrl, source)
            
            // 执行搜索规则
            val result = executeCode(rules.searchList ?: "", mapOf(
                "baseUrl" to searchUrl,
                "html" to html,
                "keyword" to keyword
            )) as? List<Map<String, Any>> ?: emptyList()
            
            // 处理每个搜索结果
            result.map { item ->
                mapOf(
                    "name" to (item["name"]?.toString() ?: ""),
                    "author" to (item["author"]?.toString() ?: ""),
                    "coverUrl" to (item["coverUrl"]?.toString() ?: ""),
                    "bookUrl" to (item["bookUrl"]?.toString() ?: ""),
                    "sourceId" to source.sourceId,
                    "sourceName" to source.name
                )
            }
        } catch (e: Exception) {
            logger.error("执行搜索规则失败: ${source.name}", e)
            emptyList()
        }
    }
    
    /**
     * 执行书籍信息规则
     */
    fun executeBookInfoRule(
        source: BookSource,
        rules: BookSourceRules,
        bookUrl: String
    ): Map<String, Any> {
        return try {
            logger.debug("执行书籍信息规则: ${source.name}, bookUrl=$bookUrl")
            
            // 获取书籍页面
            val html = fetchHtml(bookUrl, source)
            
            // 执行书籍信息规则
            val result = executeCode(rules.bookInfo ?: "", mapOf(
                "baseUrl" to bookUrl,
                "html" to html
            )) as? Map<String, Any> ?: emptyMap()
            
            mapOf(
                "name" to (result["name"]?.toString() ?: ""),
                "author" to (result["author"]?.toString() ?: ""),
                "coverUrl" to (result["coverUrl"]?.toString() ?: ""),
                "intro" to (result["intro"]?.toString() ?: ""),
                "chapterCount" to (result["chapterCount"] as? Int ?: 0)
            )
        } catch (e: Exception) {
            logger.error("执行书籍信息规则失败: ${source.name}", e)
            emptyMap()
        }
    }
    
    /**
     * 执行章节列表规则
     */
    fun executeChapterListRule(
        source: BookSource,
        rules: BookSourceRules,
        bookUrl: String
    ): List<BookChapter> {
        return try {
            logger.debug("执行章节列表规则: ${source.name}, bookUrl=$bookUrl")
            
            // 获取书籍页面
            val html = fetchHtml(bookUrl, source)
            
            // 执行章节列表规则
            val result = executeCode(rules.chapterList ?: "", mapOf(
                "baseUrl" to bookUrl,
                "html" to html
            )) as? List<Map<String, Any>> ?: emptyList()
            
            // 处理每个章节
            result.mapIndexed { index, item ->
                BookChapter(
                    bookId = "",  // 后续设置
                    index = index,
                    title = (item["name"]?.toString() ?: "未知章节"),
                    url = (item["url"]?.toString() ?: ""),
                    isVip = (item["isVip"] as? Boolean) ?: false
                )
            }
        } catch (e: Exception) {
            logger.error("执行章节列表规则失败: ${source.name}", e)
            emptyList()
        }
    }
    
    /**
     * 执行内容规则
     */
    fun executeContentRule(
        source: BookSource,
        rules: BookSourceRules,
        chapterUrl: String,
        bookUrl: String
    ): String {
        return try {
            logger.debug("执行内容规则: ${source.name}, chapterUrl=$chapterUrl")
            
            // 获取章节页面
            val html = fetchHtml(chapterUrl, source)
            
            // 执行内容规则
            val result = executeCode(rules.content ?: "", mapOf(
                "baseUrl" to chapterUrl,
                "bookUrl" to bookUrl,
                "html" to html
            ))
            
            (result?.toString() ?: "").trim()
        } catch (e: Exception) {
            logger.error("执行内容规则失败: ${source.name}", e)
            ""
        }
    }
    
    /**
     * 获取 HTML 页面
     */
    private fun fetchHtml(url: String, source: BookSource): String {
        // 检查安全策略
        if (!securityPolicy.allows("network", url)) {
            throw SecurityException("网络访问被拒绝: $url")
        }
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response: Response = okHttpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw RuntimeException("HTTP 请求失败: ${response.code}")
        }
        
        return response.body?.string() ?: ""
    }
    
    /**
     * 构建 URL
     */
    private fun buildUrl(template: String?, params: Map<String, Any>): String {
        if (template == null) {
            throw IllegalArgumentException("URL 模板为空")
        }
        
        var url = template
        params.forEach { (key, value) ->
            url = url.replace("{$key}", value.toString())
        }
        
        return url
    }
    
    /**
     * 解析 HTML
     */
    fun parseHtml(html: String): Document {
        return Jsoup.parse(html)
    }
    
    /**
     * 使用 CSS 选择器选择元素
     */
    fun selectElements(html: String, selector: String): Elements {
        return Jsoup.parse(html).select(selector)
    }
    
    /**
     * 获取元素的文本
     */
    fun getElementText(html: String, selector: String): String {
        return Jsoup.parse(html).select(selector).firstOrNull()?.text() ?: ""
    }
    
    /**
     * 获取元素的属性
     */
    fun getElementAttr(html: String, selector: String, attr: String): String {
        return Jsoup.parse(html).select(selector).firstOrNull()?.attr(attr) ?: ""
    }
    
    /**
     * 清理上下文
     */
    fun cleanup() {
        Context.exit()
        logger.info("Rhino 引擎已清理")
    }
    
    /**
     * 自定义安全控制器
     */
    private class CustomSecurityController(
        private val securityPolicy: SecurityPolicy
    ) : org.mozilla.javascript.SecurityController {
        
        override fun getStaticSecurityDomain(clazz: Class<*>?): Any {
            return this
        }
        
        override fun execWithDomain(cx: Context?, callable: Runnable, domain: Any?) {
            callable.run()
        }
        
        override fun getDynamicSecurityDomain(script: Any?): Any {
            return this
        }
        
        override fun getPackageAccess(packageName: String?): Array<String> {
            return arrayOf()
        }
        
        override fun getClassAccess(className: String?): Boolean {
            if (className == null) {
                return false
            }
            
            return securityPolicy.allows("class", className)
        }
        
        override fun getClassLoaderAccess(className: String?): Boolean {
            return false
        }
    }
}
