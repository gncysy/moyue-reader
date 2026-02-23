package com.moyue.routing
 
import com.moyue.service.BookService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
 
/**
 * 书籍路由
 * 替代原 Spring Boot 的 BookController
 */
fun Route.bookRoutes() {
    val logger = KotlinLogging.logger {}
    val bookService: BookService by inject() // Koin 注入
    
    route("/api/books") {
        
        /**
         * 获取书架所有书籍（分页）
         * GET /api/books?page=0&size=20
         */
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            
            logger.debug { "获取书籍列表: page=$page, size=$size" }
            
            val books = bookService.getAllBooks(page, size)
            call.respond(mapOf(
                "success" to true,
                "data" to books,
                "page" to page,
                "size" to size,
                "total" to books.size
            ))
        }
        
        /**
         * 获取单本书籍详情
         * GET /api/books/{id}
         */
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书籍 ID 不能为空")
            )
            
            logger.debug { "获取书籍详情: $id" }
            
            try {
                val book = bookService.getBookById(id)
                call.respond(mapOf(
                    "success" to true,
                    "book" to book
                ))
            } catch (e: Exception) {
                logger.error(e) { "获取书籍失败: $id" }
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书籍不存在"
                ))
            }
        }
        
        /**
         * 添加书籍
         * POST /api/books
         */
        post {
            try {
                val book = call.receive<Book>()
                logger.info { "添加书籍: ${book.name}" }
                
                val saved = bookService.saveBook(book)
                call.respond(HttpStatusCode.Created, mapOf(
                    "success" to true,
                    "book" to saved,
                    "message" to "书籍添加成功"
                ))
            } catch (e: Exception) {
                logger.error(e) { "添加书籍失败" }
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to "添加失败: ${e.message}"
                ))
            }
        }
        
        /**
         * 更新书籍信息
         * PUT /api/books/{id}
         */
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书籍 ID 不能为空")
            )
            
            try {
                val book = call.receive<Book>()
                logger.info { "更新书籍: $id" }
                
                val updated = bookService.saveBook(book.copy(id = id))
                call.respond(mapOf(
                    "success" to true,
                    "book" to updated,
                    "message" to "书籍更新成功"
                ))
            } catch (e: Exception) {
                logger.error(e) { "更新书籍失败: $id" }
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书籍不存在"
                ))
            }
        }
        
        /**
         * 删除书籍
         * DELETE /api/books/{id}
         */
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书籍 ID 不能为空")
            )
            
            logger.info { "删除书籍: $id" }
            
            try {
                if (bookService.deleteBook(id)) {
                    call.respond(mapOf(
                        "success" to true,
                        "message" to "书籍删除成功"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf(
                        "success" to false,
                        "message" to "书籍不存在"
                    ))
                }
            } catch (e: Exception) {
                logger.error(e) { "删除书籍失败: $id" }
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "删除失败: ${e.message}"
                ))
            }
        }
        
        /**
         * 批量删除书籍
         * DELETE /api/books
         */
        delete {
            try {
                val request = call.receive<Map<String, Any>>()
                val ids = request["ids"] as? List<String> ?: emptyList()
                
                logger.info { "批量删除书籍: ${ids.size} 本" }
                
                val count = bookService.deleteBooks(ids)
                call.respond(mapOf(
                    "success" to true,
                    "deleted" to count,
                    "message" to "成功删除 $count 本书籍"
                ))
            } catch (e: Exception) {
                logger.error(e) { "批量删除书籍失败" }
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "删除失败: ${e.message}"
                ))
            }
        }
        
        /**
         * 更新阅读进度
         * PUT /api/books/{id}/progress
         */
        put("/{id}/progress") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书籍 ID 不能为空")
            )
            
            try {
                val request = call.receive<Map<String, Any>>()
                val chapterIndex = request["chapterIndex"] as? Int 
                    ?: request["chapter"] as? Int ?: 0
                val chapterUrl = request["chapterUrl"] as? String ?: ""
                
                logger.debug { "更新阅读进度: $id, 章节: $chapterIndex" }
                
                val book = bookService.updateReadingProgress(id, chapterIndex, chapterUrl)
                call.respond(mapOf(
                    "success" to true,
                    "book" to book,
                    "message" to "阅读进度更新成功"
                ))
            } catch (e: Exception) {
                logger.error(e) { "更新阅读进度失败: $id" }
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书籍不存在"
                ))
            }
        }
        
        /**
         * 获取书籍章节列表
         * GET /api/books/{id}/chapters?page=0&size=100
         */
        get("/{id}/chapters") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "书籍 ID 不能为空")
            )
            
            logger.debug { "获取章节列表: $id" }
            
            try {
                val book = bookService.getBookById(id)
                // TODO: 实现章节列表获取逻辑
                call.respond(mapOf(
                    "success" to true,
                    "chapters" to emptyList<Any>(),
                    "total" to 0
                ))
            } catch (e: Exception) {
                logger.error(e) { "获取章节列表失败: $id" }
                call.respond(HttpStatusCode.NotFound, mapOf(
                    "success" to false,
                    "message" to "书籍不存在"
                ))
            }
        }
    }
}
