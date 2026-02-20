package com.moyue.config

import com.moyue.engine.RhinoEngine
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.TimeUnit

@Configuration
@EnableAsync
class LazyConfig {
    
    /**
     * 懒加载 Rhino 引擎（第一次使用时才初始化）
     * 减少启动时间
     */
    @Bean
    @Lazy
    fun rhinoEngine(): RhinoEngine {
        return RhinoEngine()
    }
    
    /**
     * 优化连接池
     */
    @Bean
    fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    
    /**
     * 异步任务线程池
     */
    @Bean
    fun taskExecutor(): AsyncTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 20
            threadNamePrefix = "async-"
            initialize()
        }
    }
}
