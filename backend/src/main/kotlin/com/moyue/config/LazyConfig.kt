package com.moyue.config
 
import com.moyue.engine.RhinoEngine
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import java.util.concurrent.TimeUnit
 
/**
 * 应用配置类
 *
 * Spring Boot 4.0.3
 * 配置：懒加载 Bean、异步线程池、OkHttp 客户端、RestTemplate、缓存
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
@EnableAsync
@EnableCaching
class LazyConfig {
    
    private val logger = LoggerFactory.getLogger(LazyConfig::class.java)
    
    // ==================== 配置属性 ====================
    
    @Value("\${okhttp.connect-timeout:10}")
    private val connectTimeout: Int = 10
    
    @Value("\${okhttp.read-timeout:30}")
    private val readTimeout: Int = 30
    
    @Value("\${okhttp.write-timeout:10}")
    private val writeTimeout: Int = 10
    
    @Value("\${okhttp.connection-pool.max-idle:10}")
    private val maxIdleConnections: Int = 10
    
    @Value("\${okhttp.connection-pool.keep-alive:5}")
    private val keepAliveDuration: Int = 5
    
    @Value("\${okhttp.logging.enabled:false}")
    private val loggingEnabled: Boolean = false
    
    @Value("\${okhttp.logging.level:BASIC}")
    private val loggingLevel: String = "BASIC"
    
    @Value("\${async.core-pool-size:5}")
    private val asyncCorePoolSize: Int = 5
    
    @Value("\${async.max-pool-size:10}")
    private val asyncMaxPoolSize: Int = 10
    
    @Value("\${async.queue-capacity:100}")
    private val asyncQueueCapacity: Int = 100
    
    @Value("\${okhttp.proxy.enabled:false}")
    private val proxyEnabled: Boolean = false
    
    @Value("\${okhttp.proxy.host:}")
    private val proxyHost: String = ""
    
    @Value("\${okhttp.proxy.port:8080}")
    private val proxyPort: Int = 8080
    
    @Value("\${async.thread-name-prefix:async-}")
    private val threadNamePrefix: String = "async-"
    
    // ==================== Bean 定义 ====================
    
    /**
     * 懒加载 Rhino 引擎
     * 使用 @Lazy 延迟初始化，减少应用启动时间
     */
    @Bean
    @Lazy
    fun rhinoEngine(): RhinoEngine {
        logger.info("初始化 Rhino 引擎...")
        return RhinoEngine()
    }
    
    /**
     * OkHttp 客户端
     * 用于所有 HTTP 请求，支持连接池复用、自动重试、请求日志、代理
     */
    @Bean
    fun okHttpClient(): OkHttpClient {
        logger.info("初始化 OkHttp 客户端...")
        
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(readTimeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(writeTimeout.toLong(), TimeUnit.SECONDS)
            .connectionPool(
                ConnectionPool(
                    maxIdleConnections,
                    keepAliveDuration.toLong(),
                    TimeUnit.MINUTES
                )
            )
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .pingInterval(30, TimeUnit.SECONDS)  // 心跳检测
        
        // 配置日志拦截器
        if (loggingEnabled) {
            val logging = HttpLoggingInterceptor().apply {
                level = try {
                    HttpLoggingInterceptor.Level.valueOf(loggingLevel)
                } catch (e: IllegalArgumentException) {
                    HttpLoggingInterceptor.Level.BASIC
                }
            }
            builder.addInterceptor(logging)
            logger.info("已启用 OkHttp 日志，级别: $loggingLevel")
        }
        
        // 配置代理
        if (proxyEnabled && proxyHost.isNotEmpty()) {
            val proxy = java.net.Proxy(
                java.net.Proxy.Type.HTTP,
                java.net.InetSocketAddress(proxyHost, proxyPort)
            )
            builder.proxy(proxy)
            logger.info("已配置代理: $proxyHost:$proxyPort")
        }
        
        return builder.build()
    }
    
    /**
     * RestTemplate Bean
     * 用于 Spring MVC 场景下的 HTTP 调用
     */
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(java.time.Duration.ofSeconds(connectTimeout.toLong()))
            .setReadTimeout(java.time.Duration.ofSeconds(readTimeout.toLong()))
            .build()
    }
    
    /**
     * 异步任务线程池
     * 用于执行异步任务，如并发搜索、缓存预热、后台任务
     */
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): AsyncTaskExecutor {
        logger.info("初始化异步任务线程池...")
        
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = asyncCorePoolSize
            maxPoolSize = asyncMaxPoolSize
            queueCapacity = asyncQueueCapacity
            threadNamePrefix = threadNamePrefix
            setRejectedExecutionHandler(java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy())
            setKeepAliveSeconds(60)
            setAllowCoreThreadTimeOut(true)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(60)
            initialize()
            
            logger.info(
                "线程池配置: 核心线程数=$corePoolSize, " +
                "最大线程数=$maxPoolSize, " +
                "队列容量=$queueCapacity"
            )
        }
    }
}
