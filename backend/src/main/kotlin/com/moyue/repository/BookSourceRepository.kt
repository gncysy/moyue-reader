package com.moyue.repository
 
import com.moyue.model.BookSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
 
/**
 * 书源仓储接口
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * Spring Data JPA
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Repository
interface BookSourceRepository : JpaRepository<BookSource, String> {
    
    /**
     * 根据书源 ID 查询
     */
    fun findBySourceId(sourceId: String): @Nullable BookSource?
    
    /**
     * 检查书源 ID 是否存在
     */
    fun existsBySourceId(sourceId: String): Boolean
    
    /**
     * 查询所有启用的书源（按权重排序）
     */
    @Query("""
        SELECT bs FROM BookSource bs 
        WHERE bs.enabled = true 
        ORDER BY bs.weight DESC, bs.name ASC
    """)
    fun findEnabledSources(): List<BookSource>
    
    /**
     * 查询所有启用的书源（分页）
     */
    @Query("""
        SELECT bs FROM BookSource bs 
        WHERE bs.enabled = true 
        ORDER BY bs.weight DESC, bs.name ASC
    """)
    fun findEnabledSources(pageable: Pageable): Page<BookSource>
    
    /**
     * 根据名称模糊搜索书源
     */
    @Query("""
        SELECT bs FROM BookSource bs 
        WHERE bs.name LIKE %:keyword% 
           OR bs.author LIKE %:keyword%
        ORDER BY bs.weight DESC, bs.name ASC
    """)
    fun searchSources(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<BookSource>
    
    /**
     * 获取最近使用的书源
     */
    @Query("""
        SELECT bs FROM BookSource bs 
        WHERE bs.lastUsedAt IS NOT NULL 
        ORDER BY bs.lastUsedAt DESC
    """)
    fun findRecentlyUsedSources(pageable: Pageable): Page<BookSource>
    
    /**
     * 获取可用的书源（已检查且成功）
     */
    @Query("""
        SELECT bs FROM BookSource bs 
        WHERE bs.enabled = true AND bs.checkStatus = 'success'
        ORDER BY bs.weight DESC, bs.name ASC
    """)
    fun findAvailableSources(): List<BookSource>
    
    /**
     * 获取需要检查的书源
     */
    @Query("""
        SELECT bs FROM BookSource bs 
        WHERE bs.enabled = true 
          AND (bs.lastCheckedAt IS NULL 
               OR bs.lastCheckedAt < :threshold)
        ORDER BY bs.weight DESC
    """)
    @EntityGraph(attributePaths = ["rules"])
    fun findSourcesNeedingCheck(
        @Param("threshold") threshold: LocalDateTime
    ): List<BookSource>
    
    /**
     * 统计书源数量
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN bs.enabled = true THEN 1 ELSE 0 END) as enabledCount,
            SUM(CASE WHEN bs.checkStatus = 'success' THEN 1 ELSE 0 END) as availableCount
        FROM BookSource bs
    """)
    fun getSourceStats(): Map<String, Any>
    
    /**
     * 根据作者查询书源
     */
    fun findByAuthor(author: String?): List<BookSource>
    
    /**
     * 根据作者查询书源（分页）
     */
    fun findByAuthor(author: String?, pageable: Pageable): Page<BookSource>
    
    /**
     * 批量更新权重
     */
    @Query("""
        UPDATE BookSource bs 
        SET bs.weight = :weight 
        WHERE bs.sourceId = :sourceId
    """)
    fun updateWeight(
        @Param("sourceId") sourceId: String,
        @Param("weight") weight: Int
    ): Int
    
    /**
     * 批量启用/禁用书源
     */
    @Query("""
        UPDATE BookSource bs 
        SET bs.enabled = :enabled 
        WHERE bs.sourceId = :sourceId
    """)
    fun updateEnabled(
        @Param("sourceId") sourceId: String,
        @Param("enabled") enabled: Boolean
    ): Int
}
