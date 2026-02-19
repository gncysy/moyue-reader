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
import java.io.File

@Service
class SourceService(
    private val bookSourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine,
    private val cacheService: CacheService,
    private val gson: Gson
) {
    
    /**
     * 搜索书籍（多书源并发）
     */
    fun searchBook(keyword: String, sourceIds: List<String>? = null): List<SearchResult> {
        val sources = if (sourceIds != null) {
            bookSourceRepository.findAllById(sourceIds).filter { it.enabled }
        } else {
            bookSourceRepository.findByEnabledTrue()
        }
        
        // 并发搜索，最多同时 5 个
        val results = sources.parallelStream()
            .limit(5)
            .map { source ->
                try {
                    val engineResult = rhinoEngine.executeSearch(source, keyword)
                    if (engineResult.success) {
                        parseBooks(engineResult.result, source)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .flatMap { it.stream() }
            .toList()
        
        return results.map { SearchResult(it, it.source) }
    }
    
    /**
     * 获取书籍详情
     */
    fun getBookInfo(bookUrl: String, sourceId: String): Book? {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return null
        
        val book = Book(bookUrl = bookUrl, origin = sourceId)
        val result = rhinoEngine.executeBookInfo(source, book)
        
        return if (result.success) {
            updateBookFromResult(book, result.result)
            book
        } else null
    }
    
    /**
     * 获取目录
     */
    fun getChapterList(bookUrl: String, sourceId: String): List<BookChapter> {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return emptyList()
        
        // 检查缓存
        val cacheKey = "toc:$sourceId:$bookUrl"
        cacheService.get(cacheKey)?.let {
            return it as List<BookChapter>
        }
        
        val book = Book(bookUrl = bookUrl, origin = sourceId)
        val result = rhinoEngine.executeToc(source, book)
        
        return if (result.success) {
            val chapters = parseChapters(result.result, bookUrl)
            cacheService.put(cacheKey, chapters, 3600) // 缓存1小时
            chapters
        } else emptyList()
    }
    
    /**
     * 获取正文
     */
    fun getChapterContent(chapterUrl: String, bookUrl: String, sourceId: String): String {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return ""
        
        // 检查缓存
        val cacheKey = "content:$sourceId:$chapterUrl"
        cacheService.get(cacheKey)?.let {
            return it as String
        }
        
        val book = Book(bookUrl = bookUrl, origin = sourceId)
        val chapter = BookChapter(url = chapterUrl, bookUrl = bookUrl)
        val result = rhinoEngine.executeContent(source, book, chapter)
        
        return if (result.success) {
            val content = parseContent(result.result)
            cacheService.put(cacheKey, content, 86400) // 缓存24小时
            content
        } else ""
    }
    
    /**
     * 导入书源
     */
    @Transactional
    fun importSource(json: String): List<BookSource> {
        val type = object : TypeToken<List<BookSource>>() {}.type
        val sources = gson.fromJson<List<BookSource>>(json, type)
        
        // 验证并保存
        return sources.map { source ->
            source.enabled = true
            bookSourceRepository.save(source)
        }
    }
    
    /**
     * 导出书源
     */
    fun exportSource(sourceIds: List<String>): String {
        val sources = bookSourceRepository.findAllById(sourceIds)
        return gson.toJson(sources)
    }
    
    /**
     * 测试书源
     */
    fun testSource(sourceId: String): Map<String, Any> {
        val source = bookSourceRepository.findById(sourceId).orElse(null) 
            ?: return mapOf("error" to "书源不存在")
        
        return rhinoEngine.testSource(source)
    }
    
    // 辅助方法
    private fun parseBooks(result: Any?, source: BookSource): List<Book> {
        // 实现解析逻辑
        return emptyList()
    }
    
    private fun parseChapters(result: Any?, bookUrl: String): List<BookChapter> {
        // 实现解析逻辑
        return emptyList()
    }
    
    private fun parseContent(result: Any?): String {
        return when (result) {
            is String -> result
            is List<*> -> result.joinToString("\n")
            else -> result?.toString() ?: ""
        }
    }
    
    private fun updateBookFromResult(book: Book, result: Any?) {
        // 更新书籍信息
    }
    
    data class SearchResult(val book: Book, val source: BookSource)
}
