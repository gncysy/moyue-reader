package com.moyue.model

import com.google.gson.Gson
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "book_chapters")
data class BookChapter(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,
    
    @Column(name = "book_id")
    var bookId: String? = null,
    
    @ManyToOne
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    var book: Book? = null,
    
    var title: String = "",
    
    var url: String = "",
    
    @Column(name = "book_url")
    var bookUrl: String = "",
    
    @Column(name = "`index`")
    var index: Int = 0,
    
    @Column(length = 1000000)
    var content: String? = null,
    
    @Column(name = "is_read")
    var isRead: Boolean = false,
    
    @Column(name = "cached_at")
    var cachedAt: LocalDateTime? = null
) {
    
    fun toJson(): String {
        val map = mutableMapOf(
            "id" to id,
            "bookId" to bookId,
            "title" to title,
            "url" to url,
            "bookUrl" to bookUrl,
            "index" to index,
            "isRead" to isRead
        )
        return Gson().toJson(map)
    }
    
    companion object {
        fun fromJson(json: String): BookChapter {
            return Gson().fromJson(json, BookChapter::class.java)
        }
    }
}
