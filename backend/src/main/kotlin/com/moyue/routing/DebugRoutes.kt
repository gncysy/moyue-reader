package com.moyue.routing
 
import com.moyue.service.CacheService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
 
/**
 * 调试路由
 * 提供系统状态、缓存统计等调试信息
 */
fun Route.debugRoutes() {
    val logger = KotlinLogging.logger {}
    val cacheService: CacheService by inject()
    
    route("/api/debug") {
        
        /**
         * 系统状态
         * GET /api/debug/status
         */
        get("/status") {
            call.respond(mapOf(
                "success" to true,
                "status" to "running",
                "timestamp" to System.currentTimeMillis(),
                "version" to "1.0.0"
            ))
        }
        
        /**
         * 缓存统计
         * GET /api/debug/cache
         */
        get("/cache") {
            call.respond(mapOf(
                "success" to true,
                "stats" to cacheService.getStats()
            ))
        }
        
        /**
         * 清空缓存
         * POST /api/debug/cache/clear
         */
        post("/cache/clear") {
            cacheService.clear()
            call.respond(mapOf(
                "success" to true,
                "message" to "缓存已清空"
            ))
        }
        
        /**
         * 清理过期缓存
         * POST /api/debug/cache/cleanup
         */
        post("/cache/cleanup") {
            cacheService.cleanupExpired()
            call.respond(mapOf(
                "success" to true,
                "message" to "过期缓存已清理"
            ))
        }
    }
}
