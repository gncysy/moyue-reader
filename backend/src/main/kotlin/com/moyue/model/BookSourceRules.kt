package com.moyue.model
 
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import org.jspecify.annotations.Nullable
import tools.jackson.annotation.JsonFormat
import tools.jackson.annotation.JsonProperty
import java.time.LocalDateTime
 
/**
 * 书源规则实体
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 支持 Legado 书源规则格式
 *
 * @property id 唯一标识（UUID）
 * @property ruleId 规则 ID（书源 ID）
 * @property searchUrl 搜索 URL
 * @property searchList 搜索列表规则
 * @property searchName 搜索名称规则
 * @property searchAuthor 搜索作者规则
 * @property searchCoverUrl 搜索封面规则
 * @property searchBookUrl 搜索书籍 URL 规则
 * @property bookUrl 书籍 URL
 * @property bookInfo 书籍信息规则
 * @property chapterList 章节列表规则
 * @property chapterName 章节名称规则
 * @property chapterUrl 章节 URL 规则
 * @property contentUrl 内容 URL 规则
 * @property content 内容规则
 * @property enabled 是否启用
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity
@Table(
    name = "book_source_rules",
    indexes = [
        Index(name = "idx_rule_id", columnList = "rule_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_rule_id", columnNames = ["rule_id"])
    ]
)
data class BookSourceRules(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(name = "rule_id", unique = true, length = 100)
    @field:NotBlank(message = "规则 ID 不能为空")
    var ruleId: String = "",
    
    // ==================== 搜索规则 ====================
    
    @Column(name = "search_url", columnDefinition = "TEXT")
    @Nullable
    var searchUrl: String? = null,
    
    @Column(name = "search_list", columnDefinition = "TEXT")
    @Nullable
    var searchList: String? = null,
    
    @Column(name = "search_name", columnDefinition = "TEXT")
    @Nullable
    var searchName: String? = null,
    
    @Column(name = "search_author", columnDefinition = "TEXT")
    @Nullable
    var searchAuthor: String? = null,
    
    @Column(name = "search_cover_url", columnDefinition = "TEXT")
    @Nullable
    var searchCoverUrl: String? = null,
    
    @Column(name = "search_book_url", columnDefinition = "TEXT")
    @Nullable
    var searchBookUrl: String? = null,
    
    // ==================== 书籍详情规则 ====================
    
    @Column(name = "book_url", columnDefinition = "TEXT")
    @Nullable
    var bookUrl: String? = null,
    
    @Column(name = "book_info", columnDefinition = "TEXT")
    @Nullable
    var bookInfo: String? = null,
    
    // ==================== 章节列表规则 ====================
    
    @Column(name = "chapter_list", columnDefinition = "TEXT")
    @Nullable
    var chapterList: String? = null,
    
    @Column(name = "chapter_name", columnDefinition = "TEXT")
    @Nullable
    var chapterName: String? = null,
    
    @Column(name = "chapter_url", columnDefinition = "TEXT")
    @Nullable
    var chapterUrl: String? = null,
    
    // ==================== 内容规则 ====================
    
    @Column(name = "content_url", columnDefinition = "TEXT")
    @Nullable
    var contentUrl: String? = null,
    
    @Column(name = "content", columnDefinition = "TEXT")
    @Nullable
    var content: String? = null,
    
    // ==================== 其他规则 ====================
    
    @Column(name = "headers", columnDefinition = "TEXT")
    @Nullable
    var headers: String? = null,
    
    @Column(name = "charset", length = 50)
    @Nullable
    var charset: String? = null,
    
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
    
    @Column(name = "created_at", updatable = false, nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 判断规则是否有效
     */
    @get:JsonProperty("isValid")
    val isValid: Boolean
        get() = enabled && (
            searchUrl != null ||
            bookUrl != null ||
            chapterUrl != null
        )
    
    /**
     * 获取规则统计
     */
    @get:JsonProperty("ruleStats")
    val ruleStats: Map<String, Int>
        get() = mapOf(
            "searchRules" to countRules(searchUrl, searchList, searchName, searchAuthor, searchCoverUrl, searchBookUrl),
            "bookInfoRules" to countRules(bookUrl, bookInfo),
            "chapterRules" to countRules(chapterList, chapterName, chapterUrl),
            "contentRules" to countRules(contentUrl, content)
        )
    
    private fun countRules(vararg rules: String?): Int {
        return rules.count { !it.isNullOrEmpty() }
    }
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }
    
    companion object {
        private const val serialVersionUID = 1L
        
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): BookSourceRules {
            return com.google.gson.Gson().fromJson(json, BookSourceRules::class.java)
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
