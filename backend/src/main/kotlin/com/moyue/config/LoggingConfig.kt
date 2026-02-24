package com.moyue.config
 
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.nio.charset.StandardCharsets
 
/**
 * 日志配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
class LoggingConfig {
    
    private val logger = LoggerFactory.getLogger(LoggingConfig::class.java)
    
    @Value("\${moyue.logging.request.enabled:true}")
    private var requestLoggingEnabled: Boolean = true
    
    @Value("\${moyue.logging.request.max-payload-length:1000}")
    private var maxPayloadLength: Int = 1000
    
    @Value("\${moyue.logging.request.include-client-info:true}")
    private var includeClientInfo: Boolean = true
    
    @Value("\${moyue.logging.request.include-query-string:true}")
    private var includeQueryString: Boolean = true
    
    @Value("\${moyue.logging.request.include-payload:true}")
    private var includePayload: Boolean = true
    
    @Value("\${moyue.logging.request.include-headers:false}")
    private var includeHeaders: Boolean = false
    
    /**
     * 配置请求日志过滤器
     */
    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        return CommonsRequestLoggingFilter().apply {
            setIncludeClientInfo(includeClientInfo)
            setIncludeQueryString(includeQueryString)
            setIncludePayload(includePayload)
            setIncludeHeaders(includeHeaders)
            maxPayloadLength = this@LoggingConfig.maxPayloadLength
            setAfterMessagePrefix("Request: ")
        }
    }
    
    /**
     * 自定义日志记录器
     */
    object AppLogger {
        /**
         * 获取日志记录器
         */
        fun getLogger(clazz: Class<*>): Logger {
            return LoggerFactory.getLogger(clazz)
        }
        
        /**
         * 记录 API 请求
         */
        fun logApiRequest(
            method: String,
            path: String,
            params: Map<String, Any>? = null,
            headers: Map<String, String>? = null,
            body: Any? = null
        ) {
            val logger = getLogger(LoggingConfig::class.java)
            
            val logMessage = buildString {
                append("API Request: $method $path")
                
                if (!params.isNullOrEmpty()) {
                    append(", params=$params")
                }
                
                if (!headers.isNullOrEmpty()) {
                    append(", headers=$headers")
                }
                
                if (body != null) {
                    val bodyStr = if (body is String) {
                        if (body.length > maxPayloadLength) {
                            body.substring(0, maxPayloadLength) + "..."
                        } else {
                            body
                        }
                    } else {
                        body.toString()
                    }
                    append(", body=$bodyStr")
                }
            }
            
            logger.info(logMessage)
        }
        
        /**
         * 记录 API 响应
         */
        fun logApiResponse(
            method: String,
            path: String,
            status: Int,
            duration: Long,
            body: Any? = null
        ) {
            val logger = getLogger(LoggingConfig::class.java)
            
            val logMessage = buildString {
                append("API Response: $method $path - $status (${duration}ms)")
                
                if (body != null) {
                    val bodyStr = if (body is String) {
                        if (body.length > maxPayloadLength) {
                            body.substring(0, maxPayloadLength) + "..."
                        } else {
                            body
                        }
                    } else {
                        body.toString()
                    }
                    append(", body=$bodyStr")
                }
            }
            
            logger.info(logMessage)
        }
        
        /**
         * 记录错误
         */
        fun logError(
            message: String,
            throwable: Throwable? = null,
            context: Map<String, Any>? = null
        ) {
            val logger = getLogger(LoggingConfig::class.java)
            
            val logMessage = buildString {
                append("Error: $message")
                
                if (!context.isNullOrEmpty()) {
                    append(", context=$context")
                }
            }
            
            if (throwable != null) {
                logger.error(logMessage, throwable)
            } else {
                logger.error(logMessage)
            }
        }
        
        /**
         * 记录警告
         */
        fun logWarning(
            message: String,
            context: Map<String, Any>? = null
        ) {
            val logger = getLogger(LoggingConfig::class.java)
            
            val logMessage = buildString {
                append("Warning: $message")
                
                if (!context.isNullOrEmpty()) {
                    append(", context=$context")
                }
            }
            
            logger.warn(logMessage)
        }
        
        /**
         * 记录性能指标
         */
        fun logPerformance(
            operation: String,
            duration: Long,
            context: Map<String, Any>? = null
        ) {
            val logger = getLogger(LoggingConfig::class.java)
            
            val logMessage = buildString {
                append("Performance: $operation took ${duration}ms")
                
                if (!context.isNullOrEmpty()) {
                    append(", context=$context")
                }
            }
            
            logger.info(logMessage)
        }
    }
}
