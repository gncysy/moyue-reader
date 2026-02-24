package com.moyue.config
 
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor
 
/**
 * 线程池配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
class ThreadPoolConfig {
    
    private val logger = LoggerFactory.getLogger(ThreadPoolConfig::class.java)
    
    // ==================== 书源搜索线程池 ====================
    
    @Value("\${moyue.thread-pool.search.core-size:5}")
    private var searchCoreSize: Int = 5
    
    @Value("\${moyue.thread-pool.search.max-size:10}")
    private var searchMaxSize: Int = 10
    
    @Value("\${moyue.thread-pool.search.queue-capacity:100}")
    private var searchQueueCapacity: Int = 100
    
    /**
     * 书源搜索线程池
     */
    @Bean(name = ["searchExecutor"])
    fun searchExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = this@ThreadPoolConfig.searchCoreSize
            maxPoolSize = this@ThreadPoolConfig.searchMaxSize
            setQueueCapacity(this@ThreadPoolConfig.searchQueueCapacity)
            threadNamePrefix = "search-"
            setKeepAliveSeconds(60)
            setAllowCoreThreadTimeOut(true)
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
            
            logger.info("搜索线程池初始化完成: core=$searchCoreSize, max=$searchMaxSize, queue=$searchQueueCapacity")
        }
    }
    
    // ==================== 网络请求线程池 ====================
    
    @Value("\${moyue.thread-pool.http.core-size:10}")
    private var httpCoreSize: Int = 10
    
    @Value("\${moyue.thread-pool.http.max-size:50}")
    private var httpMaxSize: Int = 50
    
    @Value("\${moyue.thread-pool.http.queue-capacity:500}")
    private var httpQueueCapacity: Int = 500
    
    /**
     * 网络请求线程池
     */
    @Bean(name = ["httpExecutor"])
    fun httpExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = this@ThreadPoolConfig.httpCoreSize
            maxPoolSize = this@ThreadPoolConfig.httpMaxSize
            setQueueCapacity(this@ThreadPoolConfig.httpQueueCapacity)
            threadNamePrefix = "http-"
            setKeepAliveSeconds(60)
            setAllowCoreThreadTimeOut(true)
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
            
            logger.info("HTTP 线程池初始化完成: core=$httpCoreSize, max=$httpMaxSize, queue=$httpQueueCapacity")
        }
    }
    
    // ==================== 缓存清理线程池 ====================
    
    @Value("\${moyue.thread-pool.cache.core-size:2}")
    private var cacheCoreSize: Int = 2
    
    @Value("\${moyue.thread-pool.cache.max-size:5}")
    private var cacheMaxSize: Int = 5
    
    @Value("\${moyue.thread-pool.cache.queue-capacity:50}")
    private var cacheQueueCapacity: Int = 50
    
    /**
     * 缓存清理线程池
     */
    @Bean(name = ["cacheExecutor"])
    fun cacheExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = this@ThreadPoolConfig.cacheCoreSize
            maxPoolSize = this@ThreadPoolConfig.cacheMaxSize
            setQueueCapacity(this@ThreadPoolConfig.cacheQueueCapacity)
            threadNamePrefix = "cache-"
            setKeepAliveSeconds(60)
            setAllowCoreThreadTimeOut(true)
            setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy())
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(10)
            initialize()
            
            logger.info("缓存线程池初始化完成: core=$cacheCoreSize, max=$cacheMaxSize, queue=$cacheQueueCapacity")
        }
    }
    
    // ==================== 定时任务线程池 ====================
    
    @Value("\${moyue.thread-pool.scheduler.core-size:3}")
    private var schedulerCoreSize: Int = 3
    
    @Value("\${moyue.thread-pool.scheduler.max-size:10}")
    private var schedulerMaxSize: Int = 10
    
    @Value("\${moyue.thread-pool.scheduler.queue-capacity:100}")
    private var schedulerQueueCapacity: Int = 100
    
    /**
     * 定时任务线程池
     */
    @Bean(name = ["schedulerExecutor"])
    fun schedulerExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = this@ThreadPoolConfig.schedulerCoreSize
            maxPoolSize = this@ThreadPoolConfig.schedulerMaxSize
            setQueueCapacity(this@ThreadPoolConfig.schedulerQueueCapacity)
            threadNamePrefix = "scheduler-"
            setKeepAliveSeconds(60)
            setAllowCoreThreadTimeOut(false)
            setRejectedExecutionHandler(ThreadPoolExecutor.AbortPolicy())
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(60)
            initialize()
            
            logger.info("定时任务线程池初始化完成: core=$schedulerCoreSize, max=$schedulerMaxSize, queue=$schedulerQueueCapacity")
        }
    }
}
