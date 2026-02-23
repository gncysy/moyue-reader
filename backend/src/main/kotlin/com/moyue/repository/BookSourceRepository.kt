package com.moyue.repository
 
import com.moyue.model.BookSource
import com.moyue.model.tables.BookSources
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
 
/**
 * 书源数据访问层
 */
class BookSourceRepository {
    
    fun findAll(): List<BookSource> = transaction {
        BookSources.selectAll()
            .orderBy(BookSources.weight to SortOrder.DESC)
            .map { it.toBookSource() }
    }
    
    fun findById(id: String): BookSource? = transaction {
        BookSources.select { BookSources.id eq id }
            .singleOrNull()
            ?.toBookSource()
    }
    
    fun findByEnabled(enabled: Boolean): List<BookSource> = transaction {
        BookSources.select { BookSources.enabled eq enabled }
            .orderBy(BookSources.weight to SortOrder.DESC)
            .map { it.toBookSource() }
    }
    
    fun save(source: BookSource): BookSource = transaction {
        val now = LocalDateTime.now()
        
        BookSources.insert {
            it[id] = source.id
            it[name] = source.name
            it[url] = source.url
            it[group] = source.group
            it[enabled] = source.enabled
            it[enableJs] = source.enableJs
            it[concurrent] = source.concurrent
            it[weight] = source.weight
            it[loginUrl] = source.loginUrl
            it[loginCheckJs] = source.loginCheckJs
            it[headerJs] = source.headerJs
            it[searchUrl] = source.searchUrl
            it[ruleSearch] = source.ruleSearch
            it[ruleBookInfo] = source.ruleBookInfo
            it[ruleToc] = source.ruleToc
            it[ruleContent] = source.ruleContent
            it[ruleExplore] = source.ruleExplore
            it[charset] = source.charset
            it[securityRating] = source.securityRating
            it[lastUsed] = source.lastUsed
            it[failCount] = source.failCount
            it[createdAt] = now
            it[updatedAt] = now
        }
        
        source.copy(updatedAt = now)
    }
    
    fun update(source: BookSource): BookSource = transaction {
        BookSources.update({ BookSources.id eq source.id }) {
            it[name] = source.name
            it[url] = source.url
            it[group] = source.group
            it[enabled] = source.enabled
            it[enableJs] = source.enableJs
            it[concurrent] = source.concurrent
            it[weight] = source.weight
            it[loginUrl] = source.loginUrl
            it[loginCheckJs] = source.loginCheckJs
            it[headerJs] = source.headerJs
            it[searchUrl] = source.searchUrl
            it[ruleSearch] = source.ruleSearch
            it[ruleBookInfo] = source.ruleBookInfo
            it[ruleToc] = source.ruleToc
            it[ruleContent] = source.ruleContent
            it[ruleExplore] = source.ruleExplore
            it[charset] = source.charset
            it[securityRating] = source.securityRating
            it[lastUsed] = source.lastUsed
            it[failCount] = source.failCount
            it[updatedAt] = LocalDateTime.now()
        }
        
        source.copy(updatedAt = LocalDateTime.now())
    }
    
    fun delete(id: String): Boolean = transaction {
        BookSources.deleteWhere { BookSources.id eq id } > 0
    }
    
    private fun ResultRow.toBookSource() = BookSource(
        id = this[BookSources.id].toString(),
        name = this[BookSources.name],
        url = this[BookSources.url],
        group = this[BookSources.group],
        enabled = this[BookSources.enabled],
        enableJs = this[BookSources.enableJs],
        concurrent = this[BookSources.concurrent],
        weight = this[BookSources.weight],
        loginUrl = this[BookSources.loginUrl],
        loginCheckJs = this[BookSources.loginCheckJs],
        headerJs = this[BookSources.headerJs],
        searchUrl = this[BookSources.searchUrl],
        ruleSearch = this[BookSources.ruleSearch],
        ruleBookInfo = this[BookSources.ruleBookInfo],
        ruleToc = this[BookSources.ruleToc],
        ruleContent = this[BookSources.ruleContent],
        ruleExplore = this[BookSources.ruleExplore],
        charset = this[BookSources.charset],
        securityRating = this[BookSources.securityRating],
        lastUsed = this[BookSources.lastUsed],
        failCount = this[BookSources.failCount],
        createdAt = this[BookSources.createdAt],
        updatedAt = this[BookSources.updatedAt]
    )
}
