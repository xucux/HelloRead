package com.github.xucux.read.service

import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.model.ReadingMode
import com.github.xucux.read.model.ReadingRecord
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

/**
 * 阅读记录服务
 * 负责管理不同阅读模式下的阅读记录更新
 */
@Service
class ReadingRecordService {
    private val logger = thisLogger()
    private val dataStorageService = DataStorageService.getInstance()
    
    companion object {
        @JvmStatic fun getInstance(): ReadingRecordService {
            return ApplicationManager.getApplication().getService(ReadingRecordService::class.java)
        }
    }
    
    /**
     * 更新阅读记录 - 章节模式
     * @param book 当前书籍
     * @param chapter 当前章节
     * @param scrollPosition 滚动位置
     * @param lineNumber 章节内当前阅读行号（从1开始）
     */
    fun updateReadingRecordForChapterMode(
        book: Book,
        chapter: Chapter,
        scrollPosition: Int,
        lineNumber: Int
    ) {
        try {
            // 计算本书内阅读行号 = 章节起始行号 + 章节内当前阅读行号
            val bookReadLineNumber = (chapter.startPosition + lineNumber - 1).toInt()
            
            val record = ReadingRecord(
                bookId = book.id,
                chapterIndex = chapter.index,
                chapterTitle = chapter.title,
                chapterStartLineNumber = chapter.startPosition,
                chapterEndLineNumber = chapter.endPosition,
                totalLines = book.totalLines,
                bookReadLineNumber = bookReadLineNumber,
                readTime = System.currentTimeMillis(),
                scrollPosition = scrollPosition,
                lineNumber = lineNumber
            )
            
            dataStorageService.saveReadingRecord(record)
            logger.info("章节模式阅读记录已更新: ${book.title} - ${chapter.title}, 行号: $lineNumber")
            
        } catch (e: Exception) {
            logger.error("更新章节模式阅读记录失败: ${book.title}", e)
        }
    }
    
    /**
     * 更新阅读记录 - 滚动模式
     * 仅在触发上一章和下一章数据加载时更新
     * @param book 当前书籍
     * @param chapter 当前章节
     */
    fun updateReadingRecordForScrollMode(
        book: Book,
        chapter: Chapter
    ) {
        try {
            // 滚动模式：lineNumber为章节首行，bookReadLineNumber为章节起始行
            val lineNumber = 1
            val bookReadLineNumber = chapter.startPosition.toInt()
            
            val record = ReadingRecord(
                bookId = book.id,
                chapterIndex = chapter.index,
                chapterTitle = chapter.title,
                chapterStartLineNumber = chapter.startPosition,
                chapterEndLineNumber = chapter.endPosition,
                totalLines = book.totalLines,
                bookReadLineNumber = bookReadLineNumber,
                readTime = System.currentTimeMillis(),
                scrollPosition = 0, // 滚动模式不需要记录滚动位置
                lineNumber = lineNumber
            )
            
            dataStorageService.saveReadingRecord(record)
            logger.info("滚动模式阅读记录已更新: ${book.title} - ${chapter.title}")
            
        } catch (e: Exception) {
            logger.error("更新滚动模式阅读记录失败: ${book.title}", e)
        }
    }
    
    /**
     * 更新阅读记录 - 状态栏模式
     * @param book 当前书籍
     * @param chapter 当前章节
     * @param lineNumber 章节内当前阅读行号（从1开始）
     */
    fun updateReadingRecordForStatusBarMode(
        book: Book,
        chapter: Chapter,
        lineNumber: Int
    ) {
        try {
            // 计算本书内阅读行号 = 章节起始行号 + 章节内当前阅读行号
            val bookReadLineNumber = (chapter.startPosition + lineNumber - 1).toInt()
            
            val record = ReadingRecord(
                bookId = book.id,
                chapterIndex = chapter.index,
                chapterTitle = chapter.title,
                chapterStartLineNumber = chapter.startPosition,
                chapterEndLineNumber = chapter.endPosition,
                totalLines = book.totalLines,
                bookReadLineNumber = bookReadLineNumber,
                readTime = System.currentTimeMillis(),
                scrollPosition = 0, // 状态栏模式不需要滚动位置
                lineNumber = lineNumber
            )
            
            dataStorageService.saveReadingRecord(record)
            logger.info("状态栏模式阅读记录已更新: ${book.title} - ${chapter.title}, 行号: $lineNumber")
            
        } catch (e: Exception) {
            logger.error("更新状态栏模式阅读记录失败: ${book.title}", e)
        }
    }
    
    /**
     * 根据滚动位置计算章节内当前阅读行号
     * @param chapter 当前章节
     * @param scrollPosition 滚动位置
     * @param contentHeight 内容总高度
     * @param viewportHeight 视口高度
     * @return 章节内当前阅读行号（从1开始）
     */
    fun calculateLineNumberFromScrollPosition(
        chapter: Chapter,
        scrollPosition: Int,
        contentHeight: Int,
        viewportHeight: Int
    ): Int {
        try {
            if (contentHeight <= 0 || viewportHeight <= 0) {
                return 1
            }
            
            // 计算滚动进度百分比
            val scrollProgress = scrollPosition.toFloat() / (contentHeight - viewportHeight).toFloat()
            scrollProgress.coerceIn(0f, 1f)
            
            // 根据章节内容行数计算当前阅读行号
            val chapterContentLines = (chapter.endPosition - chapter.startPosition).toInt()
            val currentLineInChapter = (scrollProgress * chapterContentLines).toInt() + 1
            
            return currentLineInChapter.coerceIn(1, chapterContentLines)
            
        } catch (e: Exception) {
            logger.error("计算章节内阅读行号失败", e)
            return 1
        }
    }
    
    /**
     * 获取阅读记录
     * @param bookId 书籍ID
     * @return 阅读记录，如果不存在则返回null
     */
    fun getReadingRecord(bookId: String): ReadingRecord? {
        return dataStorageService.loadReadingRecord(bookId)
    }
    
    /**
     * 删除阅读记录
     * @param bookId 书籍ID
     */
    fun removeReadingRecord(bookId: String) {
        dataStorageService.removeReadingRecord(bookId)
        logger.info("阅读记录已删除: $bookId")
    }
    
    /**
     * 根据阅读模式更新阅读记录
     * @param readingMode 阅读模式
     * @param book 当前书籍
     * @param chapter 当前章节
     * @param scrollPosition 滚动位置（章节模式和滚动模式使用）
     * @param lineNumber 章节内当前阅读行号（章节模式和状态栏模式使用）
     * @param contentHeight 内容总高度（章节模式计算行号使用）
     * @param viewportHeight 视口高度（章节模式计算行号使用）
     */
    fun updateReadingRecordByMode(
        readingMode: ReadingMode,
        book: Book,
        chapter: Chapter,
        scrollPosition: Int,
        lineNumber: Int,
        contentHeight: Int,
        viewportHeight: Int
    ) {
        when (readingMode) {
            ReadingMode.CHAPTER_MODE -> {
                // 章节模式：根据滚动位置计算行号
                val calculatedLineNumber = if (contentHeight > 0 && viewportHeight > 0) {
                    calculateLineNumberFromScrollPosition(chapter, scrollPosition, contentHeight, viewportHeight)
                } else {
                    lineNumber
                }
                updateReadingRecordForChapterMode(book, chapter, scrollPosition, calculatedLineNumber)
            }
            ReadingMode.SCROLL_MODE -> {
                // 滚动模式：仅在章节切换时更新
                updateReadingRecordForScrollMode(book, chapter)
            }
            ReadingMode.STATUS_BAR_MODE -> {
                // 状态栏模式：根据当前渲染行更新
                updateReadingRecordForStatusBarMode(book, chapter, lineNumber)
            }
        }
    }
}
