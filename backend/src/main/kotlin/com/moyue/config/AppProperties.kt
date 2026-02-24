package com.moyue.config
 
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
 
/**
 * 应用属性配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Component
@ConfigurationProperties(prefix = "moyue")
data class AppProperties(
    /**
     * 应用信息
     */
    val app: App = App(),
    
    /**
     * 安全配置
     */
    val security: Security = Security(),
    
    /**
     * 缓存配置
     */
    val cache: Cache = Cache(),
    
    /**
     * 书源配置
     */
    val bookSource: BookSource = BookSource(),
    
    /**
     * 性能配置
     */
    val performance: Performance = Performance(),
    
    /**
     * 日志配置
     */
    val logging: Logging = Logging(),
    
    /**
     * OkHttp 配置
     */
    val okhttp: OkHttp = OkHttp(),
    
    /**
     * 异步配置
     */
    val async: Async = Async(),
    
    /**
     * Swagger 配置
     */
    val swagger: Swagger = Swagger()
) {
    
    // ==================== 应用配置 ====================
    
    data class App(
        val name: String = "Moyue Reader",
        val version: String = "0.1.0",
        val description: String = "Moyue Reader - 开源阅读器"
    )
    
    // ==================== 安全配置 ====================
    
    data class Security(
        val enabled: Boolean = true,
        val jwt: Jwt = Jwt(),
        val cors: Cors = Cors(),
        val defaultLevel: String = "standard"
    ) {
        data class Jwt(
            val enabled: Boolean = true,
            val secret: String = "",
            val expiration: Long = 86400,
            val refreshTokenExpiration: Long = 604800
        )
        
        data class Cors(
            val enabled: Boolean = true,
            val allowedOrigins: List<String> = listOf("*"),
            val allowedMethods: List<String> = listOf("*"),
            val allowedHeaders: List<String> = listOf("*"),
            val allowCredentials: Boolean = true,
            val maxAge: Long = 3600
        )
    }
    
    // ==================== 缓存配置 ====================
    
    data class Cache(
        val enabled: Boolean = true,
        val defaultTtl: Long = 3600,
        val maxSize: Long = 10000,
        val initialCapacity: Int = 100
    )
    
    // ==================== 书源配置 ====================
    
    data class BookSource(
        val maxConcurrentSearch: Int = 5,
        val requestTimeout: Int = 10,
        val searchTimeout: Int = 30,
        val maxRetries: Int = 3,
        val enableCache: Boolean = true,
        val cacheTtl: Long = 3600
    )
    
    // ==================== 性能配置 ====================
    
    data class Performance(
        val enableCompression: Boolean = true,
        val enableGzip: Boolean = true,
        val minResponseSize: Int = 1024,
        val maxConnections: Int = 100
    )
    
    // ==================== 日志配置 ====================
    
    data class Logging(
        val level: String = "INFO",
        val request: Request = Request()
    ) {
        data class Request(
            val enabled: Boolean = true,
            val maxPayloadLength: Int = 1000,
            val includeClientInfo: Boolean = true,
            val includeQueryString: Boolean = true,
            val includePayload: Boolean = true,
            val includeHeaders: Boolean = false
        )
    }
    
    // ==================== OkHttp 配置 ====================
    
    data class OkHttp(
        val connectTimeout: Long = 10,
        val readTimeout: Long = 30,
        val writeTimeout: Long = 30,
        val callTimeout: Long = 60,
        val maxIdleConnections: Int = 5,
        val keepAliveDuration: Long = 300,
        val followRedirects: Boolean = true,
        val followSslRedirects: Boolean = true,
        val retryOnConnectionFailure: Boolean = true
    )
    
    // ==================== 异步配置 ====================
    
    data class Async(
        val corePoolSize: Int = 8,
        val maxPoolSize: Int = 20,
        val queueCapacity: Int = 1000,
        val threadNamePrefix: String = "async-",
        val keepAliveSeconds: Int = 60,
        val allowCoreThreadTimeout: Boolean = true,
        val waitForTasksToCompleteOnShutdown: Boolean = true,
        val awaitTerminationSeconds: Int = 60
    )
    
    // ==================== Swagger 配置 ====================
    
    data class Swagger(
        val enabled: Boolean = true,
        val title: String = "Moyue Reader API",
        val description: String = "Moyue Reader 后端 API 文档",
        val version: String = "0.1.0",
        val contact: Contact = Contact(),
        val license: License = License()
    ) {
        data class Contact(
            val name: String = "Moyue Team",
            val email: String = "",
            val url: String = ""
        )
        
        data class License(
            val name: String = "MIT",
            val url: String = ""
        )
    }
}
