package com.moyue.service

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.source.engine.RhinoEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SourceService(
    private val bookSourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine,
    private val cacheService: CacheService,
    private val gson: Gson
) {
    
    data class SearchResult(
        val book: Book,
        val source: BookSource
    )
    
    fun search(keyword: String, sourceIds: List<String>? = null): List<SearchResult> {
        val sources = if (sourceIds != null) {
            bookSourceRepository.findAllById(sourceIds).filter { it.enabled }
        } else {
            bookSourceRepository.findByEnabledTrue()
        }
        
        return sources.parallelStream()
            .limit(5)
            .map { source ->
                try {
                    val jsCode = buildString {
                        appendLine(source.headerJs ?: "")
                        appendLine("var keyword = \"${escapeJsString(keyword)}\";")
                        appendLine("var result = ${source.ruleSearch ?: "null"};")
                        appendLine("JSON.stringify(result);")
                    }
                    
                    val result = rhinoEngine.execute(
                        jsCode = jsCode,
                        source = source
                    )
                    
                    if (result.success && result.result != null) {
                        parseBooks(result.result, source)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .flatMap { it.stream() }
            .toList()
            .map { SearchResult(it, it.source ?: BookSource()) }
    }
    
    fun getBookInfo(bookUrl: String, sourceId: String): Book? {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return null
        
        val cacheKey = "book:$sourceId:$bookUrl"
        cacheService.get(cacheKey)?.let {
            return it as Book
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var bookUrl = \"${escapeJsString(bookUrl)}\";")
            appendLine("var result = ${source.ruleBookInfo ?: "null"};")
            appendLine("JSON.stringify(result);")
        }
        
        val result = rhinoEngine.execute(
            jsCode = jsCode,
            source = source
        )
        
        return if (result.success && result.result != null) {
            val book = parseBook(result.result, source)
            cacheService.put(cacheKey, book, 3600)
            book
        } else null
    }
    
    fun getChapterList(bookUrl: String, sourceId: String): List<BookChapter> {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return emptyList()
        
        val cacheKey = "toc:$sourceId:$bookUrl"
        cacheService.get(cacheKey)?.let {
            return it as List<BookChapter>
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var bookUrl = \"${escapeJsString(bookUrl)}\";")
            appendLine("var result = ${source.ruleToc ?: "null"};")
            appendLine("JSON.stringify(result);")
        }
        
        val result = rhinoEngine.execute(
            jsCode = jsCode,
            source = source
        )
        
        return if (result.success && result.result != null) {
            val chapters = parseChapters(result.result, bookUrl)
            cacheService.put(cacheKey, chapters, 3600)
            chapters
        } else emptyList()
    }
    
    fun getChapterContent(chapterUrl: String, bookUrl: String, sourceId: String): String {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return ""
        
        val cacheKey = "content:$sourceId:$chapterUrl"
        cacheService.get(cacheKey)?.let {
            return it as String
        }
        
        val jsCode = buildString {
            appendLine(source.headerJs ?: "")
            appendLine("var chapterUrl = \"${escapeJsString(chapterUrl)}\";")
            appendLine("var result = ${source.ruleContent ?: "null"};")
            appendLine("JSON.stringify(result);")
        }
        
        val result = rhinoEngine.execute(
            jsCode = jsCode,
            source = source
        )
        
        return if (result.success && result.result != null) {
            val content = parseContent(result.result)
            cacheService.put(cacheKey, content, 86400)
            content
        } else ""
    }
    
    @Transactional
    fun importSources(json: String): List<BookSource> {
        val type = object : TypeToken<List<BookSource>>() {}.type
        val sources = gson.fromJson<List<BookSource>>(json, type)
        
        return sources.map { source ->
            source.enabled = true
            bookSourceRepository.save(source)
        }
    }
    
    fun exportSources(sourceIds: List<String>): String {
        val sources = bookSourceRepository.findAllById(sourceIds)
        return gson.toJson(sources)
    }
    
    fun testSource(sourceId: String): Map<String, Any> {
        val source = bookSourceRepository.findById(sourceId).orElse(null) 
            ?: return mapOf("error" to "书源不存在")
        
        val results = mutableMapOf<String, Any>()
        
        // 测试搜索
        val searchResult = rhinoEngine.execute(
            jsCode = "var keyword='测试'; var result=search?search:null; JSON.stringify(result);",
            source = source
        )
        results["search"] = mapOf(
            "success" to searchResult.success,
            "time" to searchResult.executionTime,
            "error" to (searchResult.error ?: "")
        )
        
        return results
    }
    
    private fun parseBooks(result: Any?, source: BookSource): List<Book> {
        return when (result) {
            is List<*> -> result.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        Book(
                            name = item["name"] as? String ?: "",
                            author = item["author"] as? String ?: "",
                            bookUrl = item["bookUrl"] as? String ?: "",
                            coverUrl = item["coverUrl"] as? String,
                            intro = item["intro"] as? String,
                            origin = source.id
                        ).apply { this.source = source }
                    }
                    else -> null
                }
            }
            is Map<*, *> -> listOf(Book(
                name = result["name"] as? String ?: "",
                author = result["author"] as? String ?: "",
                bookUrl = result["bookUrl"] as? String ?: "",
                coverUrl = result["coverUrl"] as? String,
                intro = result["intro"] as? String,
                origin = source.id
            ).apply { this.source = source })
            else -> emptyList()
        }
    }
    
    private fun parseBook(result: Any?, source: BookSource): Book {
        return when (result) {
            is Map<*, *> -> Book(
                name = result["name"] as? String ?: "",
                author = result["author"] as? String ?: "",
                bookUrl = result["bookUrl"] as? String ?: "",
                coverUrl = result["coverUrl"] as? String,
                intro = result["intro"] as? String,
                origin = source.id
            ).apply { this.source = source }
            else -> Book()
        }
    }
    
    private fun parseChapters(result: Any?, bookUrl: String): List<BookChapter> {
        return when (result) {
            is List<*> -> result.mapIndexedNotNull { index, item ->
                when (item) {
                    is Map<*, *> -> BookChapter(
                        title = item["title"] as? String ?: "第${index + 1}章",
                        url = item["url"] as? String ?: "",
                        bookUrl = bookUrl,
                        index = index
                    )
                    else -> null
                }
            }
            else -> emptyList()
        }
    }
    
    private fun parseContent(result: Any?): String {
        return when (result) {
            is String -> result
            is List<*> -> result.joinToString("\n") { it?.toString() ?: "" }
            else -> result?.toString() ?: ""
        }
    }
    
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
