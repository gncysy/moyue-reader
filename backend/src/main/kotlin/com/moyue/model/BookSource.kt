package com.moyue.model
 
import com.google.gson.Gson
import java.time.LocalDateTime
import java.util.UUID
 
/**
 * 书源数据类
 */
data class BookSource(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var url: String = "",
    var group: String? = null,
    var enabled: Boolean = true,
    var enableJs: Boolean = true,
    var concurrent: Int = 1,
    var weight: Int = 0,
    var loginUrl: String? = null,
    var loginCheckJs: String? = null,
    var headerJs: String? = null,
    var searchUrl: String? = null,
    var ruleSearch: String? = null,
    var ruleBookInfo: String? = null,
    var ruleToc: String? = null,
    var ruleContent: String? = null,
    var ruleExplore: String? = null,
    var charset: String = "UTF-8",
    var securityRating: Int = 5,
    var lastUsed: LocalDateTime? = null,
    var failCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): BookSource = Gson().fromJson(json, BookSource::class.java)
    }
}
