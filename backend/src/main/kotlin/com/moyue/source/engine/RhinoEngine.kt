package com.moyue.source.engine

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.source.JsExtensions
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
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
    
    data class ExecuteResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null,
        val executionTime: Long = 0
    )
    
    fun execute(
        jsCode: String,
        functionName: String? = null,
        source: BookSource? = null,
        book: Book? = null,
        chapter: BookChapter? = null,
        vararg args: Any
    ): ExecuteResult {
        val future = executor.submit<ExecuteResult> {
            val cx = Context.enter()
            try {
                // 设置上下文
                jsExtensions.setContext(JsExtensions.ExecutionContext(
                    source = source,
                    book = book,
                    chapter = chapter
                ))
                
                configureContext(cx)
                
                val scope = cx.initStandardObjects()
                
                // 注入 java 对象
                val jsJava = Context.javaToJS(jsExtensions, scope)
                ScriptableObject.putProperty(scope, "java", jsJava)
                
                // 注入其他对象
                source?.let {
                    ScriptableObject.putProperty(scope, "source", Context.javaToJS(it, scope))
                }
                book?.let {
                    ScriptableObject.putProperty(scope, "book", Context.javaToJS(it, scope))
                }
                chapter?.let {
                    ScriptableObject.putProperty(scope, "chapter", Context.javaToJS(it, scope))
                }
                
                // 执行代码
                cx.evaluateString(scope, jsCode, "bookSource", 1, null)
                
                // 调用函数
                if (functionName != null) {
                    val func = scope.get(functionName, scope)
                    if (func is Function) {
                        val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
                        val result = func.call(cx, scope, scope, jsArgs)
                        ExecuteResult(
                            success = true,
                            result = Context.jsToJava(result, Any::class.java)
                        )
                    } else {
                        ExecuteResult(false, null, "函数不存在: $functionName")
                    }
                } else {
                    ExecuteResult(true, "OK")
                }
                
            } catch (e: Exception) {
                ExecuteResult(false, null, "${e::class.simpleName}: ${e.message}")
            } finally {
                Context.exit()
                jsExtensions.clearContext()
            }
        }
        
        return try {
            future.get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            ExecuteResult(false, null, "执行超时")
        } catch (e: Exception) {
            ExecuteResult(false, null, e.message)
        }
    }
    
    private fun configureContext(cx: Context) {
        cx.optimizationLevel = -1
        cx.languageVersion = Context.VERSION_1_8
        cx.maximumInterpreterStackDepth = 10000
        
        cx.setInstructionObserverThreshold(1000000)
        cx.generateObserverCount = true
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
