package com.moyue.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * 搜索规则
 * 用于从搜索结果页面解析书籍信息
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

/**
 * 书籍信息规则
 * 用于从书籍详情页解析信息
 */
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

/**
 * 目录规则
 * 用于从目录页解析章节列表
 */
data class TocRule(
    val chapterList: String? = null,
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val isPay: String? = null,
    val updateTime: String? = null
)

/**
 * 内容规则
 * 用于从章节页解析正文内容
 */
data class ContentRule(
    val content: String? = null,
    val nextContentUrl: String? = null
)

/**
 * 探索规则
 * 用于从发现页解析书籍列表
 */
data class ExploreRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null
)

/**
 * 泛型 JSON 转换器
 * 
 * 将数据类序列化为 JSON 存储到数据库
 */
private class JsonConverter<T>(
    private val clazz: Class<T>
) : AttributeConverter<T?, String> {
    
    private companion object {
        val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
    }
    
    override fun convertToDatabaseColumn(attribute: T?): String? {
        return try {
            attribute?.let { gson.toJson(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun convertToEntityAttribute(dbData: String?): T? {
        return try {
            dbData?.let { gson.fromJson(it, clazz) }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * SearchRule 转换器
 */
@Converter(autoApply = true)
class SearchRuleConverter : AttributeConverter<SearchRule?, String> {
    private val delegate = JsonConverter(SearchRule::class.java)
    
    override fun convertToDatabaseColumn(attribute: SearchRule?): String? {
        return delegate.convertToDatabaseColumn(attribute)
    }
    
    override fun convertToEntityAttribute(dbData: String?): SearchRule? {
        return delegate.convertToEntityAttribute(dbData)
    }
}

/**
 * BookInfoRule 转换器
 */
@Converter(autoApply = true)
class BookInfoRuleConverter : AttributeConverter<BookInfoRule?, String> {
    private val delegate = JsonConverter(BookInfoRule::class.java)
    
    override fun convertToDatabaseColumn(attribute: BookInfoRule?): String? {
        return delegate.convertToDatabaseColumn(attribute)
    }
    
    override fun convertToEntityAttribute(dbData: String?): BookInfoRule? {
        return delegate.convertToEntityAttribute(dbData)
    }
}

/**
 * TocRule 转换器
 */
@Converter(autoApply = true)
class TocRuleConverter : AttributeConverter<TocRule?, String> {
    private val delegate = JsonConverter(TocRule::class.java)
    
    override fun convertToDatabaseColumn(attribute: TocRule?): String? {
        return delegate.convertToDatabaseColumn(attribute)
    }
    
    override fun convertToEntityAttribute(dbData: String?): TocRule? {
        return delegate.convertToEntityAttribute(dbData)
    }
}

/**
 * ContentRule 转换器
 */
@Converter(autoApply = true)
class ContentRuleConverter : AttributeConverter<ContentRule?, String> {
    private val delegate = JsonConverter(ContentRule::class.java)
    
    override fun convertToDatabaseColumn(attribute: ContentRule?): String? {
        return delegate.convertToDatabaseColumn(attribute)
    }
    
    override fun convertToEntityAttribute(dbData: String?): ContentRule? {
        return delegate.convertToEntityAttribute(dbData)
    }
}

/**
 * ExploreRule 转换器
 */
@Converter(autoApply = true)
class ExploreRuleConverter : AttributeConverter<ExploreRule?, String> {
    private val delegate = JsonConverter(ExploreRule::class.java)
    
    override fun convertToDatabaseColumn(attribute: ExploreRule?): String? {
        return delegate.convertToDatabaseColumn(attribute)
    }
    
    override fun convertToEntityAttribute(dbData: String?): ExploreRule? {
        return delegate.convertToEntityAttribute(dbData)
    }
}
