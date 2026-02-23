package com.moyue.service
 
import com.moyue.model.BookSource
import com.moyue.repository.BookSourceRepository
import com.moyue.engine.RhinoEngine
import mu.KotlinLogging
 
/**
 * 书源服务
 * 适配 Ktor + Exposed
 */
class SourceService(
    private val sourceRepository: BookSourceRepository,
    private val rhinoEngine: RhinoEngine,
    private val cacheService: CacheService
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 获取所有书源
     */
    fun getAllSources(): List<BookSource> {
        return sourceRepository.findAll()
    }
    
    /**
     * 获取启用的书源
     */
    fun getEnabledSources(): List<BookSource> {
        return sourceRepository.findByEnabled(true)
    }
    
    /**
     * 获取书源详情
     */
    fun getSourceById(id: String): BookSource {
        return sourceRepository.findById(id) 
            ?: throw IllegalArgumentException("书源不存在: $id")
    }
    
    /**
     * 保存书源
     */
    fun saveSource(source: BookSource): BookSource {
        logger.info { "保存书源: ${source.name}" }
        
        val existing = sourceRepository.findById(source.id)
        return if (existing != null) {
            sourceRepository.update(source)
        } else {
            sourceRepository.save(source)
        }
    }
    
    /**
     * 删除书源
     */
    fun deleteSource(id: String): Boolean {
        logger.info { "删除书源: $id" }
        return sourceRepository.delete(id)
    }
    
    /**
     * 启用/禁用书源
     */
    fun toggleSource(id: String): BookSource {
        val source = getSourceById(id)
        val updated = source.copy(enabled = !source.enabled)
        return sourceRepository.update(updated)
    }
    
    /**
     * 搜索书籍（跨书源）
     */
    fun searchBooks(keyword: String): List<Map<String, Any>> {
        logger.info { "搜索书籍: $keyword" }
        
        val enabledSources = getEnabledSources()
        val results = mutableListOf<Map<String, Any>>()
        
        // TODO: 并发搜索多个书源
        enabledSources.forEach { source ->
            try {
                val books = searchFromSource(source, keyword)
                results.addAll(books)
            } catch (e: Exception) {
                logger.error(e) { "搜索失败: ${source.name}" }
            }
        }
        
        return results
    }
    
    /**
     * 从单个书源搜索
     */
    private fun searchFromSource(source: BookSource, keyword: String): List<Map<String, Any>> {
        // TODO: 使用 Rhino 执行搜索规则
        return emptyList()
    }
    
    /**
     * 获取章节列表
     */
    fun getChapterList(bookUrl: String, sourceId: String?): List<Map<String, Any>> {
        // TODO: 实现章节列表获取
        return emptyList()
    }
    
    /**
     * 获取章节内容
     */
    fun getChapterContent(chapterUrl: String, sourceId: String?): String {
        // TODO: 实现章节内容获取
        return ""
    }
}
