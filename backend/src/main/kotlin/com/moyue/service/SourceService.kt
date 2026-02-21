package com.moyue.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.source.engine.RhinoEngine
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

        // 并发搜索，最多同时 3 个（避免被封）
        val results = sources.parallelStream()
            .limit(3)
            .map { source ->
                try {
                    val engineResult = rhinoEngine.executeSearch(source, keyword)
                    if (engineResult.success) {
                        val books = parseBooks(engineResult.result, source)
                        books.map { SearchResult(it, source) }
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            .flatMap { it.stream() }
            .toList()

        return results
    }

    /**
     * 获取书籍详情
     */
    fun getBookInfo(bookUrl: String, sourceId: String): Book? {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return null

        // 检查缓存
        val cacheKey = "bookInfo:$sourceId:$bookUrl"
        cacheService.get(cacheKey)?.let {
            return it as Book
        }

        val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
        val result = rhinoEngine.executeBookInfo(source, book)

        return if (result.success) {
            updateBookFromResult(book, result.result)
            cacheService.put(cacheKey, book, 3600) // 缓存1小时
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

        val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
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

        val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
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
            source.failCount = 0
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

    // ==================== 解析方法实现 ====================

    /**
     * 解析搜索结果
     */
    private fun parseBooks(result: Any?, source: BookSource): List<Book> {
        if (result == null) return emptyList()
        
        return try {
            when (result) {
                is List<*> -> {
                    result.filterIsInstance<Map<String, Any>>().map { map ->
                        Book(
                            name = map["name"] as? String ?: "",
                            author = map["author"] as? String ?: "",
                            coverUrl = map["coverUrl"] as? String,
                            intro = map["intro"] as? String,
                            bookUrl = map["bookUrl"] as? String ?: "",
                            origin = source.id,
                            source = source,
                            lastReadAt = null
                        )
                    }
                }
                is String -> {
                    // 尝试解析 JSON 字符串
                    val list = gson.fromJson(result, List::class.java)
                    parseBooks(list, source)
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 解析目录
     */
    private fun parseChapters(result: Any?, bookUrl: String): List<BookChapter> {
        if (result == null) return emptyList()
        
        return try {
            when (result) {
                is List<*> -> {
                    result.filterIsInstance<Map<String, Any>>().mapIndexed { index, map ->
                        BookChapter(
                            title = map["title"] as? String ?: map["name"] as? String ?: "未知章节",
                            url = map["url"] as? String ?: map["chapterUrl"] as? String ?: "",
                            bookUrl = bookUrl,
                            index = map["index"] as? Int ?: index,
                            isRead = false
                        )
                    }
                }
                is String -> {
                    val list = gson.fromJson(result, List::class.java)
                    parseChapters(list, bookUrl)
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 解析正文内容
     */
    private fun parseContent(result: Any?): String {
        return when (result) {
            is String -> result
            is List<*> -> result.joinToString("\n")
            else -> result?.toString() ?: ""
        }
    }

    /**
     * 更新书籍信息
     */
    private fun updateBookFromResult(book: Book, result: Any?) {
        if (result !is Map<*, *>) return
        
        book.name = result["name"] as? String ?: book.name
        book.author = result["author"] as? String ?: book.author
        book.coverUrl = result["coverUrl"] as? String ?: book.coverUrl
        book.intro = result["intro"] as? String ?: book.intro
    }

    data class SearchResult(val book: Book, val source: BookSource)
}
