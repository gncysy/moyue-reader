package com.moyue.source.engine

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
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
    private val instructionObserverThreshold = 10000000 // 约 10 秒
    
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
        val ruleSearch = source.ruleSearch ?: return ExecuteResult(false, null, "搜索规则为空")
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var key = \"${escapeJsString(key)}\";")
            appendLine("var result = ${ruleSearch.bookList};")
            appendLine("JSON.stringify(result);")
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
        val ruleBookInfo = source.ruleBookInfo ?: return ExecuteResult(false, null, "详情规则为空")
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var book = ${book.toJson()};")
            appendLine("var result = ${ruleBookInfo.init};")
            appendLine("JSON.stringify(result);")
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
        val ruleToc = source.ruleToc ?: return ExecuteResult(false, null, "目录规则为空")
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var book = ${book.toJson()};")
            appendLine("var result = ${ruleToc.chapterList};")
            appendLine("JSON.stringify(result);")
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
        val ruleContent = source.ruleContent ?: return ExecuteResult(false, null, "正文规则为空")
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var book = ${book.toJson()};")
            appendLine("var chapter = ${chapter.toJson()};")
            appendLine("var result = ${ruleContent.content};")
            appendLine("JSON.stringify(result);")
        }
        
        return executeWithTimeout(jsCode, source, timeoutMs, book = book, chapter = chapter)
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
        val future = executor.submit<ExecuteResult> {
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
            // 配置 Rhino
            context.optimizationLevel = 9
            context.languageVersion = Context.VERSION_ES6
            
            // 创建全局作用域
            val scope = context.initStandardObjects()
            
            // 注入 java 对象（关键！）
            val jsJava = Context.javaToJS(jsExtensions, scope)
            ScriptableObject.putProperty(scope, "java", jsJava)
            
            // 注入常用工具
            ScriptableObject.putProperty(scope, "source", Context.javaToJS(source, scope))
            book?.let { ScriptableObject.putProperty(scope, "book", Context.javaToJS(it, scope)) }
            chapter?.let { ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(it, scope)) }
            
            // 设置观察器用于中断
            context.setInstructionObserverThreshold(instructionObserverThreshold)
            context.setGenerateObserverCount(true)
            
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
            "error" to (searchResult.error ?: "")
        )
        
        // 如果搜索成功，测试详情
        if (searchResult.success && searchResult.result != null) {
            val books = parseSearchResult(searchResult.result)
            if (books.isNotEmpty()) {
                val bookResult = executeBookInfo(source, books[0], 5000)
                results["bookInfo"] = mapOf(
                    "success" to bookResult.success,
                    "time" to bookResult.executionTime,
                    "error" to (bookResult.error ?: "")
                )
            }
        }
        
        return results
    }
    
    private fun parseSearchResult(result: Any?): List<Book> {
        // 解析搜索结果为 Book 列表
        return when (result) {
            is List<*> -> result.filterIsInstance<Book>()
            is Map<*, *> -> {
                // 单个结果转列表
                val book = Book(
                    name = result["name"] as? String ?: "",
                    author = result["author"] as? String ?: "",
                    bookUrl = result["bookUrl"] as? String ?: ""
                )
                listOf(book)
            }
            else -> emptyList()
        }
    }
    
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
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
