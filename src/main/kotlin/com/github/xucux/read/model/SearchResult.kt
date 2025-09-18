package com.github.xucux.read.model

/**
 * 搜索结果数据模型
 */
data class SearchResult(
    val chapterIndex: Int,    // 章节索引
    val position: Int,        // 在章节中的位置
    val context: String,      // 上下文内容
    val chapterTitle: String  // 章节标题
)
