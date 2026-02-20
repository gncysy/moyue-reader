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

    @Column(name = "rule_search", length = 5000)
    var ruleSearch: String? = null,

    @Column(name = "rule_book_info", length = 5000)
    var ruleBookInfo: String? = null,

    @Column(name = "rule_toc", length = 5000)
    var ruleToc: String? = null,

    @Column(name = "rule_content", length = 5000)
    var ruleContent: String? = null,

    @Column(name = "rule_explore", length = 5000)
    var ruleExplore: String? = null,

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
    // ==================== 规则解析方法（新增）====================
    
    fun getSearchRule(): SearchRule {
        return try {
            Gson().fromJson(ruleSearch, SearchRule::class.java) ?: SearchRule()
        } catch (e: Exception) {
            SearchRule()
        }
    }
    
    fun getBookInfoRule(): BookInfoRule {
        return try {
            Gson().fromJson(ruleBookInfo, BookInfoRule::class.java) ?: BookInfoRule()
        } catch (e: Exception) {
            BookInfoRule()
        }
    }
    
    fun getTocRule(): TocRule {
        return try {
            Gson().fromJson(ruleToc, TocRule::class.java) ?: TocRule()
        } catch (e: Exception) {
            TocRule()
        }
    }
    
    fun getContentRule(): ContentRule {
        return try {
            Gson().fromJson(ruleContent, ContentRule::class.java) ?: ContentRule()
        } catch (e: Exception) {
            ContentRule()
        }
    }
    
    fun getExploreRule(): ExploreRule {
        return try {
            Gson().fromJson(ruleExplore, ExploreRule::class.java) ?: ExploreRule()
        } catch (e: Exception) {
            ExploreRule()
        }
    }

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
            "headerJs" to headerJs,
            "ruleSearch" to ruleSearch,
            "ruleBookInfo" to ruleBookInfo,
            "ruleToc" to ruleToc,
            "ruleContent" to ruleContent,
            "ruleExplore" to ruleExplore,
            "charset" to charset,
            "securityRating" to securityRating
        )
        return Gson().toJson(map)
    }

    companion object {
        fun fromJson(json: String): BookSource {
            return Gson().fromJson(json, BookSource::class.java)
        }
    }
}
