package com.moyue.model
 
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
 
/**
 * 书籍数据类
 * 替代原 JPA 实体，使用 Exposed ORM
 */
data class Book(
    val id: String = UUID.randomUUID().toString(),
    
    @field:NotBlank(message = "书名不能为空")
    var name: String = "",
    
    var author: String = "",
    
    var coverUrl: String? = null,
    
    var intro: String? = null,
    
    var bookUrl: String = "",
    
    var origin: String? = null,
    
    var chapterCount: Int = 0,
    
    var currentChapter: Int = 0,
    
    var progress: Int = 0,
    
    var lastReadAt: LocalDateTime? = null,
    
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    /**
     * 更新阅读进度
     */
    fun updateProgress(chapterIndex: Int, chapterProgress: Int) {
        this.currentChapter = chapterIndex
        this.progress = chapterProgress
        this.lastReadAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
    
    /**
     * 判断是否读完
     */
    fun isFinished(): Boolean {
        return currentChapter >= chapterCount - 1 && progress >= 95
    }
    
    /**
     * 更新时间戳
     */
    fun touch() {
        this.updatedAt = LocalDateTime.now()
    }
    
    companion object {
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): Book {
            return Gson().fromJson(json, Book::class.java)
        }
    }
}
 
/**
 * 章节数据类
 */
data class BookChapter(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val index: Int,
    val title: String,
    val url: String,
    var content: String? = null,
    var isCached: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 标记为已缓存
     */
    fun markAsCached() {
        this.isCached = true
        this.updatedAt = LocalDateTime.now()
    }
}
