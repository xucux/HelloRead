package com.github.xucux.read.model

/**
 * 阅读记录数据模型
 */
data class ReadingRecord(
    val bookId: String,           // 书籍ID
    val chapterIndex: Int,        // 章节索引
    val chapterTitle: String,     // 章节标题
    val chapterStartLineNumber: Long,  // 章节在文件中的起始位置(章节起始行号)
    val chapterEndLineNumber: Long,    // 章节在文件中的结束位置(章节结束行号)
    val totalLines: Int = 0,           // 本书总行数
    val bookReadLineNumber: Int = 0,     // 本书内阅读行号 = 章节起始行号 + 章节内当前阅读行号（用于恢复阅读位置）
    val readTime: Long,           // 阅读时间
    val scrollPosition: Int = 0,  // 章节内滚动位置（可选，用于恢复阅读位置）
    val lineNumber: Int = 0       // 章节内当前阅读行号
) {
    /**
     * 获取阅读时间格式化字符串
     */
    fun getFormattedReadTime(): String {
        val date = java.util.Date(readTime)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
        return formatter.format(date)
    }
}
