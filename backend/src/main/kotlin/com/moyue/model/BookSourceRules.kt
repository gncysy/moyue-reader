package com.moyue.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import com.google.gson.Gson

/**
 * 书源规则数据类
 * 用于存储和解析 Legado 格式的书源规则
 */

data class SearchRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val kind: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null,
    val lastChapter: String? = null,
    val bookUrl: String? = null
)

data class BookInfoRule(
    val init: String? = null,
    val name: String? = null,
    val author: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val lastChapter: String? = null,
    val tocUrl: String? = null,
    val wordCount: String? = null
)

data class TocRule(
    val chapterList: String? = null,
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val isPay: String? = null,
    val updateTime: String? = null
)

data class ContentRule(
    val content: String? = null,
    val nextContentUrl: String? = null
)

data class ExploreRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null
)

/**
 * JSON 转换器用于 JPA
 */
@Converter
class SearchRuleConverter : AttributeConverter<SearchRule?, String> {
    private val gson = Gson()
    
    override fun convertToDatabaseColumn(attribute: SearchRule?): String? {
        return attribute?.let { gson.toJson(it) }
    }
    
    override fun convertToEntityAttribute(dbData: String?): SearchRule? {
        return dbData?.let { gson.fromJson(it, SearchRule::class.java) }
    }
}

@Converter
class BookInfoRuleConverter : AttributeConverter<BookInfoRule?, String> {
    private val gson = Gson()
    
    override fun convertToDatabaseColumn(attribute: BookInfoRule?): String? {
        return attribute?.let { gson.toJson(it) }
    }
    
    override fun convertToEntityAttribute(dbData: String?): BookInfoRule? {
        return dbData?.let { gson.fromJson(it, BookInfoRule::class.java) }
    }
}

@Converter
class TocRuleConverter : AttributeConverter<TocRule?, String> {
    private val gson = Gson()
    
    override fun convertToDatabaseColumn(attribute: TocRule?): String? {
        return attribute?.let { gson.toJson(it) }
    }
    
    override fun convertToEntityAttribute(dbData: String?): TocRule? {
        return dbData?.let { gson.fromJson(it, TocRule::class.java) }
    }
}

@Converter
class ContentRuleConverter : AttributeConverter<ContentRule?, String> {
    private val gson = Gson()
    
    override fun convertToDatabaseColumn(attribute: ContentRule?): String? {
        return attribute?.let { gson.toJson(it) }
    }
    
    override fun convertToEntityAttribute(dbData: String?): ContentRule? {
        return dbData?.let { gson.fromJson(it, ContentRule::class.java) }
    }
}

@Converter
class ExploreRuleConverter : AttributeConverter<ExploreRule?, String> {
    private val gson = Gson()
    
    override fun convertToDatabaseColumn(attribute: ExploreRule?): String? {
        return attribute?.let { gson.toJson(it) }
    }
    
    override fun convertToEntityAttribute(dbData: String?): ExploreRule? {
        return dbData?.let { gson.fromJson(it, ExploreRule::class.java) }
    }
}
