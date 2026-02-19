package com.moyue.controller

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.service.BookService
import com.moyue.service.SourceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService,
    private val sourceService: SourceService
) {

    /**
     * 获取书架所有书籍
     */
    @GetMapping
    fun getAllBooks(): List<Book> {
        return bookService.getAllBooks()
    }

    /**
     * 获取单本书籍详情
     */
    @GetMapping("/{id}")
    fun getBook(@PathVariable id: String): ResponseEntity<Book> {
        return bookService.getBookById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    /**
     * 添加书籍
     */
    @PostMapping
    fun addBook(@RequestBody book: Book): Book {
        return bookService.saveBook(book)
    }

    /**
     * 更新书籍信息
     */
    @PutMapping("/{id}")
    fun updateBook(@PathVariable id: String, @RequestBody book: Book): ResponseEntity<Book> {
        return if (bookService.getBookById(id) != null) {
            ResponseEntity.ok(bookService.saveBook(book.copy(id = id)))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除书籍
     */
    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: String): ResponseEntity<Void> {
        return if (bookService.deleteBook(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 更新阅读进度
     */
    @PutMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: String,
        @RequestParam chapter: Int,
        @RequestParam progress: Int
    ): ResponseEntity<Book> {
        val book = bookService.getBookById(id) ?: return ResponseEntity.notFound().build()
        
        val updatedBook = book.copy(
            currentChapter = chapter,
            progress = progress,
            lastReadAt = LocalDateTime.now()
        )
        
        return ResponseEntity.ok(bookService.saveBook(updatedBook))
    }

    /**
     * 获取书籍章节列表
     */
    @GetMapping("/{id}/chapters")
    fun getChapters(@PathVariable id: String): ResponseEntity<List<BookChapter>> {
        val book = bookService.getBookById(id) ?: return ResponseEntity.notFound().build()
        
        val chapters = sourceService.getChapterList(book.bookUrl, book.origin ?: "")
        return ResponseEntity.ok(chapters)
    }

    /**
     * 搜索书籍（从书源）
     */
    @GetMapping("/search")
    fun searchBooks(
        @RequestParam keyword: String,
        @RequestParam(required = false) sourceIds: List<String>?
    ): List<Map<String, Any>> {
        return sourceService.search(keyword, sourceIds).map { result ->
            mapOf(
                "book" to result.book,
                "source" to result.source
            )
        }
    }

    /**
     * 导入本地书籍
     */
    @PostMapping("/import")
    fun importLocalBook(
        @RequestParam filePath: String,
        @RequestParam(required = false) sourceId: String?
    ): ResponseEntity<Book> {
        return try {
            val book = bookService.importLocalBook(filePath, sourceId)
            ResponseEntity.ok(book)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 获取最近阅读的书籍
     */
    @GetMapping("/recent")
    fun getRecentBooks(@RequestParam(defaultValue = "10") limit: Int): List<Book> {
        return bookService.getRecentBooks(limit)
    }
}
