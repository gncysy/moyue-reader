package com.moyue.service
 
import com.moyue.engine.RhinoEngine
import com.moyue.model.BookChapter
import com.moyue.model.BookSource
import com.moyue.model.BookSourceRules
import com.moyue.repository.BookSourceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.jspecify.annotations.Nullable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
 
/**
 * 书源服务
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 书源 CRUD
 * - 书源规则解析
 * - 搜索书籍
 * - 获取章节列表
 * - 获取章节内容
 * - 书源检查
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Service
@Transactional
class SourceService(
    private val bookSourceRepository: BookSourceRepository,
    @Lazy
    private val rhinoEngine: RhinoEngine
) {
    
    private val logger = LoggerFactory.getLogger(SourceService::class.java)
    
    @Value("\${moyue.book-source.max-concurrent-search:5}")
    private var maxConcurrentSearch: Int = 5
    
    @Value("\${moyue.book-source.request-timeout:10}")
    private var requestTimeout: Int = 10
    
    @Value("\${moyue.book-source.search-timeout:30}")
    private var searchTimeout: Int = 30
    
    // ==================== 书源查询操作 ====================
    
    /**
     * 获取所有书源（分页）
     */
    @Cacheable(value = ["sources"], key = "#page + '-' + #size")
    fun getAllSources(page: Int = 0, size: Int = 20): Page<BookSource> {
        logger.debug("获取书源列表: page=$page, size=$size")
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weight"))
        return bookSourceRepository.findAll(pageable)
    }
    
    /**
     * 根据书源 ID 查询
     */
    @Cacheable(value = ["source"], key = "#sourceId")
    fun getSourceById(sourceId: String): BookSource {
        return bookSourceRepository.findBySourceId(sourceId)
            ?: throw IllegalArgumentException("书源不存在: $sourceId")
    }
    
    /**
     * 获取所有启用的书源
     */
    @Cacheable(value = ["enabled-sources"])
    fun getEnabledSources(): List<BookSource> {
        logger.debug("获取启用的书源")
        return bookSourceRepository.findEnabledSources()
    }
    
    /**
     * 获取可用的书源
     */
    @Cacheable(value = ["available-sources"])
    fun getAvailableSources(): List<BookSource> {
        logger.debug("获取可用的书源")
        return bookSourceRepository.findAvailableSources()
    }
    
    /**
     * 搜索书源
     */
    @Cacheable(value = ["source-search"], key = "#keyword + '-' + #page + '-' + #size")
    fun searchSources(keyword: String, page: Int = 0, size: Int = 20): Page<BookSource> {
        logger.debug("搜索书源: $keyword")
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weight"))
        return bookSourceRepository.searchSources(keyword, pageable)
    }
    
    // ==================== 书源保存操作 ====================
    
    /**
     * 保存书源
     */
    @CacheEvict(value = ["sources", "enabled-sources", "available-sources", "source-search"], allEntries = true)
    fun saveSource(source: BookSource): BookSource {
        logger.info("保存书源: ${source.name}")
        return bookSourceRepository.save(source)
    }
    
    /**
     * 导入书源（JSON 文件）
     */
    @CacheEvict(value = ["sources", "enabled-sources", "available-sources", "source-search"], allEntries = true)
    fun importSource(file: MultipartFile): BookSource {
        logger.info("导入书源: ${file.originalFilename}")
        
        val content = BufferedReader(InputStreamReader(file.inputStream)).use { it.readText() }
        val source = BookSource.fromJson(content)
        
        // 检查书源 ID 是否已存在
        if (bookSourceRepository.existsBySourceId(source.sourceId)) {
            throw IllegalArgumentException("书源 ID 已存在: ${source.sourceId}")
        }
        
        return bookSourceRepository.save(source)
    }
    
    /**
     * 批量导入书源
     */
    @CacheEvict(value = ["sources", "enabled-sources", "available-sources", "source-search"], allEntries = true)
    fun importSources(sources: List<BookSource>): List<BookSource> {
        logger.info("批量导入书源: ${sources.size} 个")
        
        sources.forEach { source ->
            if (bookSourceRepository.existsBySourceId(source.sourceId)) {
                throw IllegalArgumentException("书源 ID 已存在: ${source.sourceId}")
            }
        }
        
        return bookSourceRepository.saveAll(sources)
    }
    
    // ==================== 书源更新操作 ====================
    
    /**
     * 更新书源
     */
    @CacheEvict(value = ["sources", "source", "enabled-sources", "available-sources", "source-search"], allEntries = true)
    fun updateSource(sourceId: String, source: BookSource): BookSource {
        logger.info("更新书源: $sourceId")
        
        val existing = getSourceById(sourceId)
        val updated = source.copy(
            id = existing.id,
            sourceId = existing.sourceId,
            createdAt = existing.createdAt
        )
        
        return bookSourceRepository.save(updated)
    }
    
    /**
     * 启用/禁用书源
     */
    @CacheEvict(value = ["enabled-sources", "available-sources"], allEntries = true)
    fun toggleSource(sourceId: String, enabled: Boolean): BookSource {
        logger.info("切换书源状态: $sourceId, enabled=$enabled")
        
        val source = getSourceById(sourceId)
        source.enabled = enabled
        source.updatedAt = LocalDateTime.now()
        
        return bookSourceRepository.save(source)
    }
    
    /**
     * 更新书源权重
     */
    @CacheEvict(value = ["enabled-sources", "available-sources"], allEntries = true)
    fun updateWeight(sourceId: String, weight: Int): Int {
        logger.info("更新书源权重: $sourceId, weight=$weight")
        return bookSourceRepository.updateWeight(sourceId, weight)
    }
    
    // ==================== 书源删除操作 ====================
    
    /**
     * 删除书源
     */
    @CacheEvict(value = ["sources", "source", "enabled-sources", "available-sources", "source-search"], allEntries = true)
    fun deleteSource(sourceId: String): Boolean {
        logger.info("删除书源: $sourceId")
        
        val source = bookSourceRepository.findBySourceId(sourceId)
        if (source != null) {
            bookSourceRepository.delete(source)
            return true
        }
        return false
    }
    
    /**
     * 批量删除书源
     */
    @CacheEvict(value = ["sources", "enabled-sources", "available-sources", "source-search"], allEntries = true)
    fun deleteSources(sourceIds: List<String>): Int {
        logger.info("批量删除书源: ${sourceIds.size} 个")
        
        var count = 0
        sourceIds.forEach { id ->
            if (deleteSource(id)) count++
        }
        
        return count
    }
    
    // ==================== 书源规则解析 ====================
    
    /**
     * 获取书籍信息
     */
    @Cacheable(value = ["book-info"], key = "#bookUrl + '-' + #sourceId")
    fun getBookInfo(bookUrl: String, sourceId: String): Map<String, Any> {
        logger.debug("获取书籍信息: $bookUrl, source=$sourceId")
        
        val source = getSourceById(sourceId)
        val rules = source.rules ?: throw IllegalArgumentException("书源规则不存在: $sourceId")
        
        // 使用 Rhino 引擎执行规则
        val result = rhinoEngine.executeBookInfoRule(source, rules, bookUrl)
        
        source.updateUsedTime()
        bookSourceRepository.save(source)
        
        return result
    }
    
    /**
     * 获取章节列表
     */
    @Cacheable(value = ["chapter-list"], key = "#bookUrl + '-' + #sourceId")
    fun getChapterList(bookUrl: String, sourceId: String): List<BookChapter> {
        logger.debug("获取章节列表: $bookUrl, source=$sourceId")
        
        val source = getSourceById(sourceId)
        val rules = source.rules ?: throw IllegalArgumentException("书源规则不存在: $sourceId")
        
        // 使用 Rhino 引擎执行规则
        val chapters = rhinoEngine.executeChapterListRule(source, rules, bookUrl)
        
        source.updateUsedTime()
        bookSourceRepository.save(source)
        
        return chapters
    }
    
    /**
     * 获取章节内容
     */
    @Cacheable(value = ["chapter-content"], key = "#chapterUrl + '-' + #bookUrl + '-' + #sourceId")
    fun getChapterContent(chapterUrl: String, bookUrl: String, sourceId: String): String {
        logger.debug("获取章节内容: $chapterUrl")
        
        val source = getSourceById(sourceId)
        val rules = source.rules ?: throw IllegalArgumentException("书源规则不存在: $sourceId")
        
        // 使用 Rhino 引擎执行规则
        val content = rhinoEngine.executeContentRule(source, rules, chapterUrl, bookUrl)
        
        source.updateUsedTime()
        bookSourceRepository.save(source)
        
        return content
    }
    
    // ==================== 搜索书籍 ====================
    
    /**
     * 搜索书籍（多源并发）
     */
    fun searchBook(
        keyword: String,
        sourceIds: List<String>? = null,
        maxConcurrent: Int = maxConcurrentSearch
    ): List<Map<String, Any>> {
        logger.info("搜索书籍: $keyword, sources: ${sourceIds?.size ?: "all"}, concurrent: $maxConcurrent")
        
        val sources = if (sourceIds.isNullOrEmpty()) {
            getAvailableSources().take(maxConcurrent)
        } else {
            sourceIds.take(maxConcurrent).mapNotNull { 
                bookSourceRepository.findBySourceId(it) 
            }
        }
        
        if (sources.isEmpty()) {
            logger.warn("没有可用的书源")
            return emptyList()
        }
        
        // 并发搜索
        val results = mutableListOf<Map<String, Any>>()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(sources.size.coerceAtMost(maxConcurrent))
        
        try {
            val futures = sources.map { source ->
                java.util.concurrent.CompletableFuture.supplyAsync {
                    try {
                        logger.debug("从书源搜索: ${source.name}")
                        val searchResults = rhinoEngine.executeSearchRule(
                            source,
                            source.rules ?: throw IllegalArgumentException("书源规则不存在"),
                            keyword
                        )
                        searchResults.forEach { result ->
                            result["sourceId"] = source.sourceId
                            result["sourceName"] = source.name
                        }
                        source.updateUsedTime()
                        bookSourceRepository.save(source)
                        searchResults
                    } catch (e: Exception) {
                        logger.error("书源搜索失败: ${source.name}", e)
                        emptyList()
                    }
                }, executor
            }
            
            // 等待所有搜索完成
            futures.forEach { future ->
                try {
                    val result = future.get(searchTimeout.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                    results.addAll(result)
                } catch (e: Exception) {
                    logger.error("搜索超时或失败", e)
                }
            }
        } finally {
            executor.shutdown()
        }
        
        logger.info("搜索完成: 找到 ${results.size} 个结果")
        return results
    }
    
    // ==================== 书源检查 ====================
    
    /**
     * 检查书源可用性
     */
    @CacheEvict(value = ["available-sources"], allEntries = true)
    fun checkSource(sourceId: String): BookSource {
        logger.info("检查书源: $sourceId")
        
        val source = getSourceById(sourceId)
        
        try {
            // 简单检查：尝试获取首页
            val url = source.url ?: throw IllegalArgumentException("书源 URL 为空")
            val result = rhinoEngine.checkUrl(url)
            
            if (result) {
                source.updateCheckStatus("success", "书源可用")
            } else {
                source.updateCheckStatus("failed", "书源不可用")
            }
        } catch (e: Exception) {
            logger.error("书源检查失败: $sourceId", e)
            source.updateCheckStatus("failed", e.message)
        }
        
        return bookSourceRepository.save(source)
    }
    
    /**
     * 批量检查书源
     */
    @CacheEvict(value = ["available-sources"], allEntries = true)
    fun checkSources(sourceIds: List<String>? = null): List<BookSource> {
        logger.info("批量检查书源: ${sourceIds?.size ?: "all"}")
        
        val threshold = LocalDateTime.now().minusDays(1)
        val sourcesToCheck = if (sourceIds.isNullOrEmpty()) {
            bookSourceRepository.findSourcesNeedingCheck(threshold)
        } else {
            sourceIds.mapNotNull { bookSourceRepository.findBySourceId(it) }
        }
        
        return sourcesToCheck.map { checkSource(it.sourceId) }
    }
    
    // ==================== 书源导出 ====================
    
    /**
     * 导出书源（JSON）
     */
    fun exportSource(sourceId: String): String {
        val source = getSourceById(sourceId)
        return source.toJson()
    }
    
    /**
     * 导出所有书源（JSON 数组）
     */
    fun exportAllSources(): String {
        val sources = bookSourceRepository.findAll()
        return com.google.gson.Gson().toJson(sources)
    }
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取书源统计
     */
    @Cacheable(value = ["source-stats"])
    fun getSourceStats(): Map<String, Any> {
        logger.debug("获取书源统计")
        return bookSourceRepository.getSourceStats()
    }
    
    /**
     * 清除缓存
     */
    @CacheEvict(value = ["sources", "source", "enabled-sources", "available-sources", "source-search", "book-info", "chapter-list", "chapter-content", "source-stats"], allEntries = true)
    fun clearCache() {
        logger.info("清除书源缓存")
    }
}
