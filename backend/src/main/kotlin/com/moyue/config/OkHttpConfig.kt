package com.moyue.config
 
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit
 
/**
 * OkHttp 客户端配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
class OkHttpConfig {
    
    private val logger = LoggerFactory.getLogger(OkHttpConfig::class.java)
    
    @Value("\${moyue.okhttp.connect-timeout:10}")
    private var connectTimeout: Long = 10
    
    @Value("\${moyue.okhttp.read-timeout:30}")
    private var readTimeout: Long = 30
    
    @Value("\${moyue.okhttp.write-timeout:30}")
    private var writeTimeout: Long = 30
    
    @Value("\${moyue.okhttp.call-timeout:60}")
    private var callTimeout: Long = 60
    
    @Value("\${moyue.okhttp.max-idle-connections:5}")
    private var maxIdleConnections: Int = 5
    
    @Value("\${moyue.okhttp.keep-alive-duration:300}")
    private var keepAliveDuration: Long = 300
    
    @Value("\${moyue.okhttp.follow-redirects:true}")
    private var followRedirects: Boolean = true
    
    @Value("\${moyue.okhttp.follow-ssl-redirects:true}")
    private var followSslRedirects: Boolean = true
    
    @Value("\${moyue.okhttp.retry-on-connection-failure:true}")
    private var retryOnConnectionFailure: Boolean = true
    
    /**
     * 配置 OkHttpClient
     */
    @Bean
    fun okHttpClient(): OkHttpClient {
        logger.info("配置 OkHttp 客户端")
        
        return OkHttpClient.Builder()
            // 超时配置
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .callTimeout(callTimeout, TimeUnit.SECONDS)
            
            // 连接池配置
            .connectionPool(
                ConnectionPool(
                    maxIdleConnections,
                    keepAliveDuration,
                    TimeUnit.SECONDS
                )
            )
            
            // 重定向配置
            .followRedirects(followRedirects)
            .followSslRedirects(followSslRedirects)
            
            // 重试配置
            .retryOnConnectionFailure(retryOnConnectionFailure)
            
            // 协议配置
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            
            // 拦截器配置
            .addInterceptor(LoggingInterceptor())
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            
            // 事件监听器
            .eventListener(OkHttpEventListener())
            
            .build()
    }
    
    /**
     * 日志拦截器
     */
    private class LoggingInterceptor : okhttp3.Interceptor {
        private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)
        
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            
            logger.debug("HTTP Request: ${request.method} ${request.url}")
            
            val startTime = System.nanoTime()
            
            val response = try {
                chain.proceed(request)
            } catch (e: Exception) {
                logger.error("HTTP Request Failed: ${request.method} ${request.url}", e)
                throw e
            }
            
            val endTime = System.nanoTime()
            val duration = (endTime - startTime) / 1_000_000.0  // 毫秒
            
            logger.debug(
                "HTTP Response: ${response.code} ${request.url} - ${String.format("%.2fms", duration)}"
            )
            
            return response
        }
    }
    
    /**
     * User-Agent 拦截器
     */
    private class UserAgentInterceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .build()
            
            return chain.proceed(request)
        }
    }
    
    /**
     * 重试拦截器
     */
    private class RetryInterceptor(
        private val maxRetries: Int = 3
    ) : okhttp3.Interceptor {
        private val logger = LoggerFactory.getLogger(RetryInterceptor::class.java)
        
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            
            var lastException: Exception? = null
            
            repeat(maxRetries) { attempt ->
                try {
                    val response = chain.proceed(request)
                    
                    if (response.isSuccessful) {
                        return response
                    }
                    
                    logger.warn(
                        "HTTP Request Failed (attempt ${attempt + 1}/$maxRetries): " +
                        "${request.method} ${request.url} - ${response.code}"
                    )
                    
                    response.close()
                } catch (e: Exception) {
                    lastException = e
                    logger.warn(
                        "HTTP Request Exception (attempt ${attempt + 1}/$maxRetries): " +
                        "${request.method} ${request.url} - ${e.message}"
                    )
                }
            }
            
            throw lastException ?: RuntimeException("请求失败: ${request.url}")
        }
    }
    
    /**
     * 事件监听器
     */
    private class OkHttpEventListener : okhttp3.EventListener() {
        private val logger = LoggerFactory.getLogger(OkHttpEventListener::class.java)
        
        override fun dnsStart(call: okhttp3.Call, domainName: String) {
            logger.debug("DNS Start: $domainName")
        }
        
        override fun dnsEnd(call: okhttp3.Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
            logger.debug("DNS End: $domainName - ${inetAddressList.size} addresses")
        }
        
        override fun connectStart(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy?) {
            logger.debug("Connect Start: $inetSocketAddress")
        }
        
        override fun connectEnd(
            call: okhttp3.Call,
            inetSocketAddress: java.net.InetSocketAddress,
            proxy: java.net.Proxy?,
            protocol: okhttp3.Protocol?
        ) {
            logger.debug("Connect End: $inetSocketAddress - $protocol")
        }
        
        override fun connectionAcquired(call: okhttp3.Call, connection: okhttp3.Connection) {
            logger.debug("Connection Acquired")
        }
        
        override fun connectionReleased(call: okhttp3.Call, connection: okhttp3.Connection) {
            logger.debug("Connection Released")
        }
    }
}
