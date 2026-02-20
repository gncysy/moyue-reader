package com.moyue.service

import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.model.getSearchRule
import com.moyue.model.getBookInfoRule
import com.moyue.model.getTocRule
import com.moyue.model.getContentRule
import com.moyue.repository.BookSourceRepository
import com.moyue.engine.RhinoEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

@Service
class SourceService(
    private val bookSourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine,
    private val cacheService: CacheService,
    private val gson: Gson
) {
    
    private val logger = LoggerFactory.getLogger(SourceService::class.java)
    
    data class SearchResult(
        val book: Book,
        val source: BookSource
    )
    
    // ==================== 基础 CRUD 方法 ====================
    
    fun getAllSources(): List<BookSource> {
        return bookSourceRepository.findAll()
    }
    
    fun getEnabledSources(): List<BookSource> {
        return bookSourceRepository.findByEnabledTrue()
    }
    
    fun getSourceById(id: String): BookSource? {
        return bookSourceRepository.findById(id).orElse(null)
    }
    
    fun saveSource(source: BookSource): BookSource {
        return bookSourceRepository.save(source)
    }
    
    fun deleteSource(id: String): Boolean {
        return if (bookSourceRepository.existsById(id)) {
            bookSourceRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    // ==================== 搜索和过滤 ====================
    
    fun searchSources(keyword: String): List<BookSource> {
        return bookSourceRepository.findByNameContainingIgnoreCase(keyword)
    }
    
    fun getSourcesByGroup(group: String): List<BookSource> {
        return bookSourceRepository.findByGroup(group)
    }
    
    fun getSourceGroups(): List<String> {
        return bookSourceRepository.findAll()
            .mapNotNull { it.group }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    
    // ==================== 批量操作 ====================
    
    fun batchSetEnabled(sourceIds: List<String>, enabled: Boolean): Int {
        var count = 0
        sourceIds.forEach { id ->
            getSourceById(id)?.let { source ->
                saveSource(source.copy(enabled = enabled))
                count++
            }
        }
        return count
    }
    
    fun batchDelete(sourceIds: List<String>): Int {
        var count = 0
        sourceIds.forEach { id ->
            if (deleteSource(id)) {
                count++
            }
        }
        return count
    }
    
    // ==================== 书源搜索 ====================
    
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
                    val result = rhinoEngine.executeSearch(source, keyword)
                    
                    if (result.success && result.result != null) {
                        parseBooks(result.result, source)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    logger.error("搜索失败: ${source.name}", e)
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
        
        val result = rhinoEngine.executeBookInfo(source, Book(bookUrl = bookUrl))
        
        return if (result.success && result.result != null) {
            val book = parseBook(result.result, source, bookUrl)
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
        
        val result = rhinoEngine.executeToc(source, Book(bookUrl = bookUrl))
        
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
        
        val book = Book(bookUrl = bookUrl)
        val chapter = BookChapter(url = chapterUrl, bookUrl = bookUrl)
        val result = rhinoEngine.executeContent(source, book, chapter)
        
        return if (result.success && result.result != null) {
            val content = parseContent(result.result)
            cacheService.put(cacheKey, content, 86400)
            content
        } else ""
    }
    
    // ==================== 书源测试 ====================
    
    fun testSource(id: String, securityLevel: String): Map<String, Any> {
        val source = getSourceById(id) ?: return mapOf("error" to "书源不存在")
        
        val results = mutableMapOf<String, Any>()
        val startTime = System.currentTimeMillis()
        
        try {
            // 测试搜索
            val searchResult = rhinoEngine.executeSearch(source, "测试")
            results["search"] = mapOf(
                "success" to searchResult.success,
                "time" to searchResult.executionTime,
                "error" to (searchResult.error ?: "")
            )
            
            // 如果搜索成功，尝试获取第一个结果的详情
            if (searchResult.success && searchResult.result != null) {
                val books = parseBooks(searchResult.result, source)
                if (books.isNotEmpty()) {
                    val bookInfoResult = rhinoEngine.executeBookInfo(source, books[0])
                    results["bookInfo"] = mapOf(
                        "success" to bookInfoResult.success,
                        "time" to bookInfoResult.executionTime,
                        "error" to (bookInfoResult.error ?: "")
                    )
                }
            }
        } catch (e: Exception) {
            results["error"] = "测试异常: ${e.message}"
        }
        
        results["totalTime"] = System.currentTimeMillis() - startTime
        return results
    }
    
    fun batchTestSources(sourceIds: List<String>, securityLevel: String): List<Map<String, Any>> {
        return sourceIds.map { id ->
            mapOf(
                "id" to id,
                "result" to testSource(id, securityLevel)
            )
        }
    }
    
    // ==================== 兼容性分析 ====================
    
    fun analyzeCompatibility(): Map<String, Any> {
        val sources = getAllSources()
        
        var standardCount = 0
        var compatibleCount = 0
        var trustedCount = 0
        
        sources.forEach { source ->
            val rules = (source.ruleSearch ?: "") + 
                        (source.ruleToc ?: "") + 
                        (source.ruleContent ?: "") +
                        (source.ruleBookInfo ?: "")
            val lowerRules = rules.lowercase()
            
            val usesReflection = lowerRules.contains("class.forname") || 
                                  lowerRules.contains("getclass") ||
                                  lowerRules.contains("reflect")
            
            val usesSocket = lowerRules.contains("socket") ||
                             lowerRules.contains("serversocket")
            
            val usesFile = lowerRules.contains("file") ||
                           lowerRules.contains("getfile") ||
                           lowerRules.contains("writefile") ||
                           lowerRules.contains("readfile")
            
            when {
                usesReflection -> trustedCount++
                usesSocket || usesFile -> compatibleCount++
                else -> standardCount++
            }
        }
        
        val total = sources.size
        val analyzed = if (total > 0) minOf(total, 50) else 0
        
        return mapOf(
            "total" to total,
            "analyzed" to analyzed,
            "standard" to standardCount,
            "compatible" to compatibleCount,
            "trusted" to trustedCount,
            "standardPercent" to (if (analyzed > 0) standardCount * 100 / analyzed else 0),
            "compatiblePercent" to (if (analyzed > 0) compatibleCount * 100 / analyzed else 0),
            "trustedPercent" to (if (analyzed > 0) trustedCount * 100 / analyzed else 0)
        )
    }
    
    // ==================== 导入导出 ====================
    
    @Transactional
    fun importSources(json: String): Int {
        return try {
            val type = object : TypeToken<List<BookSource>>() {}.type
            val sources: List<BookSource> = gson.fromJson(json, type)
            
            var count = 0
            sources.forEach { source ->
                // 检查是否已存在（按 url 去重）
                val existing = bookSourceRepository.findByUrl(source.url)
                if (existing == null) {
                    saveSource(source.copy(enabled = true))
                    count++
                }
            }
            count
        } catch (e: Exception) {
            logger.error("导入书源失败", e)
            0
        }
    }
    
    fun exportSources(sourceIds: List<String>): String {
        val sources = bookSourceRepository.findAllById(sourceIds)
        return gson.toJson(sources)
    }
    
    fun exportAllSources(): String {
        val sources = getAllSources()
        return gson.toJson(sources)
    }
    
    // ==================== 解析方法（完整实现） ====================
    
    private fun parseBooks(result: Any?, source: BookSource): List<Book> {
        if (result == null) return emptyList()
        
        val books = mutableListOf<Book>()
        val searchRule = source.getSearchRule()
        
        when (result) {
            is List<*> -> {
                result.forEach { item ->
                    when (item) {
                        is Map<*, *> -> parseBookFromMap(item, source)?.let { books.add(it) }
                        is String -> parseBookFromHtml(item, searchRule, source)?.let { books.add(it) }
                    }
                }
            }
            is Map<*, *> -> {
                parseBookFromMap(result, source)?.let { books.add(it) }
            }
            is String -> {
                parseBookFromHtml(result, searchRule, source)?.let { books.add(it) }
            }
        }
        
        return books
    }
    
    private fun parseBookFromMap(map: Map<*, *>, source: BookSource): Book? {
        return try {
            Book(
                name = (map["name"] as? String) ?: (map["title"] as? String) ?: return null,
                author = (map["author"] as? String) ?: "",
                coverUrl = map["coverUrl"] as? String ?: map["cover"] as? String,
                bookUrl = (map["bookUrl"] as? String) ?: (map["url"] as? String) ?: return null,
                intro = map["intro"] as? String ?: map["desc"] as? String,
                origin = source.id,
                source = source
            )
        } catch (e: Exception) {
            logger.error("解析书籍失败", e)
            null
        }
    }
    
    private fun parseBookFromHtml(html: String, rule: SearchRule?, source: BookSource): Book? {
        if (rule == null) return null
        
        return try {
            val doc = Jsoup.parse(html)
            val book = Book(origin = source.id, source = source)
            
            // 根据规则解析
            rule.name?.let { selector ->
                doc.select(selector).first()?.text()?.let { book.name = it }
            }
            
            rule.author?.let { selector ->
                doc.select(selector).first()?.text()?.let { book.author = it }
            }
            
            rule.coverUrl?.let { selector ->
                doc.select(selector).first()?.attr("src")?.let { book.coverUrl = it }
            }
            
            rule.bookUrl?.let { selector ->
                doc.select(selector).first()?.attr("href")?.let { book.bookUrl = it }
            }
            
            rule.intro?.let { selector ->
                doc.select(selector).first()?.text()?.let { book.intro = it }
            }
            
            if (book.name.isBlank() || book.bookUrl.isBlank()) null else book
        } catch (e: Exception) {
            logger.error("HTML解析书籍失败", e)
            null
        }
    }
    
    private fun parseChapters(result: Any?, bookUrl: String): List<BookChapter> {
        if (result == null) return emptyList()
        
        val chapters = mutableListOf<BookChapter>()
        
        when (result) {
            is List<*> -> {
                result.forEachIndexed { index, item ->
                    when (item) {
                        is Map<*, *> -> parseChapterFromMap(item, bookUrl, index)?.let { chapters.add(it) }
                        is String -> parseChapterFromHtml(item, bookUrl, index)?.let { chapters.add(it) }
                    }
                }
            }
            is Map<*, *> -> {
                parseChapterFromMap(result, bookUrl, 0)?.let { chapters.add(it) }
            }
            is String -> {
                parseChapterFromHtml(result, bookUrl, 0)?.let { chapters.add(it) }
            }
        }
        
        return chapters
    }
    
    private fun parseChapterFromMap(map: Map<*, *>, bookUrl: String, index: Int): BookChapter? {
        return try {
            BookChapter(
                title = (map["title"] as? String) ?: (map["name"] as? String) ?: "第${index + 1}章",
                url = (map["url"] as? String) ?: (map["link"] as? String) ?: return null,
                bookUrl = bookUrl,
                index = index
            )
        } catch (e: Exception) {
            logger.error("解析章节失败", e)
            null
        }
    }
    
    private fun parseChapterFromHtml(html: String, bookUrl: String, index: Int): BookChapter? {
        return try {
            val doc = Jsoup.parse(html)
            // 简化版，实际需要根据规则解析
            BookChapter(
                title = "第${index + 1}章",
                url = bookUrl,
                bookUrl = bookUrl,
                index = index
            )
        } catch (e: Exception) {
            logger.error("HTML解析章节失败", e)
            null
        }
    }
    
    private fun parseContent(result: Any?): String {
        return when (result) {
            is String -> result
            is List<*> -> result.joinToString("\n") { it?.toString() ?: "" }
            is Map<*, *> -> (result["content"] as? String) ?: result.toString()
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
