package com.moyue.repository

import com.moyue.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, String> {
    
    fun findByOrigin(sourceId: String): List<Book>
    
    fun findTop10ByOrderByLastReadAtDesc(): List<Book>
    
    fun findByNameContainingIgnoreCase(name: String): List<Book>
}
