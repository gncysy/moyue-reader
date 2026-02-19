package com.moyue.repository

import com.moyue.model.BookSource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BookSourceRepository : JpaRepository<BookSource, String> {
    
    fun findByEnabledTrue(): List<BookSource>
    
    fun findByEnabledFalse(): List<BookSource>
    
    fun findByUrl(url: String): BookSource?
    
    fun findByNameContainingIgnoreCase(name: String): List<BookSource>
    
    @Query("SELECT b FROM BookSource b WHERE b.id IN :ids")
    fun findAllByIdIn(@Param("ids") ids: List<String>): List<BookSource>
    
    fun findByGroup(group: String): List<BookSource>
    
    fun findAllByOrderByWeightDesc(): List<BookSource>
    
    @Query("SELECT COUNT(b) FROM BookSource b WHERE b.enabled = true")
    fun countEnabled(): Long
}
