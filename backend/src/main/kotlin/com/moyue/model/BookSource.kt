package com.moyue.model

import com.google.gson.Gson
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "book_sources")
data class BookSource(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false, unique = true)
    var url: String = "",

    var group: String? = null,

    var enabled: Boolean = true,

    @Column(name = "enable_js")
    var enableJs: Boolean = true,

    @Column(name = "concurrent")
    var concurrent: Int = 1,

    var weight: Int = 0,

    @Column(name = "login_url")
    var loginUrl: String? = null,

    @Column(name = "login_check_js")
    var loginCheckJs: String? = null,

    @Column(name = "header_js")
    var headerJs: String? = null,

    // 新增：搜索 URL 模板，如：https://example.com/search?q={{key}}&page={{page}}
    @Column(name = "search_url", length = 2000)
    var searchUrl: String? = null,

    // 修改：规则字段改为对象类型，使用 JPA 转换器
    @Convert(converter = SearchRuleConverter::class)
    @Column(name = "rule_search", length = 5000)
    var ruleSearch: SearchRule? = null,

    @Convert(converter = BookInfoRuleConverter::class)
    @Column(name = "rule_book_info", length = 5000)
    var ruleBookInfo: BookInfoRule? = null,

    @Convert(converter = TocRuleConverter::class)
    @Column(name = "rule_toc", length = 5000)
    var ruleToc: TocRule? = null,

    @Convert(converter = ContentRuleConverter::class)
    @Column(name = "rule_content", length = 5000)
    var ruleContent: ContentRule? = null,

    @Convert(converter = ExploreRuleConverter::class)
    @Column(name = "rule_explore", length = 5000)
    var ruleExplore: ExploreRule? = null,

    var charset: String = "UTF-8",

    @Column(name = "security_rating")
    var securityRating: Int = 5,

    @Column(name = "last_used")
    var lastUsed: LocalDateTime? = null,

    @Column(name = "fail_count")
    var failCount: Int = 0,

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun toJson(): String {
        val map = mutableMapOf(
            "id" to id,
            "name" to name,
            "url" to url,
            "group" to group,
            "enabled" to enabled,
            "enableJs" to enableJs,
            "concurrent" to concurrent,
            "weight" to weight,
            "loginUrl" to loginUrl,
            "loginCheckJs" to loginCheckJs,
            "headerJs" to headerJs,
            "searchUrl" to searchUrl,
            "ruleSearch" to ruleSearch,
            "ruleBookInfo" to ruleBookInfo,
            "ruleToc" to ruleToc,
            "ruleContent" to ruleContent,
            "ruleExplore" to ruleExplore,
            "charset" to charset,
            "securityRating" to securityRating,
            "lastUsed" to lastUsed,
            "failCount" to failCount,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
        return Gson().toJson(map)
    }

    companion object {
        fun fromJson(json: String): BookSource {
            return Gson().fromJson(json, BookSource::class.java)
        }
    }
}
