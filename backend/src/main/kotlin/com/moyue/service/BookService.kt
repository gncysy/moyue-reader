package com.moyue.service
 
import com.moyue.model.Book
import com.moyue.repository.BookRepository
import mu.KotlinLogging
 
/**
 * 书籍服务
 * 适配 Ktor + Exposed
 */
class BookService(
    private val bookRepository: BookRepository,
    private val cacheService: CacheService
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 获取所有书籍（分页）
     */
    fun getAllBooks(page: Int = 0, size: Int = 20): List<Book> {
        logger.debug { "获取书籍列表: page=$page, size=$size" }
        return bookRepository.findAll(page, size)
    }
    
    /**
     * 获取书籍详情
     */
    fun getBookById(id: String): Book {
        logger.debug { "获取书籍详情: $id" }
        return bookRepository.findById(id) 
            ?: throw IllegalArgumentException("书籍不存在: $id")
    }
    
    /**
     * 保存书籍
     */
    fun saveBook(book: Book): Book {
        logger.info { "保存书籍: ${book.name}" }
        
        val existing = bookRepository.findById(book.id)
        return if (existing != null) {
            bookRepository.update(book)
        } else {
            bookRepository.save(book)
        }
    }
    
    /**
     * 删除书籍
     */
    fun deleteBook(id: String): Boolean {
        logger.info { "删除书籍: $id" }
        return bookRepository.delete(id)
    }
    
    /**
     * 批量删除书籍
     */
    fun deleteBooks(ids: List<String>): Int {
        logger.info { "批量删除书籍: ${ids.size} 本" }
        return bookRepository.deleteByIds(ids)
    }
    
    /**
     * 更新阅读进度
     */
    fun updateReadingProgress(id: String, chapterIndex: Int, chapterUrl: String): Book {
        logger.debug { "更新阅读进度: $id, 章节: $chapterIndex" }
        
        val book = getBookById(id)
        book.updateProgress(chapterIndex, 0)
        
        val updated = bookRepository.update(book)
        
        // 清除缓存
        cacheService.evict("book:$id")
        
        return updated
    }
    
    /**
     * 搜索书籍
     */
    fun searchBooks(keyword: String, page: Int = 0, size: Int = 20): List<Book> {
        logger.debug { "搜索书籍: $keyword" }
        return bookRepository.findByNameContaining(keyword)
    }
    
    /**
     * 获取最近阅读
     */
    fun getRecentReads(limit: Int = 10): List<Book> {
        return bookRepository.findRecentReads(limit)
    }
}
