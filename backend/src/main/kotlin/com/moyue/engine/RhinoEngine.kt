package com.moyue.source.engine

import com.moyue.model.*
import com.moyue.source.JsExtensions
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import org.springframework.stereotype.Component
import java.util.concurrent.*
import kotlin.math.max

/**
 * Rhino JavaScript 引擎封装
 * 提供书源执行环境
 */
@Component
class RhinoEngine(
    private val jsExtensions: JsExtensions
) {
    // 线程池用于超时控制
    private val executor = ThreadPoolExecutor(
        4, 8, 60, TimeUnit.SECONDS,
        LinkedBlockingQueue(100),
        ThreadPoolExecutor.CallerRunsPolicy()
    )

    // 观察器阈值，用于中断长时间运行的脚本
    private val instructionObserverThreshold = 10000000

    data class ExecuteResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null,
        val executionTime: Long = 0
    )

    /**
     * 执行书源搜索规则
     */
    fun executeSearch(
        source: BookSource,
        key: String,
        timeoutMs: Long = 10000
    ): ExecuteResult {
        val searchUrl = source.searchUrl
            ?: return ExecuteResult(false, null, "搜索URL为空")
        
        // 构建实际搜索 URL
        val actualUrl = buildSearchUrl(searchUrl, key, 1)
        
        // 构建执行 JS
        val jsCode = buildString {
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
            appendLine("        if (!book.bookUrl.startsWith(\"http\")) {")
            appendLine("            book.bookUrl = java.parseUrl(book.bookUrl, baseUrl);")
            appendLine("        }")
            appendLine("        books.push(book);")
            appendLine("    }")
            appendLine("}")
            appendLine("JSON.stringify(books);")
        }

        return executeWithTimeout(jsCode, source, timeoutMs)
    }

    /**
     * 执行书籍详情规则
     */
    fun executeBookInfo(
        source: BookSource,
        book: Book,
        timeoutMs: Long = 10000
    ): ExecuteResult {
        val jsCode = buildString {
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
                appendLine("if (tocUrl && !tocUrl.startsWith(\"http\")) {")
                appendLine("    tocUrl = java.parseUrl(tocUrl, baseUrl);")
                appendLine("}")
                appendLine("bookInfo.tocUrl = tocUrl || bookUrl;")
            } ?: appendLine("bookInfo.tocUrl = bookUrl;")
            appendLine("JSON.stringify(bookInfo);")
        }

        return executeWithTimeout(jsCode, source, timeoutMs, book = book)
    }

    /**
     * 执行目录规则
     */
    fun executeToc(
        source: BookSource,
        book: Book,
        timeoutMs: Long = 15000
    ): ExecuteResult {
        val tocUrl = book.bookUrl // 简化处理，实际应从 bookInfo 获取
        
        val jsCode = buildString {
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
            appendLine("        if (!chapter.url.startsWith(\"http\")) {")
            appendLine("            chapter.url = java.parseUrl(chapter.url, baseUrl);")
            appendLine("        }")
            appendLine("        chapter.index = i;")
            appendLine("        chapters.push(chapter);")
            appendLine("    }")
            appendLine("}")
            appendLine("JSON.stringify(chapters);")
        }

        return executeWithTimeout(jsCode, source, timeoutMs, book = book)
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
        val jsCode = buildString {
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
                appendLine("    content += contentElements.get(i).text() + \"\\n\\n\";")
                appendLine("}")
            } ?: appendLine("var content = doc.text();")
            appendLine("JSON.stringify(content);")
        }

        return executeWithTimeout(jsCode, source, timeoutMs, book = book, chapter = chapter)
    }

    /**
     * 构建搜索 URL
     */
    private fun buildSearchUrl(template: String, key: String, page: Int): String {
        return template
            .replace("{{key}}", java.net.URLEncoder.encode(key, "UTF-8"))
            .replace("{{page}}", page.toString())
    }

    /**
     * 带超时的脚本执行
     */
    private fun executeWithTimeout(
        jsCode: String,
        source: BookSource,
        timeoutMs: Long,
        book: Book? = null,
        chapter: BookChapter? = null
    ): ExecuteResult {
        val future = executor.submit {
            executeScript(jsCode, source, book, chapter)
        }

        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            ExecuteResult(false, null, "执行超时 (${timeoutMs}ms)")
        } catch (e: Exception) {
            ExecuteResult(false, null, "执行错误: ${e.message}")
        }
    }

    /**
     * 在 Rhino 上下文中执行脚本
     */
    private fun executeScript(
        jsCode: String,
        source: BookSource,
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

        val context = Context.enter()
        return try {
            // 配置 Rhino - 关键修复：optimizationLevel = -1
            context.optimizationLevel = -1  // ✅ 修复：关闭优化，避免某些 JS 语法错误
            context.languageVersion = Context.VERSION_ES6

            // 创建全局作用域
            val scope = context.initStandardObjects()

            // 注入 java 对象（关键！）
            val jsJava = Context.javaToJS(jsExtensions, scope)
            ScriptableObject.putProperty(scope, "java", jsJava)

            // 注入 org.jsoup 用于 HTML 解析
            ScriptableObject.putProperty(scope, "org", Context.javaToJS(org.jsoup.Jsoup::class.java, scope))

            // 注入常用工具
            ScriptableObject.putProperty(scope, "source", Context.javaToJS(source, scope))
            book?.let { ScriptableObject.putProperty(scope, "book", Context.javaToJS(it, scope)) }
            chapter?.let { ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(it, scope)) }

            // 设置观察器用于中断
            context.setInstructionObserverThreshold(instructionObserverThreshold)
            context.setGenerateObserverCount(true)

            // 关键修复：添加 ClassShutter 安全限制
            context.setClassShutter { className ->
                // 禁止访问危险类
                when {
                    className.startsWith("java.lang.Runtime") -> false
                    className.startsWith("java.lang.ProcessBuilder") -> false
                    className.startsWith("java.io.FileInputStream") -> false
                    className.startsWith("java.io.FileOutputStream") -> false
                    className.startsWith("java.net.Socket") -> false
                    className.startsWith("java.net.ServerSocket") -> false
                    className.startsWith("sun.") -> false
                    className.startsWith("com.sun.") -> false
                    else -> true
                }
            }

            // 执行脚本
            val result = context.evaluateString(scope, jsCode, "source", 1, null)

            ExecuteResult(
                success = true,
                result = Context.jsToJava(result, Any::class.java),
                executionTime = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            ExecuteResult(
                success = false,
                error = "${e::class.simpleName}: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime
            )
        } finally {
            Context.exit()
            jsExtensions.clearContext()
        }
    }

    /**
     * 测试书源可用性
     */
    fun testSource(source: BookSource): Map<String, Any> {
        val results = mutableMapOf<String, Any>()

        // 测试搜索
        val searchResult = executeSearch(source, "测试", 5000)
        results["search"] = mapOf(
            "success" to searchResult.success,
            "time" to searchResult.executionTime,
            "error" to (searchResult.error ?: ""),
            "result" to (searchResult.result ?: "")
        )

        return results
    }

    private fun escapeJsString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("'", "\\'")
    }

    /**
     * 关闭引擎
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}
