package com.moyue.model

import com.google.gson.Gson
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

/**
 * 书籍章节实体
 * 
 * @property id 唯一标识（UUID）
 * @property bookId 书籍 ID
 * @property title 章节标题
 * @property url 章节内容 URL
 * @property bookUrl 书籍源 URL
 * @property index 章节索引
 * @property content 章节内容（缓存）
 * @property isRead 是否已读
 * @property cachedAt 缓存时间
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity
@Table(name = "book_chapters", indexes = [
    Index(name = "idx_book_id", columnList = "book_id"),
    Index(name = "idx_book_url", columnList = "book_url"),
    Index(name = "idx_index", columnList = "`index`")
])
data class BookChapter(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(name = "book_id", nullable = false)
    @field:NotBlank(message = "书籍 ID 不能为空")
    var bookId: String? = null,
    
    @Column(nullable = false)
    @field:NotBlank(message = "章节标题不能为空")
    var title: String = "",
    
    @Column(nullable = false)
    @field:NotBlank(message = "章节 URL 不能为空")
    var url: String = "",
    
    @Column(name = "book_url", nullable = false)
    var bookUrl: String = "",
    
    @Column(name = "`index`", nullable = false)
    var index: Int = 0,
    
    @Column(columnDefinition = "CLOB")
    var content: String? = null,
    
    @Column(name = "is_read")
    var isRead: Boolean = false,
    
    @Column(name = "cached_at")
    var cachedAt: LocalDateTime? = null,
    
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    /**
     * 判断内容是否已缓存
     */
    fun isCached(): Boolean {
        return content != null && cachedAt != null
    }
    
    /**
     * 标记为已读
     */
    fun markAsRead() {
        this.isRead = true
        this.updatedAt = LocalDateTime.now()
    }
    
    /**
     * 更新缓存内容
     */
    fun updateContent(content: String) {
        this.content = content
        this.cachedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
    
    companion object {
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): BookChapter {
            return Gson().fromJson(json, BookChapter::class.java)
        }
    }
    
    // ==================== JPA 生命周期回调 ====================
    
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }
    
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
