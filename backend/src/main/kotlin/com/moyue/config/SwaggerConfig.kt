package com.moyue.config
 
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
 
/**
 * Swagger/OpenAPI 配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
class SwaggerConfig {
    
    @Value("\${moyue.swagger.enabled:true}")
    private var swaggerEnabled: Boolean = true
    
    @Value("\${moyue.swagger.title:Moyue Reader API}")
    private lateinit var title: String
    
    @Value("\${moyue.swagger.description:Moyue Reader 后端 API 文档}")
    private lateinit var description: String
    
    @Value("\${moyue.swagger.version:0.1.0}")
    private lateinit var version: String
    
    @Value("\${moyue.swagger.contact.name:Moyue Team}")
    private lateinit var contactName: String
    
    @Value("\${moyue.swagger.contact.email:}")
    private var contactEmail: String = ""
    
    @Value("\${moyue.swagger.contact.url:}")
    private var contactUrl: String = ""
    
    @Value("\${moyue.swagger.license.name:MIT}")
    private lateinit var licenseName: String
    
    @Value("\${moyue.swagger.license.url:}")
    private var licenseUrl: String = ""
    
    @Value("\${server.port:8080}")
    private var serverPort: Int = 8080
    
    /**
     * 配置 OpenAPI
     */
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            // 服务器配置
            .servers(
                listOf(
                    Server().url("http://localhost:$serverPort").description("本地服务器"),
                    Server().url("/").description("当前服务器")
                )
            )
            
            // 信息配置
            .info(
                Info()
                    .title(title)
                    .description(description)
                    .version(version)
                    .contact(
                        Contact()
                            .name(contactName)
                            .email(contactEmail.ifEmpty { null })
                            .url(contactUrl.ifEmpty { null })
                    )
                    .license(
                        License()
                            .name(licenseName)
                            .url(licenseUrl.ifEmpty { null })
                    )
            )
            
            // 组件配置
            .components(
                Components()
                    // 安全配置
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT 认证令牌")
                    )
                    // 响应配置
                    .addSchemas("ErrorResponse", Schema<Any>()
                        .type("object")
                        .addProperties("success", Schema<Boolean>())
                        .addProperties("code", Schema<String>())
                        .addProperties("message", Schema<String>())
                        .addProperties("details", Schema<Any>())
                        .addProperties("timestamp", Schema<String>())
                    )
                    .addSchemas("SuccessResponse", Schema<Any>()
                        .type("object")
                        .addProperties("success", Schema<Boolean>())
                        .addProperties("data", Schema<Any>())
                        .addProperties("message", Schema<String>())
                        .addProperties("timestamp", Schema<String>())
                    )
            )
            
            // 安全需求配置
            .addSecurityItem(
                SecurityRequirement().addList("bearerAuth")
            )
    }
}
