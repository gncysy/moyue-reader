 package com.moyue.service

import com.moyue.model.Book
import com.moyue.repository.BookRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BookService(
    private val bookRepository: BookRepository
) {

    fun getAllBooks(): List<Book> {
        return bookRepository.findAll()
    }

    fun getBookById(id: String): Book? {
        return bookRepository.findById(id).orElse(null)
    }

    fun saveBook(book: Book): Book {
        return bookRepository.save(book)
    }

    fun deleteBook(id: String): Boolean {
        return if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    fun getRecentBooks(limit: Int): List<Book> {
        return bookRepository.findTop10ByOrderByLastReadAtDesc()
    }

    fun importLocalBook(filePath: String, sourceId: String?): Book {
        // 这里需要实现本地书籍导入逻辑
        // 暂时返回一个空书籍
        return Book(
            name = filePath.substringAfterLast("/"),
            bookUrl = filePath,
            origin = sourceId,
            createdAt = LocalDateTime.now()
        )
    }
}
