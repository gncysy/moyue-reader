package com.moyue.config

import com.moyue.engine.RhinoEngine
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.TimeUnit

/**
 * 应用配置类
 * 
 * 配置懒加载 Bean、异步线程池、OkHttp 客户端等核心组件
 * 支持通过 application.yml 自定义配置参数
 */
@Configuration
@EnableAsync
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
    
    @Value("\${okhttp.logging.enabled:true}")
    private val loggingEnabled: Boolean = true
    
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
    
    // ==================== Bean 定义 ====================
    
    /**
     * 懒加载 Rhino 引擎
     * 
     * 使用 @Lazy 延迟初始化，减少应用启动时间
     * Rhino 引擎较重，只有当第一次执行脚本时才会初始化
     */
    @Bean
    @Lazy
    fun rhinoEngine(): RhinoEngine {
        logger.info("初始化 Rhino 引擎...")
        return RhinoEngine()
    }
    
    /**
     * OkHttp 客户端
     * 
     * 用于所有 HTTP 请求，支持：
     * - 连接池复用
     * - 自动重试
     * - 请求/响应日志
     * - 代理支持
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
     * 异步任务线程池
     * 
     * 用于执行异步任务，如：
     * - 并发搜索多个书源
     * - 缓存预热
     * - 后台任务
     */
    @Bean
    fun taskExecutor(): AsyncTaskExecutor {
        logger.info("初始化异步任务线程池...")
        
        return ThreadPoolTaskExecutor().apply {
            // 核心线程数（常驻线程）
            corePoolSize = asyncCorePoolSize
            
            // 最大线程数（任务队列满后创建）
            maxPoolSize = asyncMaxPoolSize
            
            // 任务队列容量
            queueCapacity = asyncQueueCapacity
            
            // 线程名前缀
            threadNamePrefix = "async-"
            
            // 拒绝策略：由调用线程执行
            setRejectedExecutionHandler(java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy())
            
            // 线程空闲时间（60秒后销毁多余线程）
            setKeepAliveSeconds(60)
            
            // 允许核心线程超时
            setAllowCoreThreadTimeOut(true)
            
            // 初始化
            initialize()
            
            logger.info(
                "线程池配置: 核心线程数=$corePoolSize, " +
                "最大线程数=$maxPoolSize, " +
                "队列容量=$queueCapacity"
            )
        }
    }
}
