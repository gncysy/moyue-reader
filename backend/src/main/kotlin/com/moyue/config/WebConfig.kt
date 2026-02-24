package com.moyue.config
 
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.datatype.jsr310.JavaTimeModule
import tools.jackson.module.kotlin.KotlinModule
import java.nio.charset.StandardCharsets
 
/**
 * Web 配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 使用 Jackson 3.x (tools.jackson)
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
class WebConfig : WebMvcConfigurer {
    
    @Value("\${spring.web.resources.static-locations:classpath:/static/}")
    private lateinit var staticLocations: String
    
    @Value("\${spring.servlet.multipart.max-file-size:10MB}")
    private lateinit var maxFileSize: String
    
    @Value("\${spring.servlet.multipart.max-request-size:10MB}")
    private lateinit var maxRequestSize: String
    
    /**
     * 配置消息转换器
     */
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
        // JSON 转换器
        val jsonConverter = MappingJackson2HttpMessageConverter().apply {
            objectMapper = createObjectMapper()
            supportedMediaTypes = listOf(
                org.springframework.http.MediaType.APPLICATION_JSON,
                org.springframework.http.MediaType.APPLICATION_JSON_UTF8,
                org.springframework.http.MediaType.TEXT_PLAIN
            )
        }
        converters.add(0, jsonConverter)
        
        // 字符串转换器
        val stringConverter = StringHttpMessageConverter(StandardCharsets.UTF_8)
        converters.add(stringConverter)
    }
    
    /**
     * 创建 ObjectMapper
     */
    private fun createObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // 注册 Kotlin 模块
            registerModule(KotlinModule.Builder().build())
            
            // 注册 Java 时间模块
            registerModule(JavaTimeModule())
            
            // 自定义模块
            registerModule(SimpleModule())
            
            // 配置序列化
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            
            // 配置反序列化
            configure(tools.jackson.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(tools.jackson.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        }
    }
    
    /**
     * 配置 CORS
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
    
    /**
     * 配置静态资源处理
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 静态资源
        registry.addResourceHandler("/static/**")
            .addResourceLocations(*staticLocations.split(",").toTypedArray())
            .setCachePeriod(3600)
        
        // 上传的文件
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:./uploads/")
            .setCachePeriod(0)
        
        // 缓存文件
        registry.addResourceHandler("/cache/**")
            .addResourceLocations("file:./cache/")
            .setCachePeriod(0)
    }
    
    /**
     * 配置视图控制器
     */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        // 根路径重定向到健康检查
        registry.addRedirectViewController("/", "/api/health")
    }
    
    /**
     * 配置拦截器
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        // 日志拦截器
        // registry.addInterceptor(LoggingInterceptor())
        //     .addPathPatterns("/api/**")
        
        // 认证拦截器（如需要）
        // registry.addInterceptor(AuthInterceptor())
        //     .addPathPatterns("/api/**")
        //     .excludePathPatterns("/api/health/**", "/api/security/login")
    }
}
