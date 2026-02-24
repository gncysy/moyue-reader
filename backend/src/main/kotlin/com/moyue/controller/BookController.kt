package com.moyue.controller
 
import com.moyue.model.Book
import com.moyue.service.BookService
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
 
/**
 * 书籍控制器
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 书籍 CRUD
 * - 阅读进度管理
 * - 书籍搜索
 * - 书籍导入/导出
 * - 书架管理
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@RestController
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService,
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(BookController::class.java)
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有书籍（分页）
     */
    @GetMapping
    fun getAllBooks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<Book>>> {
        logger.debug("获取书籍列表: page=$page, size=$size")
        
        val books = bookService.getAllBooks(page, size)
        return ResponseEntity.ok(
            ApiResponse.success(
                data = books,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取单本书籍详情
     */
    @GetMapping("/{id}")
    fun getBook(@PathVariable id: String): ResponseEntity<ApiResponse<Book>> {
        logger.debug("获取书籍详情: $id")
        
        return try {
            val book = bookService.getBookById(id)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = book,
                    message = "获取成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    code = "BOOK_NOT_FOUND",
                    message = "书籍不存在: $id"
                )
            )
        }
    }
    
    /**
     * 搜索书籍
     */
    @GetMapping("/search")
    fun searchBooks(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<Book>>> {
        logger.info("搜索书籍: $keyword")
        
        if (keyword.isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    code = "INVALID_KEYWORD",
                    message = "搜索关键词不能为空"
                )
            )
        }
        
        val books = bookService.searchBooks(keyword, page, size)
        return ResponseEntity.ok(
            ApiResponse.success(
                data = books,
                message = "搜索完成，找到 ${books.totalElements} 本书籍"
            )
        )
    }
    
    /**
     * 获取最近阅读的书籍
     */
    @GetMapping("/recent")
    fun getRecentBooks(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<List<Book>>> {
        logger.debug("获取最近阅读书籍: limit=$limit")
        
        val books = bookService.getRecentBooks(limit.coerceAtMost(100))
        return ResponseEntity.ok(
            ApiResponse.success(
                data = books,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取未读完的书籍
     */
    @GetMapping("/reading")
    fun getReadingBooks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<Book>>> {
        logger.debug("获取未读完的书籍")
        
        val books = bookService.getReadingBooks(page, size)
        return ResponseEntity.ok(
            ApiResponse.success(
                data = books,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取已读完的书籍
     */
    @GetMapping("/finished")
    fun getFinishedBooks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<Book>>> {
        logger.debug("获取已读完的书籍")
        
        val books = bookService.getFinishedBooks(page, size)
        return ResponseEntity.ok(
            ApiResponse.success(
                data = books,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取书籍统计
     */
    @GetMapping("/stats")
    fun getBookStats(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.debug("获取书籍统计")
        
        val stats = bookService.getBookStats()
        return ResponseEntity.ok(
            ApiResponse.success(
                data = stats,
                message = "获取成功"
            )
        )
    }
    
    // ==================== 保存操作 ====================
    
    /**
     * 添加书籍
     */
    @PostMapping
    fun addBook(@RequestBody @Valid request: AddBookRequest): ResponseEntity<ApiResponse<Book>> {
        logger.info("添加书籍: ${request.name}")
        
        return try {
            val book = Book(
                name = request.name,
                author = request.author,
                coverUrl = request.coverUrl,
                intro = request.intro,
                bookUrl = request.bookUrl,
                origin = request.sourceId
            )
            
            val saved = bookService.saveBook(book)
            ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    data = saved,
                    message = "书籍添加成功"
                )
            )
        } catch (e: Exception) {
            logger.error("添加书籍失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "ADD_BOOK_FAILED",
                    message = "添加失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 导入书籍（文件上传）
     */
    @PostMapping("/import")
    fun importBook(
        @RequestParam file: MultipartFile,
        @RequestParam(required = false) sourceId: String?
    ): ResponseEntity<ApiResponse<Book>> {
        logger.info("导入书籍: ${file.originalFilename}")
        
        return try {
            val book = bookService.importBook(file, sourceId)
            ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    data = book,
                    message = "书籍导入成功"
                )
            )
        } catch (e: Exception) {
            logger.error("导入书籍失败", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(
                    code = "IMPORT_BOOK_FAILED",
                    message = "导入失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新书籍信息
     */
    @PutMapping("/{id}")
    fun updateBook(
        @PathVariable id: String,
        @RequestBody @Valid request: UpdateBookRequest
    ): ResponseEntity<ApiResponse<Book>> {
        logger.info("更新书籍: $id")
        
        return try {
            val existing = bookService.getBookById(id)
            val updated = existing.copy(
                name = request.name ?: existing.name,
                author = request.author ?: existing.author,
                coverUrl = request.coverUrl ?: existing.coverUrl,
                intro = request.intro ?: existing.intro
            )
            
            val saved = bookService.saveBook(updated)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = saved,
                    message = "书籍更新成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    code = "BOOK_NOT_FOUND",
                    message = "书籍不存在: $id"
                )
            )
        } catch (e: Exception) {
            logger.error("更新书籍失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "UPDATE_BOOK_FAILED",
                    message = "更新失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 更新阅读进度
     */
    @PutMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: String,
        @RequestBody request: UpdateProgressRequest
    ): ResponseEntity<ApiResponse<Book>> {
        logger.debug("更新阅读进度: $id")
        
        return try {
            val book = bookService.updateReadingProgress(
                id,
                request.chapterIndex,
                request.chapterUrl ?: ""
            )
            ResponseEntity.ok(
                ApiResponse.success(
                    data = book,
                    message = "阅读进度更新成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    code = "BOOK_NOT_FOUND",
                    message = "书籍不存在: $id"
                )
            )
        } catch (e: Exception) {
            logger.error("更新阅读进度失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "UPDATE_PROGRESS_FAILED",
                    message = "更新失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除书籍
     */
    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: String): ResponseEntity<ApiResponse<Unit>> {
        logger.info("删除书籍: $id")
        
        return try {
            val success = bookService.deleteBook(id)
            if (success) {
                ResponseEntity.ok(
                    ApiResponse.success(
                        message = "书籍删除成功"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(
                        code = "BOOK_NOT_FOUND",
                        message = "书籍不存在: $id"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("删除书籍失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "DELETE_BOOK_FAILED",
                    message = "删除失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 批量删除书籍
     */
    @DeleteMapping
    fun deleteBooks(@RequestBody request: DeleteBooksRequest): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("批量删除书籍: ${request.ids.size} 本")
        
        return try {
            val count = bookService.deleteBooks(request.ids)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = mapOf("deleted" to count),
                    message = "成功删除 $count 本书籍"
                )
            )
        } catch (e: Exception) {
            logger.error("批量删除书籍失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "DELETE_BOOKS_FAILED",
                    message = "删除失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 章节操作 ====================
    
    /**
     * 获取书籍章节列表
     */
    @GetMapping("/{id}/chapters")
    fun getChapters(
        @PathVariable id: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.debug("获取章节列表: $id")
        
        return try {
            val book = bookService.getBookById(id)
            val chapters = sourceService.getChapterList(book.bookUrl, book.origin ?: "")
            
            // 分页处理
            val start = page * size
            val end = (start + size).coerceAtMost(chapters.size)
            val pagedChapters = if (start < chapters.size) {
                chapters.subList(start, end)
            } else {
                emptyList()
            }
            
            ResponseEntity.ok(
                ApiResponse.success(
                    data = mapOf(
                        "chapters" to pagedChapters,
                        "total" to chapters.size,
                        "page" to page,
                        "size" to size,
                        "book" to book
                    ),
                    message = "获取成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    code = "BOOK_NOT_FOUND",
                    message = "书籍不存在: $id"
                )
            )
        } catch (e: Exception) {
            logger.error("获取章节列表失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "GET_CHAPTERS_FAILED",
                    message = "获取失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 获取章节内容
     */
    @GetMapping("/{id}/chapters/{chapterIndex}/content")
    fun getChapterContent(
        @PathVariable id: String,
        @PathVariable chapterIndex: Int
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.debug("获取章节内容: $id, 章节: $chapterIndex")
        
        return try {
            val book = bookService.getBookById(id)
            val chapters = sourceService.getChapterList(book.bookUrl, book.origin ?: "")
            
            if (chapterIndex < 0 || chapterIndex >= chapters.size) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(
                        code = "CHAPTER_NOT_FOUND",
                        message = "章节不存在: $chapterIndex"
                    )
                )
            }
            
            val chapter = chapters[chapterIndex]
            val content = sourceService.getChapterContent(
                chapter.url,
                book.bookUrl,
                book.origin ?: ""
            )
            
            // 更新阅读进度
            bookService.updateReadingProgress(id, chapterIndex, chapter.url)
            
            ResponseEntity.ok(
                ApiResponse.success(
                    data = mapOf(
                        "content" to content,
                        "chapter" to chapter,
                        "book" to book
                    ),
                    message = "获取成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    code = "BOOK_NOT_FOUND",
                    message = "书籍不存在: $id"
                )
            )
        } catch (e: Exception) {
            logger.error("获取章节内容失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    code = "GET_CONTENT_FAILED",
                    message = "获取失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 请求/响应 DTO ====================
    
    /**
     * 添加书籍请求
     */
    data class AddBookRequest(
        @field:NotBlank(message = "书名不能为空")
        val name: String,
        
        val author: String = "",
        
        val coverUrl: String? = null,
        
        val intro: String? = null,
        
        @field:NotBlank(message = "书籍 URL 不能为空")
        val bookUrl: String,
        
        val sourceId: String? = null
    )
    
    /**
     * 更新书籍请求
     */
    data class UpdateBookRequest(
        val name: String? = null,
        val author: String? = null,
        val coverUrl: String? = null,
        val intro: String? = null
    )
    
    /**
     * 更新进度请求
     */
    data class UpdateProgressRequest(
        val chapterIndex: Int = 0,
        val chapterUrl: String? = null
    )
    
    /**
     * 删除书籍请求
     */
    data class DeleteBooksRequest(
        val ids: List<String>
    )
    
    /**
     * 通用 API 响应
     */
    data class ApiResponse<T>(
        val success: Boolean,
        val code: String? = null,
        val message: String? = null,
        val data: T? = null,
        val timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        companion object {
            fun <T> success(data: T? = null, message: String? = "操作成功"): ApiResponse<T> {
                return ApiResponse(
                    success = true,
                    message = message,
                    data = data
                )
            }
            
            fun <T> error(code: String, message: String): ApiResponse<T> {
                return ApiResponse(
                    success = false,
                    code = code,
                    message = message
                )
            }
        }
    }
}
