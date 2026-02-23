package com.moyue.config
 
import io.ktor.server.application.*
import io.ktor.server.routing.*
import mu.KotlinLogging
 
/**
 * 配置所有路由
 */
fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}
    
    routing {
        // 健康检查路由
        healthRoutes()
        
        // 书籍路由
        bookRoutes()
        
        // 书源路由
        sourceRoutes()
        
        // 安全路由
        securityRoutes()
        
        // 调试路由（仅开发环境）
        if (environment.config.property("ktor.environment").getString() == "dev") {
            debugRoutes()
        }
        
        // WebSocket 路由
        webSocketRoutes()
        
        logger.info { "路由配置完成" }
    }
}
