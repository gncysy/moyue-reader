package com.moyue.engine

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.security.SecurityPolicy
import com.moyue.security.SafeJsExtensions
import com.moyue.util.MD5Utils
import org.mozilla.javascript.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.*

/**
 * 统一的 Rhino 引擎
 * 包含：通用执行 + 书源专用方法
 */
@Component
class RhinoEngine {
    
    private val logger = LoggerFactory.getLogger(RhinoEngine::class.java)
    
    // 线程池配置
    private val executor = ThreadPoolExecutor(
        2,  // core pool size
        4,  // max pool size
        60L, TimeUnit.SECONDS,
        LinkedBlockingQueue(20)
    )
    
    // ==================== 通用执行结果 ====================
    
    sealed class ExecutionResult {
        data class Success(val data: String) : ExecutionResult()
        data class Error(val message: String) : ExecutionResult()
        object Timeout : ExecutionResult()
    }
    
    // ==================== 书源专用执行结果 ====================
    
    data class ExecuteResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null,
        val executionTime: Long = 0
    )
    
    // ==================== 通用执行方法 ====================
    
    fun execute(
        jsCode: String,
        functionName: String? = null,
        policy: SecurityPolicy = SecurityPolicy.standard(),
        vararg args: Any
    ): ExecutionResult {
        val future = executor.submit(Callable<String> {
            val cx = Context.enter()
            try {
                configureContext(cx, policy)
                
                val scope = cx.initStandardObjects()
                
                // 注入安全扩展
                val extensions = SafeJsExtensions(policy)
                ScriptableObject.putProperty(scope, "java", Context.javaToJS(extensions, scope))
                
                // 执行代码
                cx.evaluateString(scope, jsCode, "bookSource", 1, null)
                
                // 调用函数
                if (functionName != null) {
                    val func = scope.get(functionName, scope)
                    if (func is Function) {
                        val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
                        val result = func.call(cx, scope, scope, jsArgs)
                        return@Callable Context.toString(result)
                    }
                }
                return@Callable "OK"
            } catch (e: JavaScriptException) {
                "ERROR: ${e.message}"
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            } finally {
                Context.exit()
            }
        })
        
        return try {
            val result = future.get(policy.timeoutMs, TimeUnit.MILLISECONDS)
            when {
                result.startsWith("ERROR:") -> ExecutionResult.Error(result.substringAfter("ERROR:"))
                result == "TIMEOUT" -> ExecutionResult.Timeout
                else -> ExecutionResult.Success(result)
            }
        } catch (e: TimeoutException) {
            future.cancel(true)
            ExecutionResult.Timeout
        } catch (e: Exception) {
            ExecutionResult.Error(e.message ?: "未知错误")
        }
    }
    
    // ==================== 书源专用方法 ====================
    
    fun executeSearch(
        source: BookSource,
        keyword: String,
        timeoutMs: Long = 10000
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val jsCode = buildString {
                appendLine(source.headerJs ?: "")
                appendLine("var key = \"${escapeJsString(keyword)}\";")
                appendLine("var result = ${source.ruleSearch ?: "null"};")
                appendLine("JSON.stringify(result);")
            }
            
            val result = executeInternal(jsCode, source, timeoutMs)
            
            ExecuteResult(
                success = result.success,
                result = result.result,
                error = result.error,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ExecuteResult(
                success = false,
                result = null,
                error = "${e::class.simpleName}: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    fun executeBookInfo(
        source: BookSource,
        book: Book,
        timeoutMs: Long = 10000
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val jsCode = buildString {
                appendLine(source.headerJs ?: "")
                appendLine("var bookUrl = \"${escapeJsString(book.bookUrl)}\";")
                appendLine("var result = ${source.ruleBookInfo ?: "null"};")
                appendLine("JSON.stringify(result);")
            }
            
            val result = executeInternal(jsCode, source, timeoutMs)
            
            ExecuteResult(
                success = result.success,
                result = result.result,
                error = result.error,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ExecuteResult(
                success = false,
                result = null,
                error = "${e::class.simpleName}: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    fun executeToc(
        source: BookSource,
        book: Book,
        timeoutMs: Long = 15000
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val jsCode = buildString {
                appendLine(source.headerJs ?: "")
                appendLine("var bookUrl = \"${escapeJsString(book.bookUrl)}\";")
                appendLine("var result = ${source.ruleToc ?: "null"};")
                appendLine("JSON.stringify(result);")
            }
            
            val result = executeInternal(jsCode, source, timeoutMs)
            
            ExecuteResult(
                success = result.success,
                result = result.result,
                error = result.error,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ExecuteResult(
                success = false,
                result = null,
                error = "${e::class.simpleName}: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    fun executeContent(
        source: BookSource,
        book: Book,
        chapter: BookChapter,
        timeoutMs: Long = 15000
    ): ExecuteResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val jsCode = buildString {
                appendLine(source.headerJs ?: "")
                appendLine("var chapterUrl = \"${escapeJsString(chapter.url)}\";")
                appendLine("var result = ${source.ruleContent ?: "null"};")
                appendLine("JSON.stringify(result);")
            }
            
            val result = executeInternal(jsCode, source, timeoutMs)
            
            ExecuteResult(
                success = result.success,
                result = result.result,
                error = result.error,
                executionTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ExecuteResult(
                success = false,
                result = null,
                error = "${e::class.simpleName}: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    // ==================== 内部执行方法 ====================
    
    private fun executeInternal(
        jsCode: String,
        source: BookSource,
        timeoutMs: Long
    ): ExecuteResult {
        val future = executor.submit<ExecuteResult> {
            val cx = Context.enter()
            try {
                // 根据书源设置安全策略
                val policy = when (source.securityRating) {
                    1, 2 -> SecurityPolicy.trusted()
                    3, 4 -> SecurityPolicy.compatible()
                    else -> SecurityPolicy.standard()
                }
                
                configureContext(cx, policy)
                
                val scope = cx.initStandardObjects()
                
                // 注入安全扩展
                val extensions = SafeJsExtensions(policy)
                ScriptableObject.putProperty(scope, "java", Context.javaToJS(extensions, scope))
                
                // 注入书源对象
                ScriptableObject.putProperty(scope, "source", Context.javaToJS(source, scope))
                
                // 执行代码
                cx.evaluateString(scope, jsCode, "bookSource", 1, null)
                
                // 获取结果变量
                val resultObj = scope.get("result", scope)
                
                ExecuteResult(
                    success = true,
                    result = Context.jsToJava(resultObj, Any::class.java)
                )
                
            } catch (e: JavaScriptException) {
                ExecuteResult(false, null, "JS错误: ${e.message}")
            } catch (e: Exception) {
                ExecuteResult(false, null, "${e::class.simpleName}: ${e.message}")
            } finally {
                Context.exit()
            }
        }
        
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            ExecuteResult(false, null, "执行超时")
        } catch (e: Exception) {
            ExecuteResult(false, null, e.message)
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private fun configureContext(cx: Context, policy: SecurityPolicy) {
        cx.optimizationLevel = -1
        cx.languageVersion = Context.VERSION_1_8
        cx.maximumInterpreterStackDepth = policy.maxStackDepth
        
        cx.setInstructionObserverThreshold(1000000)
        cx.generateObserverCount = true
        
        // ClassShutter: 根据策略限制可访问的 Java 类
        cx.classShutter = ClassShutter { className ->
            when {
                className.startsWith("java.lang.Runtime") -> false
                className.startsWith("java.lang.ProcessBuilder") -> false
                className.startsWith("java.lang.ClassLoader") -> false
                className.startsWith("sun.misc.") -> false
                className.startsWith("java.lang.reflect.") -> policy.allowReflection
                className.startsWith("java.io.File") -> policy.allowFile
                className.startsWith("java.nio.file.") -> policy.allowFile
                className.startsWith("java.net.Socket") -> policy.allowSocket
                className.startsWith("java.net.ServerSocket") -> false
                className.startsWith("java.lang.Class") -> policy.allowReflection
                else -> true
            }
        }
    }
    
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
    
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}
