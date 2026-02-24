package com.moyue.config
 
import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
 
/**
 * 安全配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {
    
    @Value("\${moyue.security.enabled:true}")
    private var securityEnabled: Boolean = true
    
    @Value("\${moyue.security.jwt.enabled:true}")
    private var jwtEnabled: Boolean = true
    
    @Value("\${moyue.security.cors.enabled:true}")
    private var corsEnabled: Boolean = true
    
    @Value("\${moyue.security.default-level:standard}")
    private lateinit var defaultSecurityLevel: String
    
    /**
     * 配置安全策略
     */
    @Bean
    fun securityPolicy(): SecurityPolicy {
        val level = SecurityLevel.fromName(defaultSecurityLevel)
        return SecurityPolicy.forLevel(level)
    }
    
    /**
     * 配置密码编码器
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    
    /**
     * 配置安全过滤器链
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return if (securityEnabled) {
            http
                // 禁用 CSRF
                .csrf { it.disable() }
                
                // 配置 CORS
                .cors { 
                    if (corsEnabled) {
                        it.configurationSource(corsConfigurationSource())
                    } else {
                        it.disable()
                    }
                }
                
                // 配置会话管理
                .sessionManagement { 
                    it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                }
                
                // 配置授权规则
                .authorizeHttpRequests { auth ->
                    // 公开端点
                    auth
                        .requestMatchers(
                            "/api/health/**",
                            "/api/security/login",
                            "/api/security/register"
                        ).permitAll()
                        
                        // 调试端点（仅开发环境）
                        .requestMatchers("/api/debug/**").hasRole("ADMIN")
                        
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                }
                
                // 配置异常处理
                .exceptionHandling { 
                    it.authenticationEntryPoint { request, response, authException ->
                        response.status = 401
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("""{"success":false,"code":"UNAUTHORIZED","message":"${authException.message}"}""")
                    }
                    it.accessDeniedHandler { request, response, accessDeniedException ->
                        response.status = 403
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("""{"success":false,"code":"FORBIDDEN","message":"${accessDeniedException.message}"}""")
                    }
                }
                
                // 配置 HTTP 基本认证
                .httpBasic { 
                    if (jwtEnabled) {
                        it.disable()
                    } else {
                        it.realmName("Moyue Reader")
                    }
                }
                
                .build()
        } else {
            // 禁用安全
            http
                .csrf { it.disable() }
                .cors { it.disable() }
                .sessionManagement { 
                    it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                }
                .authorizeHttpRequests {
                    it.anyRequest().permitAll()
                }
                .build()
        }
    }
    
    /**
     * 配置 CORS
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("*")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
