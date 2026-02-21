package com.moyue.model

import com.google.gson.Gson
import com.google.gson.annotations.Expose
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

/**
 * 书源实体
 * 
 * @property id 唯一标识（UUID）
 * @property name 书源名称
 * @property url 书源地址
 * @property group 分组
 * @property enabled 是否启用
 * @property enableJs 是否启用 JS
 * @property concurrent 并发数
 * @property weight 权重
 * @property loginUrl 登录 URL
 * @property loginCheckJs 登录检查 JS
 * @property headerJs 请求头 JS
 * @property searchUrl 搜索 URL 模板
 * @property ruleSearch 搜索规则
 * @property ruleBookInfo 书籍信息规则
 * @property ruleToc 目录规则
 * @property ruleContent 内容规则
 * @property ruleExplore 探索规则
 * @property charset 字符集
 * @property securityRating 安全评级（1-5）
 * @property lastUsed 最后使用时间
 * @property failCount 失败次数
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity
@Table(name = "book_sources", indexes = [
    Index(name = "idx_url", columnList = "url"),
    Index(name = "idx_group", columnList = "group"),
    Index(name = "idx_enabled", columnList = "enabled"),
    Index(name = "idx_weight", columnList = "weight"),
    Index(name = "idx_security_rating", columnList = "security_rating")
])
data class BookSource(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(nullable = false, length = 100)
    @field:NotBlank(message = "书源名称不能为空")
    var name: String = "",

    @Column(nullable = false, unique = true, length = 500)
    @field:NotBlank(message = "书源 URL 不能为空")
    var url: String = "",

    @Column(length = 100)
    var group: String? = null,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "enable_js", nullable = false)
    var enableJs: Boolean = true,

    @Column(name = "concurrent", nullable = false)
    var concurrent: Int = 1,

    @Column(nullable = false)
    var weight: Int = 0,

    @Column(name = "login_url", length = 500)
    var loginUrl: String? = null,

    @Column(name = "login_check_js", columnDefinition = "TEXT")
    var loginCheckJs: String? = null,

    @Column(name = "header_js", columnDefinition = "TEXT")
    var headerJs: String? = null,

    @Column(name = "search_url", length = 2000)
    var searchUrl: String? = null,

    @Convert(converter = SearchRuleConverter::class)
    @Column(name = "rule_search", length = 5000)
    @Expose(serialize = false)  // 规则不需要序列化到 JSON
    var ruleSearch: SearchRule? = null,

    @Convert(converter = BookInfoRuleConverter::class)
    @Column(name = "rule_book_info", length = 5000)
    @Expose(serialize = false)
    var ruleBookInfo: BookInfoRule? = null,

    @Convert(converter = TocRuleConverter::class)
    @Column(name = "rule_toc", length = 5000)
    @Expose(serialize = false)
    var ruleToc: TocRule? = null,

    @Convert(converter = ContentRuleConverter::class)
    @Column(name = "rule_content", length = 5000)
    @Expose(serialize = false)
    var ruleContent: ContentRule? = null,

    @Convert(converter = ExploreRuleConverter::class)
    @Column(name = "rule_explore", length = 5000)
    @Expose(serialize = false)
    var ruleExplore: ExploreRule? = null,

    @Column(length = 20)
    var charset: String = "UTF-8",

    @Column(name = "security_rating", nullable = false)
    @field:Min(value = 1, message = "安全评级最小为 1")
    @field:Max(value = 5, message = "安全评级最大为 5")
    var securityRating: Int = 5,

    @Column(name = "last_used")
    var lastUsed: LocalDateTime? = null,

    @Column(name = "fail_count")
    var failCount: Int = 0,

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
     * 记录使用
     */
    fun recordUsage(success: Boolean = true) {
        this.lastUsed = LocalDateTime.now()
        if (success) {
            this.failCount = 0
        } else {
            this.failCount++
        }
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 判断是否安全
     */
    fun isSafe(): Boolean {
        return securityRating >= 3 && !failCountExceeded()
    }

    /**
     * 判断失败次数是否超限
     */
    private fun failCountExceeded(): Boolean {
        return failCount > 10
    }

    /**
     * 禁用书源
     */
    fun disable() {
        this.enabled = false
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 启用书源
     */
    fun enable() {
        this.enabled = true
        this.failCount = 0
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): BookSource {
            return Gson().fromJson(json, BookSource::class.java)
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
