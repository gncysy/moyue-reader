package com.moyue.repository
 
import com.moyue.model.Book
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
 
/**
 * 书籍仓储接口
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * Spring Data JPA
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Repository
interface BookRepository : JpaRepository<Book, String> {
    
    /**
     * 根据书源 ID 查询书籍
     */
    fun findByOrigin(origin: String?): List<Book>
    
    /**
     * 根据书源 ID 分页查询书籍
     */
    fun findByOrigin(origin: String?, pageable: Pageable): Page<Book>
    
    /**
     * 根据名称模糊搜索书籍
     */
    @Query("""
        SELECT b FROM Book b 
        WHERE b.name LIKE %:keyword% 
           OR b.author LIKE %:keyword%
    """)
    fun searchBooks(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<Book>
    
    /**
     * 获取最近阅读的书籍
     */
    @EntityGraph(attributePaths = ["source"])
    @Query("""
        SELECT b FROM Book b 
        WHERE b.lastReadAt IS NOT NULL 
        ORDER BY b.lastReadAt DESC
    """)
    fun findRecentBooks(pageable: Pageable): Page<Book>
    
    /**
     * 获取最近更新的书籍
     */
    @EntityGraph(attributePaths = ["source"])
    @Query("""
        SELECT b FROM Book b 
        ORDER BY b.updatedAt DESC
    """)
    fun findRecentlyUpdatedBooks(pageable: Pageable): Page<Book>
    
    /**
     * 统计书籍总数
     */
    fun countByOrigin(origin: String?): Long
    
    /**
     * 根据书源 ID 删除书籍
     */
    fun deleteByOrigin(origin: String?)
    
    /**
     * 检查书籍 URL 是否存在
     */
    fun existsByBookUrl(bookUrl: String): Boolean
    
    /**
     * 根据书籍 URL 查询书籍
     */
    fun findByBookUrl(bookUrl: String): @Nullable Book?
    
    /**
     * 获取未读完的书籍
     */
    @Query("""
        SELECT b FROM Book b 
        WHERE b.progress < 95 OR b.currentChapter < b.chapterCount - 1
        ORDER BY b.lastReadAt DESC
    """)
    fun findReadingBooks(pageable: Pageable): Page<Book>
    
    /**
     * 获取已读完的书籍
     */
    @Query("""
        SELECT b FROM Book b 
        WHERE b.progress >= 95 AND b.currentChapter >= b.chapterCount - 1
        ORDER BY b.lastReadAt DESC
    """)
    fun findFinishedBooks(pageable: Pageable): Page<Book>
    
    /**
     * 获取书籍统计信息
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN b.lastReadAt IS NOT NULL THEN 1 ELSE 0 END) as readCount,
            SUM(CASE WHEN b.progress >= 95 AND b.currentChapter >= b.chapterCount - 1 THEN 1 ELSE 0 END) as finishedCount
        FROM Book b
    """)
    fun getBookStats(): Map<String, Any>
}
