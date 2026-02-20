package com.moyue.engine

import com.google.gson.Gson
import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.security.SecurityPolicy
import com.moyue.source.JsExtensions
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import org.springframework.stereotype.Component
import java.util.concurrent.*

@Component
class RhinoEngine(
    private val jsExtensions: JsExtensions
) {
    
    private val executor = ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS,
        LinkedBlockingQueue(20)
    )
    
    private val gson = Gson()
    
    data class ExecuteResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null,
        val executionTime: Long = 0
    ) {
        fun copy(
            success: Boolean = this.success,
            result: Any? = this.result,
            error: String? = this.error,
            executionTime: Long = this.executionTime
        ): ExecuteResult {
            return ExecuteResult(success, result, error, executionTime)
        }
    }
    
    data class ExecutionContext(
        val source: BookSource? = null,
        val book: Book? = null,
        val chapter: BookChapter? = null,
        val baseUrl: String = "",
        val variable: String? = null
    )

    // ==================== 通用执行方法（带安全策略）====================
    
    fun execute(
        jsCode: String,
        policy: SecurityPolicy,
        functionName: String? = null,
        vararg args: Any
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        val future = executor.submit(Callable {
            executeInternal(jsCode, policy, functionName, *args)
        })

        return try {
            future.get(policy.timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            ExecuteResult.Timeout(policy.timeoutMs)
        } catch (e: Exception) {
            ExecuteResult.Error(e.message ?: "未知错误")
        }
    }

    // ==================== 书源专用方法（新增）====================
    
    fun executeSearch(source: BookSource, keyword: String): ExecuteResult {
        val policy = SecurityPolicy.fromRating(source.securityRating)
        val searchRule = source.getSearchRule()
        
        if (searchRule.bookList.isNullOrEmpty()) {
            return ExecuteResult(false, null, "搜索规则为空")
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var key = \"${escapeJsString(keyword)}\";")
            appendLine("var result = ${searchRule.bookList};")
            appendLine("JSON.stringify(result);")
        }
        
        return executeWithContext(jsCode, policy, ExecutionContext(source = source))
    }
    
    fun executeBookInfo(source: BookSource, book: Book): ExecuteResult {
        val policy = SecurityPolicy.fromRating(source.securityRating)
        val bookInfoRule = source.getBookInfoRule()
        
        if (bookInfoRule.init.isNullOrEmpty()) {
            return ExecuteResult(true, book, executionTime = 0)
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var book = ${gson.toJson(book)};")
            appendLine("var result = ${bookInfoRule.init};")
            appendLine("JSON.stringify(result);")
        }
        
        return executeWithContext(jsCode, policy, ExecutionContext(source = source, book = book))
    }
    
    fun executeToc(source: BookSource, book: Book): ExecuteResult {
        val policy = SecurityPolicy.fromRating(source.securityRating)
        val tocRule = source.getTocRule()
        
        if (tocRule.chapterList.isNullOrEmpty()) {
            return ExecuteResult(false, null, "目录规则为空")
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var book = ${gson.toJson(book)};")
            appendLine("var result = ${tocRule.chapterList};")
            appendLine("JSON.stringify(result);")
        }
        
        return executeWithContext(jsCode, policy, ExecutionContext(source = source, book = book))
    }
    
    fun executeContent(source: BookSource, book: Book, chapter: BookChapter): ExecuteResult {
        val policy = SecurityPolicy.fromRating(source.securityRating)
        val contentRule = source.getContentRule()
        
        if (contentRule.content.isNullOrEmpty()) {
            return ExecuteResult(false, null, "正文规则为空")
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var book = ${gson.toJson(book)};")
            appendLine("var chapter = ${gson.toJson(chapter)};")
            appendLine("var result = ${contentRule.content};")
            appendLine("JSON.stringify(result);")
        }
        
        return executeWithContext(jsCode, policy, ExecutionContext(source = source, book = book, chapter = chapter))
    }
    
    fun testSource(source: BookSource): Map<String, Any> {
        val results = mutableMapOf<String, Any>()
        
        val searchStart = System.currentTimeMillis()
        val searchResult = executeSearch(source, "测试")
        val searchTime = System.currentTimeMillis() - searchStart
        
        results["search"] = mapOf(
            "success" to searchResult.success,
            "time" to searchTime,
            "error" to (searchResult.error ?: "")
        )
        
        if (searchResult.success && searchResult.result != null) {
            try {
                val books = parseSearchResult(searchResult.result)
                if (books.isNotEmpty()) {
                    val bookStart = System.currentTimeMillis()
                    val bookResult = executeBookInfo(source, books[0])
                    val bookTime = System.currentTimeMillis() - bookStart
                    
                    results["bookInfo"] = mapOf(
                        "success" to bookResult.success,
                        "time" to bookTime,
                        "error" to (bookResult.error ?: "")
                    )
                    
                    if (bookResult.success) {
                        val tocStart = System.currentTimeMillis()
                        val tocResult = executeToc(source, books[0])
                        val tocTime = System.currentTimeMillis() - tocStart
                        
                        results["toc"] = mapOf(
                            "success" to tocResult.success,
                            "time" to tocTime,
                            "error" to (tocResult.error ?: "")
                        )
                    }
                }
            } catch (e: Exception) {
                results["bookInfo"] = mapOf(
                    "success" to false,
                    "error" to e.message
                )
            }
        }
        
        return results
    }

    // ==================== 内部执行逻辑 ====================
    
    private fun executeWithContext(
        jsCode: String,
        policy: SecurityPolicy,
        context: ExecutionContext
    ): ExecuteResult {
        val future = executor.submit(Callable {
            executeWithContextInternal(jsCode, policy, context)
        })

        return try {
            future.get(policy.timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            ExecuteResult.Timeout(policy.timeoutMs)
        } catch (e: Exception) {
            ExecuteResult.Error(e.message ?: "执行错误")
        }
    }
    
    private fun executeInternal(
        jsCode: String,
        policy: SecurityPolicy,
        functionName: String? = null,
        vararg args: Any
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        val cx = Context.enter()
        
        return try {
            configureContext(cx, policy)
            val scope = cx.initStandardObjects()

            val jsJava = Context.javaToJS(jsExtensions, scope)
            ScriptableObject.putProperty(scope, "java", jsJava)

            cx.evaluateString(scope, jsCode, "script", 1, null)

            val result = if (functionName != null) {
                val func = scope.get(functionName, scope)
                if (func is Function) {
                    val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
                    func.call(cx, scope, scope, jsArgs)
                } else {
                    "ERROR: Function $functionName not found"
                }
            } else {
                "OK"
            }

            val executionTime = System.currentTimeMillis() - startTime
            ExecuteResult.Success(Context.toString(result), executionTime)
            
        } catch (e: org.mozilla.javascript.JavaScriptException) {
            ExecuteResult.Error("JS Error: ${e.message}", System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ExecuteResult.Error("Error: ${e.message}", System.currentTimeMillis() - startTime)
        } finally {
            Context.exit()
        }
    }
    
    private fun executeWithContextInternal(
        jsCode: String,
        policy: SecurityPolicy,
        context: ExecutionContext
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        jsExtensions.setContext(JsExtensions.ExecutionContext(
            source = context.source,
            book = context.book,
            chapter = context.chapter,
            baseUrl = context.baseUrl,
            variable = context.variable
        ))

        val cx = Context.enter()
        
        return try {
            configureContext(cx, policy)
            val scope = cx.initStandardObjects()

            val jsJava = Context.javaToJS(jsExtensions, scope)
            ScriptableObject.putProperty(scope, "java", jsJava)

            context.source?.let { 
                ScriptableObject.putProperty(scope, "source", Context.javaToJS(it, scope))
            }
            context.book?.let { 
                ScriptableObject.putProperty(scope, "book", Context.javaToJS(it, scope))
            }
            context.chapter?.let { 
                ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(it, scope))
            }

            val result = cx.evaluateString(scope, jsCode, "bookSource", 1, null)
            
            val executionTime = System.currentTimeMillis() - startTime
            ExecuteResult.Success(Context.toString(result), executionTime)
            
        } catch (e: org.mozilla.javascript.JavaScriptException) {
            ExecuteResult.Error("JS Error: ${e.message}", System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ExecuteResult.Error("Error: ${e.message}", System.currentTimeMillis() - startTime)
        } finally {
            Context.exit()
            jsExtensions.clearContext()
        }
    }
    
    // ==================== Rhino 1.9.1 配置 ====================
    
    private fun configureContext(cx: Context, policy: SecurityPolicy) {
        // Rhino 1.9.1 优化配置
        cx.optimizationLevel = if (policy.allowReflection) 9 else -1
        cx.languageVersion = Context.VERSION_ES6  // ES6 支持
        cx.maximumInterpreterStackDepth = policy.maxStackDepth

        // ClassShutter 安全限制
        cx.classShutter = org.mozilla.javascript.ClassShutter { className ->
            when {
                className.startsWith("java.lang.Runtime") -> false
                className.startsWith("java.lang.ProcessBuilder") -> false
                className.startsWith("java.lang.ClassLoader") -> false
                className.startsWith("sun.misc.") -> false
                className.startsWith("java.net.ServerSocket") -> false
                className.startsWith("java.lang.reflect.") -> policy.allowReflection
                className.startsWith("java.io.File") -> policy.allowFile
                className.startsWith("java.nio.file.") -> policy.allowFile
                className.startsWith("java.net.Socket") -> policy.allowSocket
                className.startsWith("java.lang.Class") -> policy.allowReflection
                else -> true
            }
        }

        cx.setInstructionObserverThreshold(10000000)
        cx.setGenerateObserverCount(true)
    }
    
    // ==================== 辅助方法 ====================
    
    private fun escapeJsString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    private fun parseSearchResult(result: Any?): List<Book> {
        return try {
            when (result) {
                is String -> {
                    val list = gson.fromJson(result, List::class.java)
                    list?.mapNotNull { item ->
                        if (item is Map<*, *>) {
                            Book(
                                name = (item["name"] as? String)?.trim() ?: "",
                                author = (item["author"] as? String)?.trim() ?: "",
                                bookUrl = (item["bookUrl"] as? String) ?: "",
                                coverUrl = item["coverUrl"] as? String,
                                intro = item["intro"] as? String
                            )
                        } else null
                    } ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    sealed class ExecutionResult {
        data class Success(val data: String, val executionTime: Long) : ExecutionResult()
        data class Error(val message: String, val executionTime: Long = 0) : ExecutionResult()
        data class Timeout(val limitMs: Long) : ExecutionResult()
    }

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
