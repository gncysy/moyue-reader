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

    fun searchBook(keyword: String, sourceIds: List<String>? = null): List<SearchResult> {
        val sources = if (sourceIds != null) {
            bookSourceRepository.findAllById(sourceIds).filter { it.enabled }
        } else {
            bookSourceRepository.findByEnabledTrue()
        }

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
                    empty emptyList<Book>()
               List<Book>()
                }
            }
            .flatMap { it.stream() }
            .toList()

        return results.map { SearchResult(it, it.source) }
    }

    fun getBookInfo(bookUrl: String, sourceId: String): Book? {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return null
        val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
        val result = rhinoEngine.executeBookInfo(source, book)

        return if (result.success) {
            updateBookFromResult(book, result.result)
            book
        } else null
    }

    fun getChapterList(bookUrl: String, sourceId: String): List<BookChapter> {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return emptyList()
        val cacheKey = "toc:$sourceId:$bookUrl"
        cacheService.get(cacheKey)?.let {
            @Suppress("UNCHECKED_CAST")
            return it as List<BookChapter>
        }

        val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
        val result = rhinoEngine.executeToc(source, book)

        return if (result.success) {
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

        val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
        val chapter = BookChapter(url = chapterUrl, bookUrl = bookUrl)
        val result = rhinoEngine.executeContent(source, book, chapter)

        return if (result.success) {
            val content = parseContent(result.result)
            cacheService.put(cacheKey, content, 86400)
            content
        } else ""
    }

    @Transactional
    fun importSource(json: String): List<BookSource> {
        val type = object : TypeToken<List<BookSource>>() {}.type
        val sources = gson.fromJson<List<BookSource>>(json, type)
        return sources.map { source ->
            source.enabled = true
            bookSourceRepository.save(source)
        }
    }

    fun exportSource(sourceIds: List<String>): String {
        val sources = bookSourceRepository.findAllById(sourceIds)
        return gson.toJson(sources)
    }

    fun testSource(sourceId: String): Map<String, Any> {
        val source = bookSourceRepository.findById(sourceId).orElse(null)
            ?: return mapOf("error" to "书源不存在")
        return rhinoEngine.testSource(source)
    }

    private fun parseBooks(result: Any?, source: BookSource): List<Book> {
        if (result == null) return emptyList()
        
        return try {
            when (result) {
                is String -> {
                    val list = gson.fromJson(result, List::class.java)
                    list?.mapNotNull { item ->
                        if (item is Map<*, *>) {
                            Book(
                                name = (item["name"] as? String)?.trim() ?: "",
                                author = (item["author"] as? String)?.trim() ?: "",
                                bookUrl = (item["bookUrl"] as? String) ?: "",
                                coverUrl = item["coverUrl"] as? String,
                                intro = item["intro"] as? String,
                                origin = source.id,
                                source = source
                            )
                        } else null
                    } ?: emptyList()
                }
                is List<*> -> {
                    result.mapNotNull { item ->
                        if (item is Map<*, *>) {
                            Book(
                                name = (item["name"] as? String)?.trim() ?: "",
                                author = (item["author"] as? String)?.trim() ?: "",
                                bookUrl = (item["bookUrl"] as? String) ?: "",
                                coverUrl = item["coverUrl"] as? String,
                                intro = item["intro"] as? String,
                                origin = source.id,
                                source = source
                            )
                        } else null
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            try {
                val map = gson.fromJson(result.toString(), Map::class.java) as? Map<String, Any>
                if (map != null) {
                    listOf(Book(
                        name = (map["name"] as? String)?.trim() ?: "",
                        author = (map["author"] as? String)?.trim() ?: "",
                        bookUrl = (map["bookUrl"] as? String) ?: "",
                        coverUrl = map["coverUrl"] as? String,
                        intro = map["intro"] as? String,
                        origin = source.id,
                        source = source
                    ))
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseChapters(result: Any?, bookUrl: String): List<BookChapter> {
        if (result == null) return emptyList()
        
        return try {
            val resultList = when (result) {
                is String -> gson.fromJson(result, List::class.java)
                is List<*> -> result
                else -> emptyList()
            }
            
            resultList?.mapIndexed { index, item ->
                if (item is Map<*, *>) {
                    BookChapter(
                        title = (item["title"] as? String 
                            ?: item["chapterName"] as? String 
                            ?: "第${index + 1}章").trim(),
                        url = (item["url"] as? String 
                            ?: item["chapterUrl"] as? String 
                            ?: ""),
                        bookUrl = bookUrl,
                        index = index,
                        isVip = item["isVip"] as? Boolean ?: false,
                        isPay = item["isPay"] as? Boolean ?: false
                    )
                } else null
            }?.filterNotNull() ?: emptyList()
        } catch (e: Exception) {
            try {
                val map = gson.fromJson(result.toString(), Map::class.java) as? Map<String, Any>
                if (map != null) {
                    listOf(BookChapter(
                        title = (map["title"] as? String 
                            ?: map["chapterName"] as? String 
                            ?: "第1章").trim(),
                        url = (map["url"] as? String 
                            ?: map["chapterUrl"] as? String 
                            ?: ""),
                        bookUrl = bookUrl,
                        index = 0
                    ))
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseContent(result: Any?): String {
        return try {
            when (result) {
                is String -> result
                is List<*> -> result.filterIsInstance<String>().joinToString("\n")
                else -> result?.toString() ?: ""
            }
        } catch (e: Exception) {
            result?.toString() ?: ""
        }
    }

    private fun updateBookFromResult(book: Book, result: Any?) {
        if (result == null) return
        try {
            when (result) {
                is String -> {
                    val map = gson.fromJson(result, Map::class.java) as? Map<String, Any>
                    map?.let { updateBookFromMap(book, it) }
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    updateBookFromMap(book, result as Map<String, Any>)
                }
            }
        } catch (e: Exception) {}
    }

    private fun updateBookFromMap(book: Book, map: Map<String, Any>) {
        map["name"]?.let { book.name = it as String }
        map["author"]?.let { book.author = it as String }
        map["coverUrl"]?.let { book.coverUrl = it as? String }
        map["intro"]?.let { book.intro = it as? String }
        map["chapterCount"]?.let { 
            book.chapterCount = (it as? Number)?.toInt() ?: 0 
        }
    }

    data class SearchResult(val book: Book, val source: BookSource)
}
