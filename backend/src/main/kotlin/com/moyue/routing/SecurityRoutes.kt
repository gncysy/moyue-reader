package com.moyue.routing
 
import com.moyue.service.SecurityService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
 
/**
 * 安全路由
 * 提供安全级别管理、规则验证等功能
 */
fun Route.securityRoutes() {
    val logger = KotlinLogging.logger {}
    val securityService: SecurityService by inject()
    
    route("/api/security") {
        
        /**
         * 获取安全统计
         * GET /api/security/stats
         */
        get("/stats") {
            call.respond(mapOf(
                "success" to true,
                "stats" to securityService.getSecurityStats()
            ))
        }
        
        /**
         * 设置默认安全级别
         * POST /api/security/level
         */
        post("/level") {
            try {
                val request = call.receive<Map<String, String>>()
                val level = request["level"] ?: "standard"
                
                val securityLevel = when (level) {
                    "trusted" -> com.moyue.security.SecurityLevel.TRUSTED
                    "compatible" -> com.moyue.security.SecurityLevel.COMPATIBLE
                    else -> com.moyue.security.SecurityLevel.STANDARD
                }
                
                securityService.setDefaultSecurityLevel(securityLevel)
                
                call.respond(mapOf(
                    "success" to true,
                    "message" to "安全级别已设置为: $level"
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to e.message
                ))
            }
        }
        
        /**
         * 验证书源规则
         * POST /api/security/validate-rule
         */
        post("/validate-rule") {
            try {
                val request = call.receive<Map<String, String>>()
                val rule = request["rule"] ?: ""
                
                val isValid = securityService.validateRule(rule)
                
                call.respond(mapOf(
                    "success" to true,
                    "valid" to isValid,
                    "message" to if (isValid) "规则安全" else "规则包含危险操作"
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to e.message
                ))
            }
        }
    }
}
