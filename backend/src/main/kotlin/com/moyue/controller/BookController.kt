package com.moyue.controller

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.service.BookService
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService,
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(BookController::class.java)
    
    /**
     * 获取书架所有书籍（分页）
     */
    @GetMapping
    fun getAllBooks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<Book> {
        logger.debug("获取书籍列表: page=$page, size=$size")
        return bookService.getAllBooks(page, size)
    }
    
    /**
     * 获取单本书籍详情
     */
    @GetMapping("/{id}")
    fun getBook(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.debug("获取书籍详情: $id")
        return try {
            val book = bookService.getBookById(id)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "book" to book
            ))
        } catch (e: Exception) {
            logger.error("获取书籍失败: $id", e)
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 添加书籍
     */
    @PostMapping
    fun addBook(@RequestBody book: Book): ResponseEntity<Map<String, Any>> {
        logger.info("添加书籍: ${book.name}")
        return try {
            val saved = bookService.saveBook(book)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "book" to saved,
                "message" to "书籍添加成功"
            ))
        } catch (e: Exception) {
            logger.error("添加书籍失败", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "添加失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 更新书籍信息
     */
    @PutMapping("/{id}")
    fun updateBook(
        @PathVariable id: String,
        @RequestBody book: Book
    ): ResponseEntity<Map<String, Any>> {
        logger.info("更新书籍: $id")
        return try {
            val existing = bookService.getBookById(id)
            val updated = bookService.saveBook(book.copy(id = id))
            ResponseEntity.ok(mapOf(
                "success" to true,
                "book" to updated,
                "message" to "书籍更新成功"
            ))
        } catch (e: Exception) {
            logger.error("更新书籍失败: $id", e)
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 删除书籍
     */
    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("删除书籍: $id")
        return try {
            if (bookService.deleteBook(id)) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "书籍删除成功"
                ))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("删除书籍失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "删除失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量删除书籍
     */
    @DeleteMapping
    fun deleteBooks(@RequestBody ids: List<String>): ResponseEntity<Map<String, Any>> {
        logger.info("批量删除书籍: ${ids.size} 本")
        return try {
            val count = bookService.deleteBooks(ids)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "deleted" to count,
                "message" to "成功删除 $count 本书籍"
            ))
        } catch (e: Exception) {
            logger.error("批量删除书籍失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "删除失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 更新阅读进度
     */
    @PutMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val chapterIndex = request["chapterIndex"] as? Int ?: request["chapter"] as? Int ?: 0
        val chapterUrl = request["chapterUrl"] as? String ?: ""
        
        logger.debug("更新阅读进度: $id, 章节: $chapterIndex")
        
        return try {
            val book = bookService.updateReadingProgress(id, chapterIndex, chapterUrl)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "book" to book,
                "message" to "阅读进度更新成功"
            ))
        } catch (e: Exception) {
            logger.error("更新阅读进度失败: $id", e)
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 获取书籍章节列表
     */
    @GetMapping("/{id}/chapters")
    fun getChapters(
        @PathVariable id: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<Map<String, Any>> {
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
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "chapters" to pagedChapters,
                "total" to chapters.size,
                "page" to page,
                "size" to size
            ))
        } catch (e: Exception) {
            logger.error("获取章节列表失败: $id", e)
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 搜索书籍（从书源）
     */
    @GetMapping("/search")
    fun searchBooks(
        @RequestParam keyword: String,
        @RequestParam(required = false) sourceIds: List<String>?,
        @RequestParam(defaultValue = "3") maxConcurrent: Int
    ): ResponseEntity<Map<String, Any>> {
        if (keyword.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "搜索关键词不能为空"
            ))
        }
        
        logger.info("搜索书籍: $keyword, 书源: ${sourceIds?.size ?: "all"}")
        
        val results = try {
            sourceService.searchBook(keyword, sourceIds, maxConcurrent)
        } catch (e: Exception) {
            logger.error("搜索书籍失败: $keyword", e)
            emptyList()
        }
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "results" to results,
            "total" to results.size,
            "keyword" to keyword
        ))
    }
    
    /**
     * 导入本地书籍（文件上传）
     */
    @PostMapping("/import")
    fun importBook(
        @RequestParam file: MultipartFile,
        @RequestParam(required = false) sourceId: String?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("导入书籍: ${file.originalFilename}")
        
        return try {
            val book = bookService.importBook(file, sourceId)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "book" to book,
                "message" to "书籍导入成功"
            ))
        } catch (e: Exception) {
            logger.error("导入书籍失败", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "导入失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取书籍内容
     */
    @GetMapping("/{id}/chapters/{chapterIndex}/content")
    fun getChapterContent(
        @PathVariable id: String,
        @PathVariable chapterIndex: Int
    ): ResponseEntity<Map<String, Any>> {
        logger.debug("获取章节内容: $id, 章节: $chapterIndex")
        
        return try {
            val book = bookService.getBookById(id)
            val chapters = sourceService.getChapterList(book.bookUrl, book.origin ?: "")
            
            if (chapterIndex < 0 || chapterIndex >= chapters.size) {
                return ResponseEntity.notFound().build()
            }
            
            val chapter = chapters[chapterIndex]
            val content = sourceService.getChapterContent(
                chapter.url,
                book.bookUrl,
                book.origin ?: ""
            )
            
            // 更新阅读进度
            bookService.updateReadingProgress(id, chapterIndex, chapter.url)
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "content" to content,
                "chapter" to chapter
            ))
        } catch (e: Exception) {
            logger.error("获取章节内容失败: $id, 章节: $chapterIndex", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取最近阅读的书籍
     */
    @GetMapping("/recent")
    fun getRecentBooks(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<Map<String, Any>> {
        logger.debug("获取最近阅读书籍: limit=$limit")
        return try {
            val books = bookService.getRecentBooks(limit.coerceAtMost(100))
            ResponseEntity.ok(mapOf(
                "success" to true,
                "books" to books,
                "total" to books.size
            ))
        } catch (e: Exception) {
            logger.error("获取最近阅读书籍失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取书籍统计
     */
    @GetMapping("/stats")
    fun getBookStats(): ResponseEntity<Map<String, Any>> {
        return try {
            val stats = bookService.getBookStats()
            ResponseEntity.ok(mapOf(
                "success" to true,
                "stats" to stats
            ))
        } catch (e: Exception) {
            logger.error("获取书籍统计失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
}
