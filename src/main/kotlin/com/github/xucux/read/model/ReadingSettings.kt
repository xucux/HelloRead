package com.github.xucux.read.model

/**
 * 阅读设置数据模型
 */
data class ReadingSettings(
    val readingMode: ReadingMode = ReadingMode.CHAPTER_MODE,  // 阅读模式
    val currentLine: Int = 0,                                 // 当前阅读行
    val totalLines: Int = 0,                                  // 总计行数
    val currentChapterIndex: Int = 0,                         // 当前章节索引
    val currentChapterTitle: String = "",                     // 当前章节标题
    val lastUpdateTime: Long = System.currentTimeMillis()     // 最后更新时间
) {
    /**
     * 获取阅读进度百分比
     */
    fun getReadingProgress(): Float {
        return if (totalLines > 0) {
            currentLine.toFloat() / totalLines.toFloat()
        } else {
            0f
        }
    }
    
    /**
     * 获取进度显示文本
     */
    fun getProgressText(): String {
        return "$currentLine / $totalLines"
    }
    
    /**
     * 获取章节标题显示文本
     */
    fun getChapterTitleText(): String {
        return if (currentChapterTitle.isNotEmpty()) {
            "${currentChapterIndex + 1}-$currentChapterTitle"
        } else {
            "${currentChapterIndex + 1}"
        }
    }
}
