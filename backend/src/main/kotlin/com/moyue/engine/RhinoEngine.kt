package com.moyue.engine

import com.moyue.security.SafeJsExtensions
import com.moyue.security.SecurityPolicy
import org.mozilla.javascript.*
import org.slf4j.LoggerFactory
import java.util.concurrent.*

class RhinoEngine(private val policy: SecurityPolicy) {
    private val logger = LoggerFactory.getLogger(RhinoEngine::class.java)
    private val executor = ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, LinkedBlockingQueue(10))
    
    fun execute(jsCode: String, functionName: String? = null, vararg args: Any): ExecutionResult {
        val future = executor.submit(Callable<String> {
            val cx = Context.enter()
            try {
                configureContext(cx)
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
            ExecutionResult.Timeout
        } catch (e: Exception) {
            ExecutionResult.Error(e.message ?: "未知错误")
        }
    }
    
    private fun configureContext(cx: Context) {
        cx.optimizationLevel = -1
        cx.languageVersion = Context.VERSION_ES6
        cx.maximumInterpreterStackDepth = policy.maxStackDepth
        
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
                else -> true // 默认允许基础类（String、Integer等）
            }
        }
    }
    
    fun shutdown() {
        executor.shutdown()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}

sealed class ExecutionResult {
    data class Success(val data: String) : ExecutionResult()
    data class Error(val message: String) : ExecutionResult()
    object Timeout : ExecutionResult()
}
