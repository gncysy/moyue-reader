package com.moyue.model

import com.google.gson.Gson
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "books")
data class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,
    
    @Column(nullable = false)
    var name: String = "",
    
    var author: String = "",
    
    @Column(length = 1000)
    var coverUrl: String? = null,
    
    @Column(length = 10000)
    var intro: String? = null,
    
    @Column(name = "book_url")
    var bookUrl: String = "",
    
    @Column(name = "origin")
    var origin: String? = null,  // 书源ID
    
    @ManyToOne
    @JoinColumn(name = "source_id")
    var source: BookSource? = null,
    
    var chapterCount: Int = 0,
    
    @Column(name = "current_chapter")
    var currentChapter: Int = 0,
    
    var progress: Int = 0,
    
    @Column(name = "last_read_at")
    var lastReadAt: LocalDateTime? = null,
    
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    fun toJson(): String {
        val map = mutableMapOf(
            "id" to id,
            "name" to name,
            "author" to author,
            "coverUrl" to coverUrl,
            "intro" to intro,
            "bookUrl" to bookUrl,
            "origin" to origin,
            "chapterCount" to chapterCount,
            "currentChapter" to currentChapter,
            "progress" to progress,
            "lastReadAt" to lastReadAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "createdAt" to createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        return Gson().toJson(map)
    }
    
    companion object {
        fun fromJson(json: String): Book {
            return Gson().fromJson(json, Book::class.java)
        }
    }
}
