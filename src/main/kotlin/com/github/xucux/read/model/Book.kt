package com.github.xucux.read.model

import java.io.File

/**
 * 书籍数据模型
 */
data class Book(
    val id: String,                    // 书籍唯一标识
    val title: String,                 // 书籍标题
    val filePath: String,              // 文件路径
    val file: File,                    // 文件对象
    val totalChapters: Int = 0,        // 总章节数
    val totalLines: Int = 0,           // 总行数
    val currentChapterIndex: Int = 0,  // 当前阅读章节索引
    val currentChapterTitle: String = "", // 当前章节标题
    val lastReadTime: Long = System.currentTimeMillis(), // 最后阅读时间
    val addTime: Long = System.currentTimeMillis()       // 添加时间
) {
    /**
     * 获取显示用的章节信息
     */
    fun getDisplayChapterInfo(): String {
        return if (currentChapterTitle.isNotEmpty()) {
            "第${currentChapterIndex + 1}章 $currentChapterTitle"
        } else {
            "第${currentChapterIndex + 1}章"
        }
    }
    
    /**
     * 获取阅读进度百分比
     */
    fun getReadingProgress(): Float {
        return if (totalChapters > 0) {
            (currentChapterIndex + 1).toFloat() / totalChapters.toFloat()
        } else {
            0f
        }
    }
}