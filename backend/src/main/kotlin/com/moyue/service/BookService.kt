package com.moyue.service
 
import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.repository.BookRepository
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.jspecify.annotations.Nullable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
 
/**
 * 书籍服务
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 书籍 CRUD
 * - 阅读进度管理
 * - 书籍搜索
 * - 统计信息
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Service
@Transactional
class BookService(
    private val bookRepository: BookRepository,
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(BookService::class.java)
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有书籍（分页）
     */
    @Cacheable(value = ["books"], key = "#page + '-' + #size")
    fun getAllBooks(page: Int = 0, size: Int = 20): Page<Book> {
        logger.debug("获取书籍列表: page=$page, size=$size")
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        return bookRepository.findAll(pageable)
    }
    
    /**
     * 根据书籍 ID 查询
     */
    @Cacheable(value = ["book"], key = "#id")
    fun getBookById(id: String): Book {
        return bookRepository.findById(id)
            .orElseThrow { IllegalArgumentException("书籍不存在: $id") }
    }
    
    /**
     * 根据书源 ID 查询书籍
     */
    @Cacheable(value = ["books-by-source"], key = "#sourceId")
    fun getBooksBySource(sourceId: String, page: Int = 0, size: Int = 20): Page<Book> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        return bookRepository.findByOrigin(sourceId, pageable)
    }
    
    /**
     * 搜索书籍
     */
    @Cacheable(value = ["book-search"], key = "#keyword + '-' + #page + '-' + #size")
    fun searchBooks(keyword: String, page: Int = 0, size: Int = 20): Page<Book> {
        logger.debug("搜索书籍: $keyword, page=$page, size=$size")
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        return bookRepository.searchBooks(keyword, pageable)
    }
    
    /**
     * 获取最近阅读的书籍
     */
    @Cacheable(value = ["recent-books"], key = "#limit")
    fun getRecentBooks(limit: Int = 10): List<Book> {
        logger.debug("获取最近阅读书籍: limit=$limit")
        val pageable = PageRequest.of(0, limit)
        return bookRepository.findRecentBooks(pageable).content
    }
    
    /**
     * 获取未读完的书籍
     */
    @Cacheable(value = ["reading-books"], key = "#page + '-' + #size")
    fun getReadingBooks(page: Int = 0, size: Int = 20): Page<Book> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastReadAt"))
        return bookRepository.findReadingBooks(pageable)
    }
    
    /**
     * 获取已读完的书籍
     */
    @Cacheable(value = ["finished-books"], key = "#page + '-' + #size")
    fun getFinishedBooks(page: Int = 0, size: Int = 20): Page<Book> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastReadAt"))
        return bookRepository.findFinishedBooks(pageable)
    }
    
    // ==================== 保存操作 ====================
    
    /**
     * 保存书籍
     */
    @CacheEvict(value = ["books", "book-search", "recent-books"], allEntries = true)
    fun saveBook(book: Book): Book {
        logger.info("保存书籍: ${book.name}")
        return bookRepository.save(book)
    }
    
    /**
     * 批量保存书籍
     */
    @CacheEvict(value = ["books", "book-search", "recent-books"], allEntries = true)
    fun saveBooks(books: List<Book>): List<Book> {
        logger.info("批量保存书籍: ${books.size} 本")
        return bookRepository.saveAll(books)
    }
    
    /**
     * 添加书籍（从书源）
     */
    @CacheEvict(value = ["books", "book-search", "recent-books"], allEntries = true)
    fun addBook(bookUrl: String, sourceId: String?): Book {
        logger.info("添加书籍: $bookUrl, source=$sourceId")
        
        // 从书源获取书籍信息
        val bookInfo = sourceService.getBookInfo(bookUrl, sourceId ?: "")
        
        val book = Book(
            name = bookInfo["name"] as String,
            author = bookInfo["author"] as? String ?: "",
            coverUrl = bookInfo["coverUrl"] as? String,
            intro = bookInfo["intro"] as? String,
            bookUrl = bookUrl,
            origin = sourceId,
            chapterCount = (bookInfo["chapterCount"] as? Int) ?: 0
        )
        
        return bookRepository.save(book)
    }
    
    /**
     * 导入书籍（文件上传）
     */
    @CacheEvict(value = ["books", "book-search", "recent-books"], allEntries = true)
    fun importBook(file: MultipartFile, sourceId: String?): Book {
        logger.info("导入书籍: ${file.originalFilename}")
        
        val content = BufferedReader(InputStreamReader(file.inputStream)).use { it.readText() }
        val book = Book.fromJson(content)
        book.origin = sourceId
        
        return bookRepository.save(book)
    }
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新书籍
     */
    @CacheEvict(value = ["books", "book", "book-search", "recent-books"], allEntries = true)
    fun updateBook(id: String, book: Book): Book {
        logger.info("更新书籍: $id")
        
        val existing = getBookById(id)
        val updated = book.copy(
            id = id,
            createdAt = existing.createdAt
        )
        
        return bookRepository.save(updated)
    }
    
    /**
     * 更新阅读进度
     */
    @CacheEvict(value = ["books", "book", "recent-books", "reading-books"], allEntries = true)
    fun updateReadingProgress(id: String, chapterIndex: Int, chapterUrl: String): Book {
        logger.debug("更新阅读进度: $id, chapter=$chapterIndex")
        
        val book = getBookById(id)
        book.updateProgress(chapterIndex, 0)  // 简单起见，只记录章节
        book.lastReadAt = LocalDateTime.now()
        
        return bookRepository.save(book)
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除书籍
     */
    @CacheEvict(value = ["books", "book", "book-search", "recent-books"], allEntries = true)
    fun deleteBook(id: String): Boolean {
        logger.info("删除书籍: $id")
        
        return if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    /**
     * 批量删除书籍
     */
    @CacheEvict(value = ["books", "book", "book-search", "recent-books"], allEntries = true)
    fun deleteBooks(ids: List<String>): Int {
        logger.info("批量删除书籍: ${ids.size} 本")
        
        var count = 0
        ids.forEach { id ->
            if (deleteBook(id)) count++
        }
        
        return count
    }
    
    /**
     * 根据书源删除书籍
     */
    @CacheEvict(value = ["books", "book-search", "recent-books"], allEntries = true)
    fun deleteBooksBySource(sourceId: String) {
        logger.info("删除书源书籍: $sourceId")
        bookRepository.deleteByOrigin(sourceId)
    }
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取书籍统计
     */
    @Cacheable(value = ["book-stats"])
    fun getBookStats(): Map<String, Any> {
        logger.debug("获取书籍统计")
        return bookRepository.getBookStats()
    }
    
    /**
     * 清除缓存
     */
    @CacheEvict(value = ["books", "book", "book-search", "recent-books", "reading-books", "finished-books", "book-stats"], allEntries = true)
    fun clearCache() {
        logger.info("清除书籍缓存")
    }
}
