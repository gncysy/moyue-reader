package com.moyue.model
 
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import tools.jackson.annotation.JsonFormat
import tools.jackson.annotation.JsonIgnore
import tools.jackson.annotation.JsonProperty
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
 
/**
 * 书籍实体
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 使用 Jackson 3.x (tools.jackson)
 *
 * @property id 唯一标识（UUID）
 * @property name 书籍名称
 * @property author 作者
 * @property coverUrl 封面 URL
 * @property intro 简介
 * @property bookUrl 书籍源 URL
 * @property origin 书源 ID
 * @property source 书源（关联）
 * @property chapterCount 章节总数
 * @property currentChapter 当前章节
 * @property progress 阅读进度（0-100）
 * @property lastReadAt 最后阅读时间
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity
@Table(
    name = "books",
    indexes = [
        Index(name = "idx_origin", columnList = "origin"),
        Index(name = "idx_last_read_at", columnList = "last_read_at"),
        Index(name = "idx_updated_at", columnList = "updated_at")
    ]
)
data class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(nullable = false)
    @field:NotBlank(message = "书名不能为空")
    var name: String = "",
    
    @Column(nullable = false)
    var author: String = "",
    
    @Column(length = 1000)
    @Nullable
    var coverUrl: String? = null,
    
    @Column(length = 10000)
    @Nullable
    var intro: String? = null,
    
    @Column(name = "book_url", nullable = false, length = 2000)
    var bookUrl: String = "",
    
    @Column(name = "origin", length = 100, nullable = true)
    @Nullable
    var origin: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = true)
    @JsonIgnore  // 避免循环序列化
    @Nullable
    var source: BookSource? = null,
    
    @Column(nullable = false)
    var chapterCount: Int = 0,
    
    @Column(name = "current_chapter", nullable = false)
    var currentChapter: Int = 0,
    
    @Column(
        columnDefinition = "INT DEFAULT 0 CHECK (progress >= 0 AND progress <= 100)",
        nullable = false
    )
    var progress: Int = 0,
    
    @Column(name = "last_read_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Nullable
    var lastReadAt: LocalDateTime? = null,
    
    @Column(name = "created_at", updatable = false, nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
     * 获取阅读进度百分比
     */
    @get:JsonProperty("progressPercent")
    val progressPercent: String
        get() = if (chapterCount > 0) {
            String.format("%.1f%%", (currentChapter * 100.0 / chapterCount))
        } else {
            "0.0%"
        }
    
    companion object {
        private const val serialVersionUID = 1L
        
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): Book {
            return Gson().fromJson(json, Book::class.java)
        }
    }
    
    // ==================== JPA 生命周期回调 ====================
    
    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }
    
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}package com.moyue.model
 
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import tools.jackson.annotation.JsonFormat
import tools.jackson.annotation.JsonIgnore
import tools.jackson.annotation.JsonProperty
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
 
/**
 * 书籍实体
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 使用 Jackson 3.x (tools.jackson)
 *
 * @property id 唯一标识（UUID）
 * @property name 书籍名称
 * @property author 作者
 * @property coverUrl 封面 URL
 * @property intro 简介
 * @property bookUrl 书籍源 URL
 * @property origin 书源 ID
 * @property source 书源（关联）
 * @property chapterCount 章节总数
 * @property currentChapter 当前章节
 * @property progress 阅读进度（0-100）
 * @property lastReadAt 最后阅读时间
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity
@Table(
    name = "books",
    indexes = [
        Index(name = "idx_origin", columnList = "origin"),
        Index(name = "idx_last_read_at", columnList = "last_read_at"),
        Index(name = "idx_updated_at", columnList = "updated_at")
    ]
)
data class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(nullable = false)
    @field:NotBlank(message = "书名不能为空")
    var name: String = "",
    
    @Column(nullable = false)
    var author: String = "",
    
    @Column(length = 1000)
    @Nullable
    var coverUrl: String? = null,
    
    @Column(length = 10000)
    @Nullable
    var intro: String? = null,
    
    @Column(name = "book_url", nullable = false, length = 2000)
    var bookUrl: String = "",
    
    @Column(name = "origin", length = 100, nullable = true)
    @Nullable
    var origin: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = true)
    @JsonIgnore  // 避免循环序列化
    @Nullable
    var source: BookSource? = null,
    
    @Column(nullable = false)
    var chapterCount: Int = 0,
    
    @Column(name = "current_chapter", nullable = false)
    var currentChapter: Int = 0,
    
    @Column(
        columnDefinition = "INT DEFAULT 0 CHECK (progress >= 0 AND progress <= 100)",
        nullable = false
    )
    var progress: Int = 0,
    
    @Column(name = "last_read_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Nullable
    var lastReadAt: LocalDateTime? = null,
    
    @Column(name = "created_at", updatable = false, nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
     * 获取阅读进度百分比
     */
    @get:JsonProperty("progressPercent")
    val progressPercent: String
        get() = if (chapterCount > 0) {
            String.format("%.1f%%", (currentChapter * 100.0 / chapterCount))
        } else {
            "0.0%"
        }
    
    companion object {
        private const val serialVersionUID = 1L
        
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): Book {
            return Gson().fromJson(json, Book::class.java)
        }
    }
    
    // ==================== JPA 生命周期回调 ====================
    
    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }
    
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
