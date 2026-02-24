package com.moyue.model
 
import jakarta.persistence.*
import org.jspecify.annotations.Nullable
import tools.jackson.annotation.JsonFormat
import tools.jackson.annotation.JsonProperty
import java.time.LocalDateTime
 
/**
 * 章节实体
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * @property id 唯一标识（UUID）
 * @property bookId 所属书籍 ID
 * @property book 关联书籍
 * @property index 章节索引
 * @property title 章节标题
 * @property url 章节内容 URL
 * @property isVip 是否 VIP 章节
 * @property content 章节内容（缓存）
 * @property contentCachedAt 内容缓存时间
 * @property createdAt 创建时间
 */
@Entity
@Table(
    name = "book_chapters",
    indexes = [
        Index(name = "idx_book_id", columnList = "book_id"),
        Index(name = "idx_book_index", columnList = "book_id, chapter_index")
    ]
)
data class BookChapter(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(name = "book_id", nullable = false)
    var bookId: String = "",
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    @Nullable
    var book: Book? = null,
    
    @Column(name = "chapter_index", nullable = false)
    var index: Int = 0,
    
    @Column(nullable = false, length = 500)
    var title: String = "",
    
    @Column(nullable = false, length = 2000)
    var url: String = "",
    
    @Column(nullable = false)
    var isVip: Boolean = false,
    
    @Column(columnDefinition = "TEXT", nullable = true)
    @Nullable
    var content: String? = null,
    
    @Column(name = "content_cached_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Nullable
    var contentCachedAt: LocalDateTime? = null,
    
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 判断内容是否已缓存
     */
    @get:JsonProperty("isContentCached")
    val isContentCached: Boolean
        get() = content != null && contentCachedAt != null
    
    /**
     * 判断内容是否过期（默认 24 小时）
     */
    fun isContentExpired(): Boolean {
        return if (contentCachedAt == null) {
            true
        } else {
            val hours = java.time.Duration.between(contentCachedAt, LocalDateTime.now()).toHours()
            hours > 24
        }
    }
    
    /**
     * 获取章节标题（去除特殊字符）
     */
    @get:JsonProperty("cleanTitle")
    val cleanTitle: String
        get() = title
            .replace(Regex("[\\r\\n]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    
    // ==================== JPA 生命周期回调 ====================
    
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
