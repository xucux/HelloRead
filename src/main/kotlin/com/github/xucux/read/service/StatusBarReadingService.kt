package com.github.xucux.read.service

import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.model.DisplaySettings
import com.github.xucux.read.model.ReadingRecord
import com.github.xucux.read.service.ChapterParserService
import com.github.xucux.read.service.DataStorageService
import com.github.xucux.read.service.DisplaySettingsService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.github.xucux.read.factory.StatusBarReadingWidgetFactory
import com.github.xucux.read.widget.StatusBarReadingWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import javax.swing.Timer
import java.awt.event.ActionListener
import java.awt.event.ActionEvent

/**
 * 状态栏阅读操作回调接口
 */
interface StatusBarReadingCallback {
    fun onNextLine()
    fun onPreviousLine()
    fun onNextChapter()
    fun onPreviousChapter()
}

/**
 * 底部状态栏阅读服务
 * 负责在IDE底部状态栏显示阅读内容
 */
@Service
class StatusBarReadingService {
    private val logger = logger<StatusBarReadingService>()
    
    companion object {
        @JvmStatic fun getInstance(): StatusBarReadingService {
            return ApplicationManager.getApplication().getService(StatusBarReadingService::class.java)
        }
    }
    
    private val chapterParserService = ChapterParserService.getInstance()
    private val displaySettingsService = DisplaySettingsService.getInstance()
    private val dataStorageService = DataStorageService.getInstance()
    private val readingRecordService = ReadingRecordService.getInstance()
    private var currentProject: Project? = null
    private var currentBook: Book? = null
    private var currentChapter: Chapter? = null
    private var currentContent: String = ""
    private var currentLineIndex: Int = 0
    private var contentLines: List<String> = emptyList()
    private var autoScrollTimer: Timer? = null
    private var isActive: Boolean = false
    var callback: StatusBarReadingCallback? = null
        private set
    
    /**
     * 启动底部状态栏阅读模式
     */
    fun startStatusBarReading(project: Project, book: Book, chapter: Chapter, callback: StatusBarReadingCallback? = null) {
        this.currentProject = project
        this.currentBook = book
        this.currentChapter = chapter
        this.callback = callback
        
        try {
            // 获取章节内容
            val content = chapterParserService.getChapterContent(book.file, chapter)
            val formattedContent = chapterParserService.formatChapterContent(content)
            
            // 按行分割内容，过滤空行
            contentLines = formattedContent.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            // 如果contentLines有超过30个字符时再次拆分，拆分后每个元素放入contentLines
            contentLines = contentLines.chunked(30)
                .map { it.joinToString("\n") }
                .flatMap { it.split("\n") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
            
            if (contentLines.isEmpty()) {
                logger.warn("章节内容为空: ${chapter.title}")
                return
            }
            
            currentContent = formattedContent
            
            // 获取当前章节的阅读记录，使用保存的行号作为起始位置
            val readingRecord = dataStorageService.loadReadingRecord(book.id)
            currentLineIndex = if (readingRecord != null && readingRecord.chapterIndex == chapter.index) {
                // 如果是同一章节，使用保存的行号
                val savedLineNumber = readingRecord.lineNumber
                if (savedLineNumber >= 0 && savedLineNumber < contentLines.size) {
                    savedLineNumber
                } else {
                    0
                }
            } else {
                // 如果没有阅读记录或不是同一章节，从开头开始
                0
            }
            
            // 启用状态栏Widget
            val widgetFactory = StatusBarReadingWidgetFactory()
            widgetFactory.setEnabled(project, false)
            
            // 开始自动滚动
            startAutoScroll()
            
            isActive = true
            logger.info("底部状态栏阅读模式已启动: ${book.title} - ${chapter.title}")
            
        } catch (e: Exception) {
            logger.error("启动底部状态栏阅读模式失败", e)
        }
    }
    
    /**
     * 停止底部状态栏阅读模式
     */
    fun stopStatusBarReading() {
        // 保存最终阅读进度
        saveCurrentReadingProgress()
        
        isActive = false
        
        // 停止自动滚动
        autoScrollTimer?.stop()
        autoScrollTimer = null
        
        // 清除状态栏显示
        currentProject?.let { project ->
            val statusBarWidgetsManager = project.getService(StatusBarWidgetsManager::class.java)
            val widgetFactory = StatusBarReadingWidgetFactory()
            statusBarWidgetsManager.updateWidget(widgetFactory)
        }
        currentProject = null
        currentBook = null
        currentChapter = null
        currentContent = ""
        currentLineIndex = 0
        contentLines = emptyList()
        
        logger.info("底部状态栏阅读模式已停止")
    }
    
    
    /**
     * 开始自动滚动
     */
    private fun startAutoScroll() {
        // 获取显示设置
        val displaySettings = displaySettingsService.loadDisplaySettings()
        
        // 如果启用了自动滚动
        if (displaySettings.statusBarAutoScroll) {
            val interval = displaySettings.statusBarScrollInterval
            autoScrollTimer = Timer(interval) {
                if (isActive && currentLineIndex < contentLines.size - 1) {
                    currentLineIndex++
                    // 更新状态栏Widget显示
                    currentProject?.let { project ->
                        StatusBarReadingWidget.update(project, getCurrentLineText())
                    }
                    // 保存阅读进度
                    saveCurrentReadingProgress()
                } else if (isActive && currentLineIndex >= contentLines.size - 1) {
                    // 如果到达章节末尾，可以选择停止或循环
                    currentLineIndex = 0
                    // 更新状态栏Widget显示
                    currentProject?.let { project ->
                        StatusBarReadingWidget.update(project, getCurrentLineText())
                    }
                    // 保存阅读进度
                    saveCurrentReadingProgress()
                }
            }
            autoScrollTimer?.start()
        } else {
            // 如果没有启用自动滚动，则只更新状态栏Widget显示
            currentProject?.let { project ->
                StatusBarReadingWidget.update(project, getCurrentLineText())
            }
        }
    }
    
    
    
    /**
     * 手动滚动到下一行
     */
    fun scrollToNextLine() {
        if (isActive && currentLineIndex < contentLines.size - 1) {
            currentLineIndex++
            // 更新状态栏Widget显示
            currentProject?.let { project ->
                StatusBarReadingWidget.update(project, getCurrentLineText())
            }
            // 保存阅读进度
            saveCurrentReadingProgress()
        }
    }
    
    /**
     * 手动滚动到上一行
     */
    fun scrollToPreviousLine() {
        if (isActive && currentLineIndex > 0) {
            currentLineIndex--
            // 更新状态栏Widget显示
            currentProject?.let { project ->
                StatusBarReadingWidget.update(project, getCurrentLineText())
            }
            // 保存阅读进度
            saveCurrentReadingProgress()
        }
    }
    
    /**
     * 跳转到指定行
     */
    fun jumpToLine(lineIndex: Int) {
        if (isActive && lineIndex >= 0 && lineIndex < contentLines.size) {
            currentLineIndex = lineIndex
            // 更新状态栏Widget显示
            currentProject?.let { project ->
                StatusBarReadingWidget.update(project, getCurrentLineText())
            }
            // 保存阅读进度
            saveCurrentReadingProgress()
        }
    }
    
    /**
     * 检查是否处于活动状态
     */
    fun isActive(): Boolean = isActive
    
    /**
     * 保存当前阅读进度
     */
    private fun saveCurrentReadingProgress() {
        if (isActive && currentBook != null && currentChapter != null) {
            try {
                // 状态栏模式：根据当前渲染行更新阅读记录
                readingRecordService.updateReadingRecordForStatusBarMode(
                    book = currentBook!!,
                    chapter = currentChapter!!,
                    lineNumber = currentLineIndex + 1 // 转换为从1开始的行号
                )
                logger.debug("保存状态栏阅读进度: ${currentBook!!.title} - ${currentChapter!!.title} 第${currentLineIndex + 1}行")
            } catch (e: Exception) {
                logger.error("保存状态栏阅读进度失败", e)
            }
        }
    }
    
    /**
     * 获取当前阅读进度
     */
    fun getCurrentProgress(): Pair<Int, Int> {
        return Pair(currentLineIndex + 1, contentLines.size)
    }
    
    /**
     * 获取当前书籍信息
     */
    fun getCurrentBookInfo(): Pair<String, String>? {
        return if (currentBook != null && currentChapter != null) {
            Pair(currentBook!!.title, currentChapter!!.title)
        } else {
            null
        }
    }
    
    /**
     * 获取当前行文本（供Widget使用）
     */
    fun getCurrentLineText(): String {
        return if (currentLineIndex < contentLines.size) {
            val line = contentLines[currentLineIndex]
            // 限制显示长度，避免状态栏过长
            if (line.length > 100) {
                line.substring(0, 97) + "..."
            } else {
                line
            }
        } else {
            "阅读结束"
        }
    }
    
    /**
     * 获取工具提示文本（供Widget使用）
     */
    fun getTooltipText(): String {
        val bookTitle = currentBook?.title ?: "未知书籍"
        val chapterTitle = currentChapter?.title ?: "未知章节"
        val progress = if (contentLines.isNotEmpty()) {
            "${currentLineIndex + 1} / ${contentLines.size}"
        } else {
            "0 / 0"
        }
        
        return "<html>" +
                "<b>书名:</b> $bookTitle<br/>" +
                "<b>章节:</b> $chapterTitle<br/>" +
                "<b>进度:</b> $progress<br/>" +
                "<b>当前行:</b> ${getCurrentLineText()}" +
                "</html>"
    }
}
