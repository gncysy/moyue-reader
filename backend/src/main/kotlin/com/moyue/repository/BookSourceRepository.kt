package com.moyue.repository

import com.moyue.model.BookSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookSourceRepository : JpaRepository<BookSource, String> {
    
    // 基础查询 - 返回全部（小数据量场景）
    fun findByEnabledTrue(): List<BookSource>
    
    fun findByEnabledFalse(): List<BookSource>
    
    fun findByUrl(url: String): BookSource?
    
    fun findByNameContainingIgnoreCase(name: String): List<BookSource>
    
    // 删除冗余的自定义查询，使用 JpaRepository 内置的 findAllById 方法
    // @Query("SELECT b FROM BookSource b WHERE b.id IN :ids")
    // fun findAllByIdIn(@Param("ids") ids: List<String>): List<BookSource>
    
    fun findByGroup(group: String): List<BookSource>
    
    fun findAllByOrderByWeightDesc(): List<BookSource>
    
    // 统计查询
    @Query("SELECT COUNT(b) FROM BookSource b WHERE b.enabled = true")
    fun countEnabled(): Long
    
    // 新增：分页查询版本（大数据量场景推荐使用）
    fun findByEnabledTrue(pageable: Pageable): Page<BookSource>
    
    fun findByEnabledFalse(pageable: Pageable): Page<BookSource>
    
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<BookSource>
    
    fun findByGroup(group: String, pageable: Pageable): Page<BookSource>
    
    fun findAllByOrderByWeightDesc(pageable: Pageable): Page<BookSource>
    
    // 新增：批量查询（按权重范围）
    fun findByWeightBetween(minWeight: Int, maxWeight: Int): List<BookSource>
}
