package com.moyue.model.tables
 
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
 
/**
 * 书源表
 */
object BookSources : UUIDTable("book_sources") {
    
    val name = varchar("name", 255)
    val url = varchar("url", 500)
    val group = varchar("group", 100).nullable()
    val enabled = bool("enabled").default(true)
    val enableJs = bool("enable_js").default(true)
    val concurrent = integer("concurrent").default(1)
    val weight = integer("weight").default(0)
    val loginUrl = varchar("login_url", 1000).nullable()
    val loginCheckJs = text("login_check_js").nullable()
    val headerJs = text("header_js").nullable()
    val searchUrl = varchar("search_url", 2000).nullable()
    val ruleSearch = text("rule_search").nullable()
    val ruleBookInfo = text("rule_book_info").nullable()
    val ruleToc = text("rule_toc").nullable()
    val ruleContent = text("rule_content").nullable()
    val ruleExplore = text("rule_explore").nullable()
    val charset = varchar("charset", 20).default("UTF-8")
    val securityRating = integer("security_rating").default(5)
    val lastUsed = datetime("last_used").nullable()
    val failCount = integer("fail_count").default(0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
 
/**
 * 书源规则表
 */
object BookSourceRules : UUIDTable("book_source_rules") {
    
    val sourceId = reference("source_id", BookSources)
    val ruleType = varchar("rule_type", 50) // search, bookInfo, toc, content, explore
    val ruleContent = text("rule_content")
    val enabled = bool("enabled").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
