package com.moyue.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moyue.model.Book
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.source.engine.RhinoEngine
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Service
@Transactional
class SourceService(
    private val bookSourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine,
    private val cacheService: CacheService,
    private val gson: Gson
) {

    companion object {
        private const val DEFAULT_SEARCH_CONCURRENT = 3
        private const val MAX_SEARCH_CONCURRENT = 10
        private const val BOOK_INFO_CACHE_TTL = 3600L // 1小时
        private const val TOC_CACHE_TTL = 3600L // 1小时
        private const val CONTENT_CACHE_TTL = 86400L // 24小时
        private const val SEARCH_RESULT_CACHE_TTL = 300L // 5分钟
        private const val MAX_FAIL_COUNT = 10
        private const val CHAPTER_RATE_LIMIT = 60 // 每分钟最多60次
    }

    private val logger = LoggerFactory.getLogger(SourceService::class.java)
    
    // 并发控制：每个书源的请求限流
    private val requestRateLimiter = ConcurrentHashMap<String, AtomicInteger>()
    
    // 搜索结果缓存
    private val searchResultCache = ConcurrentHashMap<String, Pair<List<SearchResult>, Long>>()
    
    // 书源状态更新锁
    private val sourceUpdateLock = ConcurrentHashMap<String, Any>()
    
    /**
     * 搜索书籍（多书源并发）
     * @param keyword 搜索关键词
     * @param sourceIds 指定书源ID列表，null 表示搜索所有启用书源
     * @param maxConcurrent 最大并发数
     */
    fun searchBook(
        keyword: String,
        sourceIds: List<String>? = null,
        maxConcurrent: Int = DEFAULT_SEARCH_CONCURRENT
    ): List<SearchResult> {
        if (keyword.isBlank()) {
            return emptyList()
        }
        
        // 检查缓存
        val cacheKey = "search:${keyword}:${sourceIds?.hashCode() ?: "all"}"
        val cached = searchResultCache[cacheKey]
        if (cached != null && cached.second > System.currentTimeMillis()) {
            logger.debug("使用搜索缓存: $keyword")
            return cached.first
        }
        
        // 获取可用书源
        val sources = if (sourceIds != null) {
            bookSourceRepository.findAllById(sourceIds).filter { it.isEnabled() }
        } else {
            bookSourceRepository.findByEnabledTrue().filter { it.isEnabled() }
        }
        
        if (sources.isEmpty()) {
            logger.warn("没有可用的书源进行搜索: $keyword")
            return emptyList()
        }
        
        // 限制并发数
        val concurrent = maxConcurrent.coerceIn(1, MAX_SEARCH_CONCURRENT)
        logger.info("开始搜索书籍: $keyword, 书源数量: ${sources.size}, 并发数: $concurrent")
        
        // 并发搜索
        val results = sources.parallelStream()
            .limit(concurrent.toLong())
            .map { source ->
                searchInSource(source, keyword)
            }
            .flatMap { it.stream() }
            .toList()
        
        // 缓存结果
        searchResultCache[cacheKey] = results to (System.currentTimeMillis() + SEARCH_RESULT_CACHE_TTL * 1000)
        
        logger.info("搜索完成: $keyword, 结果数量: ${results.size}")
        return results
    }
    
    /**
     * 在单个书源中搜索
     */
    private fun searchInSource(source: BookSource, keyword: String): List<SearchResult> {
        return try {
            val lock = sourceUpdateLock.getOrPut(source.id!!) { Any() }
            synchronized(lock) {
                val engineResult = rhinoEngine.executeSearch(source, keyword)
                
                if (engineResult.success) {
                    val books = parseBooks(engineResult.result, source)
                    updateSourceSuccess(source)
                    books.map { SearchResult(it, source) }
                } else {
                    updateSourceFailure(source, "搜索失败: ${engineResult.error}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("书源搜索异常: ${source.name}", e)
            updateSourceFailure(source, "搜索异常: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 获取书籍详情
     */
    fun getBookInfo(bookUrl: String, sourceId: String): Book? {
        if (bookUrl.isBlank() || sourceId.isBlank()) {
            return null
        }
        
        val source = bookSourceRepository.findById(sourceId).orElse(null)
            ?: run {
                logger.warn("书源不存在: $sourceId")
                return null
            }
        
        if (!source.isEnabled()) {
            logger.warn("书源未启用: ${source.name}")
            return null
        }
        
        // 检查缓存（使用唯一的缓存键）
        val cacheKey = "bookInfo:${sourceId}:${bookUrl.hashCode()}"
        cacheService.getPersistent(cacheKey)?.let {
            logger.debug("使用书籍信息缓存: $bookUrl")
            return it as Book
        }
        
        try {
            val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
            val result = rhinoEngine.executeBookInfo(source, book)
            
            return if (result.success) {
                updateBookFromResult(book, result.result)
                updateSourceSuccess(source)
                cacheService.putPersistent(cacheKey, book, BOOK_INFO_CACHE_TTL)
                logger.info("获取书籍详情: ${book.name}")
                book
            } else {
                updateSourceFailure(source, "获取书籍详情失败: ${result.error}")
                null
            }
        } catch (e: Exception) {
            logger.error("获取书籍详情异常: $bookUrl", e)
            updateSourceFailure(source, "获取书籍详情异常: ${e.message}")
            null
        }
    }
    
    /**
     * 获取目录
     */
    fun getChapterList(bookUrl: String, sourceId: String): List<BookChapter> {
        if (bookUrl.isBlank() || sourceId.isBlank()) {
            return emptyList()
        }
        
        val source = bookSourceRepository.findById(sourceId).orElse(null)
            ?: run {
                logger.warn("书源不存在: $sourceId")
                return emptyList()
            }
        
        if (!source.isEnabled()) {
            logger.warn("书源未启用: ${source.name}")
            return emptyList()
        }
        
        // 检查缓存
        val cacheKey = "toc:${sourceId}:${bookUrl.hashCode()}"
        cacheService.getPersistent(cacheKey)?.let {
            logger.debug("使用目录缓存: $bookUrl")
            return it as List<BookChapter>
        }
        
        try {
            val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
            val result = rhinoEngine.executeToc(source, book)
            
            return if (result.success) {
                val chapters = parseChapters(result.result, bookUrl)
                updateSourceSuccess(source)
                cacheService.putPersistent(cacheKey, chapters, TOC_CACHE_TTL)
                logger.info("获取目录: ${bookUrl}, 章节数量: ${chapters.size}")
                chapters
            } else {
                updateSourceFailure(source, "获取目录失败: ${result.error}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("获取目录异常: $bookUrl", e)
            updateSourceFailure(source, "获取目录异常: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 获取正文（带限流）
     */
    fun getChapterContent(chapterUrl: String, bookUrl: String, sourceId: String): String {
        if (chapterUrl.isBlank() || sourceId.isBlank()) {
            return ""
        }
        
        // 限流检查
        if (!checkRateLimit(sourceId)) {
            logger.warn("请求过于频繁: $sourceId")
            return ""
        }
        
        val source = bookSourceRepository.findById(sourceId).orElse(null)
            ?: run {
                logger.warn("书源不存在: $sourceId")
                return ""
            }
        
        if (!source.isEnabled()) {
            logger.warn("书源未启用: ${source.name}")
            return ""
        }
        
        // 检查缓存
        val cacheKey = "content:${sourceId}:${chapterUrl.hashCode()}"
        cacheService.getContent(cacheKey)?.let {
            logger.debug("使用章节内容缓存: $chapterUrl")
            return it
        }
        
        try {
            val book = Book(bookUrl = bookUrl, origin = sourceId, source = source)
            val chapter = BookChapter(url = chapterUrl, bookUrl = bookUrl)
            val result = rhinoEngine.executeContent(source, book, chapter)
            
            return if (result.success) {
                val content = parseContent(result.result)
                updateSourceSuccess(source)
                cacheService.putContent(cacheKey, content)
                logger.debug("获取章节内容: $chapterUrl")
                content
            } else {
                updateSourceFailure(source, "获取章节内容失败: ${result.error}")
                ""
            }
        } catch (e: Exception) {
            logger.error("获取章节内容异常: $chapterUrl", e)
            updateSourceFailure(source, "获取章节内容异常: ${e.message}")
            ""
        }
    }
    
    /**
     * 导入书源（带校验）
     */
    @Transactional
    fun importSource(json: String): ImportResult {
        if (json.isBlank()) {
            return ImportResult(emptyList(), emptyList(), "JSON 不能为空")
        }
        
        return try {
            val type = object : TypeToken<List<BookSource>>() {}.type
            val sources = gson.fromJson<List<BookSource>>(json, type)
            
            if (sources.isEmpty()) {
                return ImportResult(emptyList(), emptyList(), "没有有效的书源")
            }
            
            val successList = mutableListOf<BookSource>()
            val failList = mutableListOf<BookSource>()
            val errors = mutableListOf<String>()
            
            sources.forEach { source ->
                try {
                    validateBookSource(source)
                    
                    // 检查是否已存在
                    val existing = bookSourceRepository.findByUrl(source.url)
                    if (existing.isNotEmpty()) {
                        failList.add(source)
                        errors.add("书源已存在: ${source.name}")
                        return@forEach
                    }
                    
                    // 初始化新书源
                    source.apply {
                        enabled = true
                        failCount = 0
                        lastUsed = null
                        if (id.isNullOrBlank()) {
                            id = null // 让 JPA 生成 ID
                        }
                    }
                    
                    val saved = bookSourceRepository.save(source)
                    successList.add(saved)
                } catch (e: Exception) {
                    failList.add(source)
                    errors.add("${source.name}: ${e.message}")
                }
            }
            
            ImportResult(successList, failList, errors.joinToString("; "))
        } catch (e: Exception) {
            logger.error("导入书源异常", e)
            ImportResult(emptyList(), emptyList(), "JSON 解析失败: ${e.message}")
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
    fun testSource(sourceId: String): SourceTestResult {
        val source = bookSourceRepository.findById(sourceId).orElse(null)
            ?: return SourceTestResult(
                sourceId = sourceId,
                sourceName = null,
                success = false,
                error = "书源不存在",
                details = emptyList()
            )
        
        return try {
            val testDetails = mutableListOf<TestDetail>()
            
            // 测试搜索
            val searchResult = rhinoEngine.executeSearch(source, "测试")
            testDetails.add(TestDetail("搜索", searchResult.success, searchResult.error))
            
            // 测试获取书籍详情（如果有搜索结果）
            if (searchResult.success) {
                val books = parseBooks(searchResult.result, source)
                if (books.isNotEmpty()) {
                    val bookInfoResult = rhinoEngine.executeBookInfo(source, books[0])
                    testDetails.add(TestDetail("获取书籍详情", bookInfoResult.success, bookInfoResult.error))
                    
                    // 测试获取目录
                    if (bookInfoResult.success) {
                        val tocResult = rhinoEngine.executeToc(source, books[0])
                        testDetails.add(TestDetail("获取目录", tocResult.success, tocResult.error))
                        
                        // 测试获取内容
                        if (tocResult.success) {
                            val chapters = parseChapters(tocResult.result, books[0].bookUrl)
                            if (chapters.isNotEmpty()) {
                                val chapter = BookChapter(url = chapters[0].url, bookUrl = books[0].bookUrl)
                                val contentResult = rhinoEngine.executeContent(source, books[0], chapter)
                                testDetails.add(TestDetail("获取章节内容", contentResult.success, contentResult.error))
                            }
                        }
                    }
                }
            }
            
            val successCount = testDetails.count { it.success }
            SourceTestResult(
                sourceId = source.id!!,
                sourceName = source.name,
                success = successCount == testDetails.size || successCount > 0,
                error = if (successCount == testDetails.size) null else "部分测试失败",
                details = testDetails
            )
        } catch (e: Exception) {
            logger.error("测试书源异常: ${source.name}", e)
            SourceTestResult(
                sourceId = source.id!!,
                sourceName = source.name,
                success = false,
                error = "测试异常: ${e.message}",
                details = emptyList()
            )
        }
    }
    
    /**
     * 获取书源统计
     */
    fun getSourceStats(): Map<String, Any> {
        val allSources = bookSourceRepository.findAll()
        val enabledSources = allSources.filter { it.enabled }
        val failedSources = allSources.filter { it.failCount >= MAX_FAIL_COUNT }
        
        return mapOf(
            "total" to allSources.size,
            "enabled" to enabledSources.size,
            "disabled" to (allSources.size - enabledSources.size),
            "failed" to failedSources.size,
            "byGroup" to allSources.groupingBy { it.group ?: "未分组" }.eachCount()
        )
    }
    
    /**
     * 重置书源失败计数
     */
    fun resetSourceFailCount(sourceId: String): Boolean {
        val source = bookSourceRepository.findById(sourceId).orElse(null) ?: return false
        source.failCount = 0
        bookSourceRepository.save(source)
        return true
    }
    
    /**
     * 批量重置失败计数
     */
    @Transactional
    fun resetAllSourceFailCount(): Int {
        val sources = bookSourceRepository.findAll()
        sources.forEach { it.failCount = 0 }
        bookSourceRepository.saveAll(sources)
        return sources.size
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
                    result.filterIsInstance<Map<String, Any>>().mapNotNull { map ->
                        val bookUrl = map["bookUrl"] as? String ?: map["url"] as? String ?: ""
                        if (bookUrl.isBlank()) {
                            logger.warn("搜索结果缺少 bookUrl: ${map}")
                            return@mapNotNull null
                        }
                        
                        Book(
                            name = map["name"] as? String ?: map["title"] as? String ?: "未知书名",
                            author = map["author"] as? String ?: "",
                            coverUrl = map["coverUrl"] as? String ?: map["cover"] as? String,
                            intro = map["intro"] as? String ?: map["description"] as? String,
                            bookUrl = bookUrl,
                            origin = source.id,
                            source = source,
                            lastReadAt = null
                        )
                    }
                }
                is String -> {
                    val list = gson.fromJson(result, List::class.java)
                    parseBooks(list, source)
                }
                else -> {
                    logger.warn("未知的搜索结果类型: ${result::class.simpleName}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("解析搜索结果异常", e)
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
                        val chapterUrl = map["url"] as? String ?: map["chapterUrl"] as? String ?: ""
                        val title = map["title"] as? String ?: map["name"] as? String ?: "未知章节"
                        
                        // 尝试解析索引，如果不是数字则使用当前索引
                        val chapterIndex = when (val indexValue = map["index"]) {
                            is Number -> indexValue.toInt()
                            is String -> indexValue.toIntOrNull() ?: index
                            else -> index
                        }
                        
                        BookChapter(
                            title = title,
                            url = chapterUrl,
                            bookUrl = bookUrl,
                            index = chapterIndex,
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
            logger.error("解析目录异常", e)
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
            is Map<*, *> -> {
                result.values.joinToString("\n")
            }
            else -> result?.toString() ?: ""
        }
    }
    
    /**
     * 更新书籍信息
     */
    private fun updateBookFromResult(book: Book, result: Any?) {
        if (result !is Map<*, *>) return
        
        book.apply {
            result["name"]?.let { name = it.toString().ifBlank { name } }
            result["author"]?.let { author = it.toString().ifBlank { author } }
            result["coverUrl"]?.let { coverUrl = it.toString().ifBlank { coverUrl } }
            result["cover"]?.let { coverUrl = it.toString().ifBlank { coverUrl } }
            result["intro"]?.let { intro = it.toString().ifBlank { intro } }
            result["description"]?.let { intro = it.toString().ifBlank { intro } }
            result["lastChapter"]?.let { lastChapter = it.toString().ifBlank { lastChapter } }
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private fun validateBookSource(source: BookSource) {
        if (source.name.isBlank()) {
            throw IllegalArgumentException("书源名称不能为空")
        }
        if (source.url.isBlank()) {
            throw IllegalArgumentException("书源URL不能为空")
        }
        if (!isValidUrl(source.url)) {
            throw IllegalArgumentException("书源URL格式不正确")
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun updateSourceSuccess(source: BookSource) {
        source.failCount = 0
        source.lastUsed = java.time.LocalDateTime.now()
        bookSourceRepository.save(source)
    }
    
    private fun updateSourceFailure(source: BookSource, error: String) {
        source.failCount++
        source.lastUsed = java.time.LocalDateTime.now()
        
        // 如果失败次数过多，禁用书源
        if (source.failCount >= MAX_FAIL_COUNT) {
            source.enabled = false
            logger.warn("书源因失败次数过多被禁用: ${source.name} (${source.failCount})")
        }
        
        bookSourceRepository.save(source)
    }
    
    private fun checkRateLimit(sourceId: String): Boolean {
        val counter = requestRateLimiter.getOrPut(sourceId) { AtomicInteger(0) }
        return counter.getAndIncrement() <= CHAPTER_RATE_LIMIT
    }
    
    /**
     * 定时任务：重置限流计数器
     */
    @Scheduled(fixedRate = 60000) // 每分钟
    fun resetRateLimiters() {
        requestRateLimiter.clear()
    }
    
    /**
     * 定时任务：清理过期搜索缓存
     */
    @Scheduled(fixedRate = 300000) // 每5分钟
    fun cleanupSearchCache() {
        val now = System.currentTimeMillis()
        searchResultCache.entries.removeIf { it.value.second <= now }
    }
    
    /**
     * 定时任务：自动禁用失败过多的书源
     */
    @Scheduled(fixedRate = 3600000) // 每小时
    fun autoDisableFailedSources() {
        val sources = bookSourceRepository.findAll()
        var disabledCount = 0
        
        sources.forEach { source ->
            if (source.enabled && source.failCount >= MAX_FAIL_COUNT) {
                source.enabled = false
                bookSourceRepository.save(source)
                disabledCount++
                logger.info("自动禁用失败书源: ${source.name} (${source.failCount})")
            }
        }
        
        if (disabledCount > 0) {
            logger.info("自动禁用了 $disabledCount 个失败书源")
        }
    }
}

// ==================== 数据模型类 ====================

data class SearchResult(
    val book: Book,
    val source: BookSource,
    val searchedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

data class ImportResult(
    val success: List<BookSource>,
    val failed: List<BookSource>,
    val message: String
) {
    val total: Int get() = success.size + failed.size
    val successCount: Int get() = success.size
    val failedCount: Int get() = failed.size
}

data class SourceTestResult(
    val sourceId: String,
    val sourceName: String?,
    val success: Boolean,
    val error: String?,
    val details: List<TestDetail>
)

data class TestDetail(
    val action: String,
    val success: Boolean,
    val error: String?
)
