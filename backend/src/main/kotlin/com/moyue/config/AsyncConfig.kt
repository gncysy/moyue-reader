package com.moyue.config
 
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
 
/**
 * 异步任务配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
@EnableAsync
class AsyncConfig {
    
    private val logger = LoggerFactory.getLogger(AsyncConfig::class.java)
    
    @Value("\${moyue.async.core-pool-size:8}")
    private var corePoolSize: Int = 8
    
    @Value("\${moyue.async.max-pool-size:20}")
    private var maxPoolSize: Int = 20
    
    @Value("\${moyue.async.queue-capacity:1000}")
    private var queueCapacity: Int = 1000
    
    @Value("\${moyue.async.thread-name-prefix:async-}")
    private var threadNamePrefix: String = "async-"
    
    @Value("\${moyue.async.keep-alive-seconds:60}")
    private var keepAliveSeconds: Int = 60
    
    @Value("\${moyue.async.allow-core-thread-timeout:true}")
    private var allowCoreThreadTimeout: Boolean = true
    
    @Value("\${moyue.async.wait-for-tasks-to-complete-on-shutdown:true}")
    private var waitForTasksToCompleteOnShutdown: Boolean = true
    
    @Value("\${moyue.async.await-termination-seconds:60}")
    private var awaitTerminationSeconds: Int = 60
    
    /**
     * 配置异步任务执行器
     */
    @Bean(name = ["asyncExecutor"])
    fun asyncExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            // 核心线程数
            corePoolSize = this@AsyncConfig.corePoolSize
            
            // 最大线程数
            maxPoolSize = this@AsyncConfig.maxPoolSize
            
            // 队列容量
            setQueueCapacity(this@AsyncConfig.queueCapacity)
            
            // 线程名称前缀
            threadNamePrefix = this@AsyncConfig.threadNamePrefix
            
            // 空闲线程存活时间
            setKeepAliveSeconds(this@AsyncConfig.keepAliveSeconds)
            
            // 核心线程超时
            setAllowCoreThreadTimeOut(this@AsyncConfig.allowCoreThreadTimeout)
            
            // 拒绝策略
            setRejectedExecutionHandler(CustomRejectedExecutionHandler())
            
            // 等待任务完成
            setWaitForTasksToCompleteOnShutdown(this@AsyncConfig.waitForTasksToCompleteOnShutdown)
            setAwaitTerminationSeconds(this@AsyncConfig.awaitTerminationSeconds)
            
            // 初始化
            initialize()
            
            logger.info("异步任务执行器初始化完成: core=$corePoolSize, max=$maxPoolSize, queue=$queueCapacity")
        }
    }
    
    /**
     * 自定义拒绝策略
     */
    private class CustomRejectedExecutionHandler : RejectedExecutionHandler {
        private val logger = LoggerFactory.getLogger(CustomRejectedExecutionHandler::class.java)
        
        override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
            logger.warn("任务被拒绝执行: ${r.javaClass.name}, pool=${executor.poolSize}, queue=${executor.queue.size}")
            
            try {
                // 尝试再次执行
                executor.execute(r)
            } catch (e: Exception) {
                logger.error("任务执行失败", e)
            }
        }
    }
}
