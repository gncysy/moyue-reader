package com.moyue.source.engine

import com.moyue.model.*
import com.moyue.security.SecurityPolicy
import com.moyue.source.JsExtensions
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Rhino JavaScript 引擎封装
 * 提供书源执行环境
 */
@Component
class RhinoEngine(
    private val jsExtensions: JsExtensions,
    private val securityPolicy: SecurityPolicy
) {
    
    companion object {
        private const val DEFAULT_THREAD_POOL_CORE_SIZE = 4
        private const val DEFAULT_THREAD_POOL_MAX_SIZE = 8
        private const val DEFAULT_THREAD_POOL_QUEUE_SIZE = 100
        private const val DEFAULT_INSTRUCTION_OBSERVER_THRESHOLD = 1000000
        private const val MAX_SCRIPT_SIZE = 1_000_000 // 1MB
        private const val MAX_EXECUTION_TIME_DEFAULT = 30000L
    }
    
    private val logger = LoggerFactory.getLogger(RhinoEngine::class.java)
    
    // 线程池用于超时控制
    private val executor = ThreadPoolExecutor(
        DEFAULT_THREAD_POOL_CORE_SIZE,
        DEFAULT_THREAD_POOL_MAX_SIZE,
        60,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(DEFAULT_THREAD_POOL_QUEUE_SIZE),
        ThreadFactory { r -> Thread(r, "rhino-engine-${r.hashCode()}") },
        ThreadPoolExecutor.AbortPolicy()
    )
    
    // 执行计数器
    private val executionCounter = AtomicLong(0)
    
    // 失败计数器
    private val failureCounter = AtomicLong(0)
    
    // 观察器阈值，用于中断长时间运行的脚本
    private val instructionObserverThreshold: Int = DEFAULT_INSTRUCTION_OBSERVER_THRESHOLD
    
    // 活跃执行任务
    private val activeExecutions = ConcurrentHashMap<Long, ExecutionContext>()
    
    data class ExecuteResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null,
        val executionTime: Long = 0,
        val executionId: Long = 0
    )
    
    data class ExecutionContext(
        val id: Long,
        val source: BookSource,
        val operation: String,
        val startTime: Long = System.currentTimeMillis()
    )
    
    init {
        logger.info("Rhino引擎初始化完成")
        logger.info("线程池配置: 核心线程={}, 最大线程={}, 队列大小={}", 
            DEFAULT_THREAD_POOL_CORE_SIZE, DEFAULT_THREAD_POOL_MAX_SIZE, DEFAULT_THREAD_POOL_QUEUE_SIZE)
    }
    
    /**
     * 执行书源搜索规则
     */
    fun executeSearch(
        source: BookSource,
        key: String,
        timeoutMs: Long = 10000
    ): ExecuteResult {
        if (key.isBlank()) {
            logger.warn("搜索关键词为空: ${source.name}")
            return ExecuteResult(false, null, "搜索关键词不能为空")
        }
        
        if (source.searchUrl.isNullOrBlank()) {
            logger.warn("搜索URL为空: ${source.name}")
            return ExecuteResult(false, null, "搜索URL为空")
        }
        
        if (source.ruleSearch == null) {
            logger.warn("搜索规则为空: ${source.name}")
            return ExecuteResult(false, null, "搜索规则为空")
        }
        
        // 构建实际搜索 URL
        val actualUrl = try {
            buildSearchUrl(source.searchUrl, key, 1)
        } catch (e: Exception) {
            logger.error("构建搜索URL失败: ${source.searchUrl}", e)
            return ExecuteResult(false, null, "构建搜索URL失败: ${e.message}")
        }
        
        // 构建执行 JS
        val jsCode = buildSearchJsCode(source, key, actualUrl)
        
        val context = ExecutionContext(
            id = executionCounter.incrementAndGet(),
            source = source,
            operation = "search"
        )
        
        return executeWithTimeout(jsCode, source, timeoutMs, context)
    }
    
    /**
     * 执行书籍详情规则
     */
    fun executeBookInfo(
        source: BookSource,
        book: Book,
        timeoutMs: Long = 10000
    ): ExecuteResult {
        if (book.bookUrl.isBlank()) {
            logger.warn("书籍URL为空")
            return ExecuteResult(false, null, "书籍URL不能为空")
        }
        
        if (source.ruleBookInfo == null) {
            logger.warn("书籍详情规则为空: ${source.name}")
            return ExecuteResult(false, null, "书籍详情规则为空")
        }
        
        val jsCode = buildBookInfoJsCode(source, book)
        
        val context = ExecutionContext(
            id = executionCounter.incrementAndGet(),
            source = source,
            operation = "bookInfo"
        )
        
        return executeWithTimeout(jsCode, source, timeoutMs, context, book)
    }
    
    /**
     * 执行目录规则
     */
    fun executeToc(
        source: BookSource,
        book: Book,
        timeoutMs: Long = 15000
    ): ExecuteResult {
        val tocUrl = book.tocUrl ?: book.bookUrl
        
        if (tocUrl.isBlank()) {
            logger.warn("目录URL为空")
            return ExecuteResult(false, null, "目录URL不能为空")
        }
        
        if (source.ruleToc == null) {
            logger.warn("目录规则为空: ${source.name}")
            return ExecuteResult(false, null, "目录规则为空")
        }
        
        val jsCode = buildTocJsCode(source, book, tocUrl)
        
        val context = ExecutionContext(
            id = executionCounter.incrementAndGet(),
            source = source,
            operation = "toc"
        )
        
        return executeWithTimeout(jsCode, source, timeoutMs, context, book)
    }
    
    /**
     * 执行正文规则
     */
    fun executeContent(
        source: BookSource,
        book: Book,
        chapter: BookChapter,
        timeoutMs: Long = 15000
    ): ExecuteResult {
        if (chapter.url.isBlank()) {
            logger.warn("章节URL为空")
            return ExecuteResult(false, null, "章节URL不能为空")
        }
        
        if (source.ruleContent == null) {
            logger.warn("正文规则为空: ${source.name}")
            return ExecuteResult(false, null, "正文规则为空")
        }
        
        val jsCode = buildContentJsCode(source, book, chapter)
        
        val context = ExecutionContext(
            id = executionCounter.incrementAndGet(),
            source = source,
            operation = "content"
        )
        
        return executeWithTimeout(jsCode, source, timeoutMs, context, book, chapter)
    }
    
    /**
     * 测试书源可用性
     */
    fun testSource(source: BookSource): Map<String, Any> {
        logger.info("开始测试书源: ${source.name}")
        
        val results = mutableMapOf<String, Any>()
        val testResults = mutableMapOf<String, TestResult>()
        var allPassed = true
        
        // 测试搜索
        val searchResult = executeSearch(source, "测试", 5000)
        testResults["search"] = TestResult(
            action = "搜索",
            success = searchResult.success,
            time = searchResult.executionTime,
            error = searchResult.error,
            result = if (searchResult.success) "成功" else "失败"
        )
        if (!searchResult.success) allPassed = false
        
        // 如果搜索成功，测试书籍详情
        if (searchResult.success) {
            try {
                val books = parseTestResult(searchResult.result)
                if (books.isNotEmpty()) {
                    val testBook = books.first()
                    val bookInfoResult = executeBookInfo(source, testBook, 5000)
                    testResults["bookInfo"] = TestResult(
                        action = "书籍详情",
                        success = bookInfoResult.success,
                        time = bookInfoResult.executionTime,
                        error = bookInfoResult.error,
                        result = if (bookInfoResult.success) "成功" else "失败"
                    )
                    if (!bookInfoResult.success) allPassed = false
                    
                    // 测试目录
                    if (bookInfoResult.success) {
                        val tocResult = executeToc(source, testBook, 5000)
                        testResults["toc"] = TestResult(
                            action = "目录",
                            success = tocResult.success,
                            time = tocResult.executionTime,
                            error = tocResult.error,
                            result = if (tocResult.success) "成功" else "失败"
                        )
                        if (!tocResult.success) allPassed = false
                        
                        // 测试正文
                        if (tocResult.success) {
                            val chapters = parseTestResult(tocResult.result)
                            if (chapters.isNotEmpty()) {
                                val testChapter = chapters.first()
                                val contentResult = executeContent(
                                    source,
                                    testBook,
                                    testChapter,
                                    5000
                                )
                                testResults["content"] = TestResult(
                                    action = "正文",
                                    success = contentResult.success,
                                    time = contentResult.executionTime,
                                    error = contentResult.error,
                                    result = if (contentResult.success) "成功" else "失败"
                                )
                                if (!contentResult.success) allPassed = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("测试书源异常: ${source.name}", e)
            }
        }
        
        results["success"] = allPassed
        results["sourceName"] = source.name
        results["testResults"] = testResults
        results["summary"] = if (allPassed) "测试通过" else "测试失败"
        
        logger.info("书源测试完成: ${source.name}, 结果: {}", results["summary"])
        
        return results
    }
    
    /**
     * 构建搜索 URL
     */
    private fun buildSearchUrl(template: String, key: String, page: Int): String {
        return template
            .replace("{{key}}", java.net.URLEncoder.encode(key, "UTF-8"))
            .replace("{{page}}", page.toString())
            .replace("{key}", java.net.URLEncoder.encode(key, "UTF-8"))
            .replace("{page}", page.toString())
    }
    
    /**
     * 构建搜索 JS 代码
     */
    private fun buildSearchJsCode(source: BookSource, key: String, actualUrl: String): String {
        return buildString {
            appendLine("// 搜索脚本 - 书源: ${source.name}")
            appendLine(source.headerJs ?: "")
            appendLine("var key = \"${escapeJsString(key)}\";")
            appendLine("var page = 1;")
            appendLine("var baseUrl = \"${escapeJsString(source.url)}\";")
            appendLine("var searchUrl = \"${escapeJsString(actualUrl)}\";")
            appendLine("")
            appendLine("// 获取搜索页面 HTML")
            appendLine("var html = java.get(searchUrl);")
            appendLine("")
            appendLine("// 解析书籍列表")
            appendLine("var books = [];")
            appendLine("var bookList = org.jsoup.Jsoup.parse(html).select(\"${escapeJsString(source.ruleSearch?.bookList ?: "")}\");")
            appendLine("for (var i = 0; i < bookList.size(); i++) {")
            appendLine("    var item = bookList.get(i);")
            appendLine("    var book = {};")
            source.ruleSearch?.name?.let { appendLine("    book.name = item.select(\"${escapeJsString(it)}\").text();") }
            source.ruleSearch?.author?.let { appendLine("    book.author = item.select(\"${escapeJsString(it)}\").text();") }
            source.ruleSearch?.coverUrl?.let { appendLine("    book.coverUrl = item.select(\"${escapeJsString(it)}\").attr(\"src\");") }
            source.ruleSearch?.intro?.let { appendLine("    book.intro = item.select(\"${escapeJsString(it)}\").text();") }
            source.ruleSearch?.bookUrl?.let { appendLine("    book.bookUrl = item.select(\"${escapeJsString(it)}\").attr(\"href\");") }
            source.ruleSearch?.lastChapter?.let { appendLine("    book.lastChapter = item.select(\"${escapeJsString(it)}\").text();") }
            appendLine("    if (book.name && book.bookUrl) {")
            appendLine("        if (!book.bookUrl.startsWith(\"http\") && !book.bookUrl.startsWith(\"/\")) {")
            appendLine("            book.bookUrl = java.parseUrl(book.bookUrl, baseUrl);")
            appendLine("        }")
            appendLine("        books.push(book);")
            appendLine("    }")
            appendLine("}")
            appendLine("JSON.stringify(books);")
        }
    }
    
    /**
     * 构建书籍详情 JS 代码
     */
    private fun buildBookInfoJsCode(source: BookSource, book: Book): String {
        return buildString {
            appendLine("// 书籍详情脚本 - 书源: ${source.name}")
            appendLine(source.headerJs ?: "")
            appendLine("var bookUrl = \"${escapeJsString(book.bookUrl)}\";")
            appendLine("var baseUrl = \"${escapeJsString(source.url)}\";")
            appendLine("")
            appendLine("// 获取详情页面 HTML")
            appendLine("var html = java.get(bookUrl);")
            appendLine("var doc = org.jsoup.Jsoup.parse(html);")
            appendLine("")
            appendLine("// 解析书籍信息")
            appendLine("var bookInfo = {};")
            source.ruleBookInfo?.name?.let { appendLine("bookInfo.name = doc.select(\"${escapeJsString(it)}\").text();") }
            source.ruleBookInfo?.author?.let { appendLine("bookInfo.author = doc.select(\"${escapeJsString(it)}\").text();") }
            source.ruleBookInfo?.intro?.let { appendLine("bookInfo.intro = doc.select(\"${escapeJsString(it)}\").text();") }
            source.ruleBookInfo?.coverUrl?.let { appendLine("bookInfo.coverUrl = doc.select(\"${escapeJsString(it)}\").attr(\"src\");") }
            source.ruleBookInfo?.lastChapter?.let { appendLine("bookInfo.lastChapter = doc.select(\"${escapeJsString(it)}\").text();") }
            source.ruleBookInfo?.tocUrl?.let { 
                appendLine("var tocUrl = doc.select(\"${escapeJsString(it)}\").attr(\"href\");")
                appendLine("if (tocUrl && !tocUrl.startsWith(\"http\") && !tocUrl.startsWith(\"/\")) {")
                appendLine("    tocUrl = java.parseUrl(tocUrl, baseUrl);")
                appendLine("}")
                appendLine("bookInfo.tocUrl = tocUrl || bookUrl;")
            } ?: appendLine("bookInfo.tocUrl = bookUrl;")
            appendLine("JSON.stringify(bookInfo);")
        }
    }
    
    /**
     * 构建目录 JS 代码
     */
    private fun buildTocJsCode(source: BookSource, book: Book, tocUrl: String): String {
        return buildString {
            appendLine("// 目录脚本 - 书源: ${source.name}")
            appendLine(source.headerJs ?: "")
            appendLine("var bookUrl = \"${escapeJsString(book.bookUrl)}\";")
            appendLine("var tocUrl = \"${escapeJsString(tocUrl)}\";")
            appendLine("var baseUrl = \"${escapeJsString(source.url)}\";")
            appendLine("")
            appendLine("// 获取目录页面 HTML")
            appendLine("var html = java.get(tocUrl);")
            appendLine("")
            appendLine("// 解析章节列表")
            appendLine("var chapters = [];")
            appendLine("var chapterList = org.jsoup.Jsoup.parse(html).select(\"${escapeJsString(source.ruleToc?.chapterList ?: "")}\");")
            appendLine("for (var i = 0; i < chapterList.size(); i++) {")
            appendLine("    var item = chapterList.get(i);")
            appendLine("    var chapter = {};")
            source.ruleToc?.chapterName?.let { appendLine("    chapter.title = item.select(\"${escapeJsString(it)}\").text();") }
            source.ruleToc?.chapterUrl?.let { appendLine("    chapter.url = item.select(\"${escapeJsString(it)}\").attr(\"href\");") }
            appendLine("    if (chapter.title && chapter.url) {")
            appendLine("        if (!chapter.url.startsWith(\"http\") && !chapter.url.startsWith(\"/\")) {")
            appendLine("            chapter.url = java.parseUrl(chapter.url, baseUrl);")
            appendLine("        }")
            appendLine("        chapter.index = i;")
            appendLine("        chapters.push(chapter);")
            appendLine("    }")
            appendLine("}")
            appendLine("JSON.stringify(chapters);")
        }
    }
    
    /**
     * 构建正文 JS 代码
     */
    private fun buildContentJsCode(source: BookSource, book: Book, chapter: BookChapter): String {
        return buildString {
            appendLine("// 正文脚本 - 书源: ${source.name}")
            appendLine(source.headerJs ?: "")
            appendLine("var chapterUrl = \"${escapeJsString(chapter.url)}\";")
            appendLine("var baseUrl = \"${escapeJsString(source.url)}\";")
            appendLine("")
            appendLine("// 获取正文页面 HTML")
            appendLine("var html = java.get(chapterUrl);")
            appendLine("var doc = org.jsoup.Jsoup.parse(html);")
            appendLine("")
            appendLine("// 解析正文内容")
            source.ruleContent?.content?.let { 
                appendLine("var contentElements = doc.select(\"${escapeJsString(it)}\");")
                appendLine("var content = \"\";")
                appendLine("for (var i = 0; i < contentElements.size(); i++) {")
                appendLine("    var element = contentElements.get(i);")
                appendLine("    content += element.text() + \"\\n\\n\";")
                appendLine("}")
            } ?: appendLine("var content = doc.text();")
            appendLine("")
            appendLine("// 清理内容")
            appendLine("content = content.replace(/\\s+/g, \" \");")
            appendLine("content = content.trim();")
            appendLine("JSON.stringify(content);")
        }
    }
    
    /**
     * 带超时的脚本执行
     */
    private fun executeWithTimeout(
        jsCode: String,
        source: BookSource,
        timeoutMs: Long,
        context: ExecutionContext,
        book: Book? = null,
        chapter: BookChapter? = null
    ): ExecuteResult {
        // 校验脚本大小
        if (jsCode.length > MAX_SCRIPT_SIZE) {
            logger.warn("脚本过大: ${jsCode.length} > $MAX_SCRIPT_SIZE")
            return ExecuteResult(false, null, "脚本过大，超过限制")
        }
        
        // 校验超时时间
        val actualTimeout = timeoutMs.coerceAtMost(securityPolicy.timeoutMs)
        
        // 记录活跃执行
        activeExecutions[context.id] = context
        
        try {
            val future = executor.submit<ExecuteResult> {
                executeScript(jsCode, source, context, book, chapter)
            }
            
            val result = try {
                future.get(actualTimeout, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                logger.warn("执行超时: 书源={}, 操作={}, 超时={}ms", 
                    source.name, context.operation, actualTimeout)
                future.cancel(true)
                failureCounter.incrementAndGet()
                ExecuteResult(false, null, "执行超时 (${actualTimeout}ms)", 0, context.id)
            } catch (e: Exception) {
                logger.error("执行异常: 书源={}, 操作={}", source.name, context.operation, e)
                failureCounter.incrementAndGet()
                ExecuteResult(false, null, "执行错误: ${e.message}", 0, context.id)
            }
            
            return result
        } finally {
            activeExecutions.remove(context.id)
        }
    }
    
    /**
     * 在 Rhino 上下文中执行脚本
     */
    private fun executeScript(
        jsCode: String,
        source: BookSource,
        context: ExecutionContext,
        book: Book?,
        chapter: BookChapter?
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        // 设置执行上下文
        jsExtensions.setContext(JsExtensions.ExecutionContext(
            source = source,
            book = book,
            chapter = chapter
        ))
        
        val rhinoContext = Context.enter()
        return try {
            // 配置 Rhino
            rhinoContext.optimizationLevel = -1  // 关闭优化，避免某些 JS 语法错误
            rhinoContext.languageVersion = Context.VERSION_ES5  // 使用 ES5（更稳定）
            
            // 创建全局作用域
            val scope = rhinoContext.initStandardObjects()
            
            // 注入 java 对象
            val jsJava = Context.javaToJS(jsExtensions, scope)
            ScriptableObject.putProperty(scope, "java", jsJava)
            
            // 注入 org.jsoup 用于 HTML 解析
            ScriptableObject.putProperty(scope, "org", 
                Context.javaToJS(org.jsoup.Jsoup::class.java, scope))
            
            // 注入常用对象
            ScriptableObject.putProperty(scope, "source", Context.javaToJS(source, scope))
            book?.let { ScriptableObject.putProperty(scope, "book", Context.javaToJS(it, scope)) }
            chapter?.let { ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(it, scope)) }
            
            // 设置观察器用于中断
            rhinoContext.setInstructionObserverThreshold(instructionObserverThreshold)
            rhinoContext.setGenerateObserverCount(true)
            
            // 设置 ClassShutter 安全限制
            rhinoContext.setClassShutter { className ->
                isClassAllowed(className)
            }
            
            // 执行脚本
            val result = rhinoContext.evaluateString(scope, jsCode, 
                "${source.name}-${context.operation}", 1, null)
            
            val executionTime = System.currentTimeMillis() - startTime
            logger.debug("执行成功: 书源={}, 操作={}, 耗时={}ms", 
                source.name, context.operation, executionTime)
            
            ExecuteResult(
                success = true,
                result = Context.jsToJava(result, Any::class.java),
                executionTime = executionTime,
                executionId = context.id
            )
            
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error("脚本执行失败: 书源={}, 操作={}", source.name, context.operation, e)
            
            ExecuteResult(
                success = false,
                error = "${e::class.simpleName}: ${e.message}",
                executionTime = executionTime,
                executionId = context.id
            )
        } finally {
            Context.exit()
            jsExtensions.clearContext()
        }
    }
    
    /**
     * 检查类是否允许访问
     */
    private fun isClassAllowed(className: String): Boolean {
        // 禁止访问危险类
        val blockedPackages = listOf(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.System",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.FileReader",
            "java.io.FileWriter",
            "java.io.RandomAccessFile",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.DatagramSocket",
            "sun.",
            "com.sun."
        )
        
        return !blockedPackages.any { className.startsWith(it) }
    }
    
    /**
     * 转义 JS 字符串
     */
    private fun escapeJsString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("'", "\\'")
            .replace("/", "\\/")
            .replace("\u0000", "\\0")
    }
    
    /**
     * 解析测试结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseTestResult(result: Any?): List<Map<String, Any>> {
        return try {
            when (result) {
                is String -> {
                    com.google.gson.Gson().fromJson(result, 
                        object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type) 
                        as? List<Map<String, Any>> ?: emptyList()
                }
                is List<*> -> {
                    result.filterIsInstance<Map<String, Any>>()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            logger.error("解析测试结果失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取执行统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "totalExecutions" to executionCounter.get(),
            "totalFailures" to failureCounter.get(),
            "activeExecutions" to activeExecutions.size,
            "executor" to mapOf(
                "poolSize" to executor.poolSize,
                "activeCount" to executor.activeCount,
                "queueSize" to executor.queue.size,
                "completedTaskCount" to executor.completedTaskCount
            )
        )
    }
    
    /**
     * 重置统计
     */
    fun resetStats() {
        executionCounter.set(0)
        failureCounter.set(0)
        logger.info("执行统计已重置")
    }
    
    /**
     * 关闭引擎
     */
    fun shutdown() {
        logger.info("正在关闭 Rhino 引擎...")
        
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("线程池未在60秒内关闭，强制关闭")
                executor.shutdownNow()
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("线程池强制关闭失败")
                }
            }
        } catch (e: InterruptedException) {
            logger.error("线程池关闭被中断", e)
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
        
        activeExecutions.clear()
        logger.info("Rhino 引擎已关闭")
    }
    
    /**
     * 测试结果
     */
    data class TestResult(
        val action: String,
        val success: Boolean,
        val time: Long,
        val error: String?,
        val result: String
    )
}
