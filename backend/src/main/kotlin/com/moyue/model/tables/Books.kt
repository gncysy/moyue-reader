package com.moyue.model.tables
 
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID
 
/**
 * 书籍表
 */
object Books : UUIDTable("books") {
    
    val name = varchar("name", 255)
    val author = varchar("author", 255)
    val coverUrl = varchar("cover_url", 1000).nullable()
    val intro = text("intro").nullable()
    val bookUrl = varchar("book_url", 1000)
    val origin = varchar("origin", 50).nullable()
    val chapterCount = integer("chapter_count").default(0)
    val currentChapter = integer("current_chapter").default(0)
    val progress = integer("progress").default(0)
    val lastReadAt = datetime("last_read_at").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
 
/**
 * 书籍章节表
 */
object BookChapters : UUIDTable("book_chapters") {
    
    val bookId = reference("book_id", Books)
    val index = integer("index")
    val title = varchar("title", 500)
    val url = varchar("url", 1000)
    val content = text("content").nullable()
    val isCached = bool("is_cached").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
