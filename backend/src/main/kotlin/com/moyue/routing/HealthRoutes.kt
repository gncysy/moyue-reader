package com.moyue.routing
 
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
 
/**
 * 健康检查路由
 * 替代原 Spring Boot 的 HealthController
 */
fun Route.healthRoutes() {
    
    route("/api/health") {
        
        /**
         * 基础健康检查
         * GET /api/health
         */
        get {
            call.respond(mapOf(
                "status" to "UP",
                "timestamp" to LocalDateTime.now().toString(),
                "service" to "moyue-backend"
            ))
        }
        
        /**
         * 详细健康检查
         * GET /api/health/detail
         */
        get("/detail") {
            // TODO: 检查数据库、缓存、外部服务等
            call.respond(mapOf(
                "status" to "UP",
                "timestamp" to LocalDateTime.now().toString(),
                "service" to "moyue-backend",
                "components" to mapOf(
                    "database" to "UP",
                    "cache" to "UP",
                    "rhino" to "UP"
                )
            ))
        }
    }
}
