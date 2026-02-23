package com.moyue.config
 
import com.moyue.engine.RhinoEngine
import com.moyue.service.*
import okhttp3.OkHttpClient
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
 
/**
 * Koin 依赖注入模块
 * 替代 Spring Boot 的 @Component 和 @Bean
 */
val appModule = module {
    
    // ==================== 引擎 ====================
    
    single { 
        RhinoEngine() 
    }
    
    // ==================== OkHttp 客户端 ====================
    
    single {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    
    // ==================== 服务层 ====================
    
    single { 
        BookService(get(), get()) 
    }
    
    single { 
        SourceService(get(), get(), get()) 
    }
    
    single { 
        CacheService(get()) 
    }
    
    single { 
        PreferenceService() 
    }
    
    single { 
        SecurityService(get(), get()) 
    }
}
