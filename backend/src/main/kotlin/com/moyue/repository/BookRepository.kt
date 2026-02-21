package com.moyue.repository

import com.moyue.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 书籍数据访问接口
 */
@Repository
interface BookRepository : JpaRepository<Book, String> {
    
    /**
     * 按书源 ID 查找书籍
     */
    fun findByOrigin(sourceId: String?): List<Book>
    
    /**
     * 按书名模糊查找（不区分大小写）
     */
    fun findByNameContainingIgnoreCase(name: String): List<Book>
    
    /**
     * 按作者查找
     */
    fun findByAuthorContainingIgnoreCase(author: String): List<Book>
    
    /**
     * 按书名精确查找
     */
    fun findByName(name: String): Book?
    
    /**
     * 获取最近阅读的 10 本书
     */
    fun findTop10ByOrderByLastReadAtDesc(): List<Book>
    
    /**
     * 统计书源书籍数量
     */
    fun countByOrigin(sourceId: String?): Long
    
    /**
     * 按书源 ID 删除所有书籍
     */
    fun deleteByOrigin(sourceId: String)
}
