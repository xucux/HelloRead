package com.github.xucux.read.model

/**
 * 章节数据模型
 */
data class Chapter(
    val index: Int,           // 章节索引
    val title: String,        // 章节标题
    val content: String,      // 章节内容
    val startPosition: Long,  // 在文件中的起始位置(行号)
    val endPosition: Long,    // 在文件中的结束位置(行号)
    val originalTitle: String, // 原始章节名称（如：第三十五章 张平三顾醉仙楼）
    val chapterNumber: String  // 章节序列号（如：第35章）
) {
    /**
     * 获取章节显示名称
     */
    fun getDisplayName(): String {
        return originalTitle.ifEmpty { "第${index + 1}章 $title" }
    }
    
    /**
     * 获取章节内容长度
     */
    fun getContentLength(): Int {
        return content.length
    }
}