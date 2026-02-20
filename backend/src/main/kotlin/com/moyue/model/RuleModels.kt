package com.moyue.model

import com.google.gson.Gson

/**
 * 搜索规则
 */
data class SearchRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val bookUrl: String? = null,
    val kind: String? = null,
    val wordCount: String? = null,
    val lastChapter: String? = null
)

/**
 * 书籍详情规则
 */
data class BookInfoRule(
    val init: String? = null,
    val name: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null,
    val kind: String? = null,
    val wordCount: String? = null,
    val latestChapter: String? = null
)

/**
 * 目录规则
 */
data class TocRule(
    val chapterList: String? = null,
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val nextTocUrl: String? = null,
    val isVolume: String? = null
)

/**
 * 正文规则
 */
data class ContentRule(
    val content: String? = null,
    val nextContentUrl: String? = null
)

/**
 * 发现规则
 */
data class ExploreRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val bookUrl: String? = null,
    val intro: String? = null
)

/**
 * 在 BookSource 中扩展的解析方法
 */
fun BookSource.getSearchRule(): SearchRule? {
    return if (ruleSearch.isNullOrBlank()) null 
           else Gson().fromJson(ruleSearch, SearchRule::class.java)
}

fun BookSource.getBookInfoRule(): BookInfoRule? {
    return if (ruleBookInfo.isNullOrBlank()) null 
           else Gson().fromJson(ruleBookInfo, BookInfoRule::class.java)
}

fun BookSource.getTocRule(): TocRule? {
    return if (ruleToc.isNullOrBlank()) null 
           else Gson().fromJson(ruleToc, TocRule::class.java)
}

fun BookSource.getContentRule(): ContentRule? {
    return if (ruleContent.isNullOrBlank()) null 
           else Gson().fromJson(ruleContent, ContentRule::class.java)
}
