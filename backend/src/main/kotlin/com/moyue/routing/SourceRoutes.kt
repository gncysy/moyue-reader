package com.moyue.routing
 
import com.moyue.service.SourceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
 
/**
 * 书源路由
 * 替代原 Spring Boot 的 SourceController
 */
fun Route.sourceRoutes() {
    val logger = KotlinLogging.logger {}
    val sourceService: SourceService by inject()
    
    route("/api/sources") {
        
        /**
         * 获取所有书源
         * GET /api/sources
         */
        get {
            val enabledOnly = call.request.queryParameters["enabled"]?.toBoolean() ?: false
            
            val sources = if (enabledOnly) {
                sourceService.getEnabledSources()
            } else {
                sourceService.getAllSources()
            }
            
            call.respond(mapOf(
                "success" to true,
                "sources" to sources,
                "total" to sources.size
            ))
        }
        
        /**
         * 获取单个书源详情
         * GET /api/sources/{id}
         */
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书源 ID 不能为空")
            )
            
            try {
                val source = sourceService.getSourceById(id)
                call.respond(mapOf(
                    "success" to true,
                    "source" to source
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书源不存在"
                ))
            }
        }
        
        /**
         * 添加书源
         * POST /api/sources
         */
        post {
            try {
                val source = call.receive<BookSource>()
                val saved = sourceService.saveSource(source)
                call.respond(HttpStatusCode.Created, mapOf(
                    "success" to true,
                    "source" to saved
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to e.message
                ))
            }
        }
        
        /**
         * 更新书源
         * PUT /api/sources/{id}
         */
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书源 ID 不能为空")
            )
            
            try {
                val source = call.receive<BookSource>()
                val updated = sourceService.saveSource(source.copy(id = id))
                call.respond(mapOf(
                    "success" to true,
                    "source" to updated
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书源不存在"
                ))
            }
        }
        
        /**
         * 删除书源
         * DELETE /api/sources/{id}
         */
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书源 ID 不能为空")
            )
            
            try {
                if (sourceService.deleteSource(id)) {
                    call.respond(mapOf(
                        "success" to true,
                        "message" to "书源删除成功"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf(
                        "success" to false,
                        "message" to "书源不存在"
                    ))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to e.message
                ))
            }
        }
        
        /**
         * 启用/禁用书源
         * PATCH /api/sources/{id}/toggle
         */
        patch("/{id}/toggle") {
            val id = call.parameters["id"] ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书源 ID 不能为空")
            )
            
            try {
                val source = sourceService.toggleSource(id)
                call.respond(mapOf(
                    "success" to true,
                    "source" to source
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书源不存在"
                ))
            }
        }
    }
}
