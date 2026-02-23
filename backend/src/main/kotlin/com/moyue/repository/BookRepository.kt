package com.moyue.repository
 
import com.moyue.model.Book
import com.moyue.model.tables.Books
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
 
/**
 * 书籍数据访问层
 * 替代原 JPA Repository，使用 Exposed ORM
 */
class BookRepository {
    
    /**
     * 查找所有书籍（分页）
     */
    fun findAll(page: Int = 0, size: Int = 20): List<Book> = transaction {
        Books.selectAll()
            .orderBy(Books.updatedAt to SortOrder.DESC)
            .limit(size, offset = page * size)
            .map { it.toBook() }
    }
    
    /**
     * 按 ID 查找书籍
     */
    fun findById(id: String): Book? = transaction {
        Books.select { Books.id eq id }
            .singleOrNull()
            ?.toBook()
    }
    
    /**
     * 按书源 ID 查找书籍
     */
    fun findByOrigin(sourceId: String?): List<Book> = transaction {
        Books.select { Books.origin eq sourceId }
            .map { it.toBook() }
    }
    
    /**
     * 按书名模糊查找
     */
    fun findByNameContaining(name: String): List<Book> = transaction {
        Books.select { Books.name like "%$name%" }
            .map { it.toBook() }
    }
    
    /**
     * 按作者模糊查找
     */
    fun findByAuthorContaining(author: String): List<Book> = transaction {
        Books.select { Books.author like "%$author%" }
            .map { it.toBook() }
    }
    
    /**
     * 获取最近阅读的 10 本书
     */
    fun findRecentReads(limit: Int = 10): List<Book> = transaction {
        Books.selectAll()
            .orderBy(Books.lastReadAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toBook() }
    }
    
    /**
     * 统计书源书籍数量
     */
    fun countByOrigin(sourceId: String?): Long = transaction {
        Books.select { Books.origin eq sourceId }.count()
    }
    
    /**
     * 保存书籍
     */
    fun save(book: Book): Book = transaction {
        val now = LocalDateTime.now()
        
        Books.insert {
            it[id] = book.id
            it[name] = book.name
            it[author] = book.author
            it[coverUrl] = book.coverUrl
            it[intro] = book.intro
            it[bookUrl] = book.bookUrl
            it[origin] = book.origin
            it[chapterCount] = book.chapterCount
            it[currentChapter] = book.currentChapter
            it[progress] = book.progress
            it[lastReadAt] = book.lastReadAt
            it[createdAt] = now
            it[updatedAt] = now
        }
        
        book.copy(updatedAt = now)
    }
    
    /**
     * 更新书籍
     */
    fun update(book: Book): Book = transaction {
        Books.update({ Books.id eq book.id }) {
            it[name] = book.name
            it[author] = book.author
            it[coverUrl] = book.coverUrl
            it[intro] = book.intro
            it[bookUrl] = book.bookUrl
            it[origin] = book.origin
            it[chapterCount] = book.chapterCount
            it[currentChapter] = book.currentChapter
            it[progress] = book.progress
            it[lastReadAt] = book.lastReadAt
            it[updatedAt] = LocalDateTime.now()
        }
        
        book.copy(updatedAt = LocalDateTime.now())
    }
    
    /**
     * 删除书籍
     */
    fun delete(id: String): Boolean = transaction {
        Books.deleteWhere { Books.id eq id } > 0
    }
    
    /**
     * 批量删除书籍
     */
    fun deleteByIds(ids: List<String>): Int = transaction {
        Books.deleteWhere { Books.id inList ids }
    }
    
    /**
     * 统计总数
     */
    fun count(): Long = transaction {
        Books.selectAll().count()
    }
    
    // ==================== 扩展函数 ====================
    
    private fun ResultRow.toBook() = Book(
        id = this[Books.id].toString(),
        name = this[Books.name],
        author = this[Books.author],
        coverUrl = this[Books.coverUrl],
        intro = this[Books.intro],
        bookUrl = this[Books.bookUrl],
        origin = this[Books.origin],
        chapterCount = this[Books.chapterCount],
        currentChapter = this[Books.currentChapter],
        progress = this[Books.progress],
        lastReadAt = this[Books.lastReadAt],
        createdAt = this[Books.createdAt],
        updatedAt = this[Books.updatedAt]
    )
}
