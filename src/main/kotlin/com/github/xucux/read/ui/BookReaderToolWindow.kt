package com.github.xucux.read.ui

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.model.FontSettings
import com.github.xucux.read.model.ReadingRecord
import com.github.xucux.read.model.ReadingMode
import com.github.xucux.read.model.ReadingSettings
import com.github.xucux.read.service.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.openapi.diagnostic.thisLogger
import javax.swing.text.*
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * 书籍阅读器工具窗口
 */
class BookReaderToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true), com.github.xucux.read.service.StatusBarReadingCallback {
    
    private val logger = thisLogger()
    private val dataStorageService = DataStorageService.getInstance()
    private val chapterParserService = ChapterParserService.getInstance()
    private val fontSettingsService = FontSettingsService.getInstance()
    private val displaySettingsService = DisplaySettingsService.getInstance()
    private val readingSettingsService = ReadingSettingsService.getInstance()
    private val statusBarReadingService = StatusBarReadingService.getInstance()
    private val readingRecordService = ReadingRecordService.getInstance()
    
    private var currentBook: Book? = null
    private var chapters: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = 0
    private var currentFontSettings: FontSettings = FontSettings.DEFAULT
    private var currentReadingSettings: ReadingSettings = ReadingSettings()
    
    // UI组件
    private val titleLabel = JLabel("请选择一本书开始阅读")
    private val chapterTitleLabel = JLabel("")
    private val contentArea = JTextPane()
    private val scrollPane = JBScrollPane(contentArea)
    private lateinit var chapterTitlePanel: JPanel
    
    // 导航按钮
    private val prevButton = JButton("上一章")
    private val nextButton = JButton("下一章")
    private val chapterListButton = JButton("章节列表")
    private val fontSettingsButton = JButton("字体设置")
    private val readingModeButton = JButton("阅读模式")
    
    // 操作面板
    private lateinit var actionButtonPanel: JPanel
    
    // 显示设置
    private var hideOperationPanel = false
    private var hideTitleButton = false
    private var hideProgressLabel = false
    
    // 滚动自动加载相关
    private var isAutoLoading = false
    private var lastScrollPosition = 0
    private var loadedChapters = mutableListOf<Int>() // 记录已加载的章节索引
    private var scrollPositions = mutableMapOf<Int, Int>() // 记录每个章节的滚动位置

    init {
        layout = BorderLayout()
        setupUI()
        setupEventListeners()
        loadFontSettings()
        loadDisplaySettings()
    }
    
    /**
     * 设置UI界面
     */
    private fun setupUI() {
        // 标题面板 - 左右布局：书籍标题在左，章节标题在右
        val titlePanel = JPanel(BorderLayout())
        titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        titleLabel.border = JBUI.Borders.empty(0, 10, 3, 5)
        // 设置书籍标题的最大宽度，防止过长时挤压章节标题
        titleLabel.preferredSize = java.awt.Dimension(200, titleLabel.preferredSize.height)
        titlePanel.add(titleLabel, BorderLayout.WEST)
        
        // 章节标题面板 - 作为标题面板的右侧部分
        chapterTitlePanel = JPanel(BorderLayout())
        chapterTitleLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        chapterTitleLabel.border = JBUI.Borders.empty(0, 5, 3, 10)
        // 设置章节标题右对齐，并允许换行
        chapterTitleLabel.horizontalAlignment = JLabel.RIGHT
        chapterTitleLabel.verticalAlignment = JLabel.TOP
        chapterTitlePanel.add(chapterTitleLabel, BorderLayout.EAST)
        
        // 将章节标题面板添加到标题面板的右侧
        titlePanel.add(chapterTitlePanel, BorderLayout.EAST)
        
        // 内容区域
        contentArea.isEditable = false
        contentArea.isOpaque = false
        contentArea.font = Font(Font.SERIF, Font.PLAIN, 14)
        contentArea.border = JBUI.Borders.empty(10)
        
        // JTextPane的换行设置
        val doc = contentArea.styledDocument
        val set = SimpleAttributeSet()
        StyleConstants.setLineSpacing(set, currentFontSettings.lineSpacing - 1.0f) // 使用字体设置的行间距
        doc.setParagraphAttributes(0, doc.length, set, false)
        
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        
        // 添加滚动监听器，实现自动加载下一章
        setupScrollListener()
        
        // 按钮面板
        actionButtonPanel = JPanel()
        actionButtonPanel.add(prevButton)
        actionButtonPanel.add(chapterListButton)
        actionButtonPanel.add(fontSettingsButton)
        actionButtonPanel.add(readingModeButton)
        actionButtonPanel.add(nextButton)
        actionButtonPanel.border = JBUI.Borders.empty(1)
        
        // 创建内容容器，只包含滚动面板
        val contentContainer = JPanel(BorderLayout())
        contentContainer.add(scrollPane, BorderLayout.CENTER)
        
        // 组装界面
        add(titlePanel, BorderLayout.NORTH)
        add(contentContainer, BorderLayout.CENTER)
        add(actionButtonPanel, BorderLayout.SOUTH)
        
        // 应用显示设置
        applyDisplaySettings()
        
        // 设置初始状态
        updateButtonStates()
    }
    
    /**
     * 设置滚动监听器
     */
    private fun setupScrollListener() {
        val scrollBar = scrollPane.verticalScrollBar
        scrollBar.addAdjustmentListener { e ->
            if (!isAutoLoading && e.source == scrollBar) {
                val currentPosition = scrollBar.value
                val maxPosition = scrollBar.maximum - scrollBar.visibleAmount
                
                // 更新阅读进度
                updateReadingProgress()
                
                // 根据阅读模式处理滚动事件
                when (currentReadingSettings.readingMode) {
                    ReadingMode.SCROLL_MODE -> {
                        // 滚动模式：自动加载章节
                        handleScrollModeScrolling(currentPosition, maxPosition)
                    }
                    ReadingMode.CHAPTER_MODE -> {
                        // 章节模式：根据滚动位置更新阅读记录
                        handleChapterModeScrolling(currentPosition)
                    }
                    ReadingMode.STATUS_BAR_MODE -> {
                        // 底部状态栏模式：不需要处理滚动事件
                    }
                }
                
                lastScrollPosition = currentPosition
            }
        }
    }
    
    /**
     * 处理滚动模式的滚动事件
     */
    private fun handleScrollModeScrolling(currentPosition: Int, maxPosition: Int) {
        // 记录当前章节的滚动位置
        if (loadedChapters.isNotEmpty()) {
            scrollPositions[currentChapterIndex] = currentPosition
        }
        
        // 当滚动到接近底部时（距离底部小于50像素），自动加载下一章
        if (currentPosition >= maxPosition - 50 && currentPosition > lastScrollPosition) {
            autoLoadNextChapter()
        }
        
        // 当滚动到接近顶部时（距离顶部小于50像素），自动加载上一章
        if (currentPosition <= 50 && currentPosition < lastScrollPosition) {
            autoLoadPreviousChapter()
        }
    }
    
    /**
     * 处理章节模式的滚动事件
     */
    private fun handleChapterModeScrolling(currentPosition: Int) {
        // 记录当前章节的滚动位置
        scrollPositions[currentChapterIndex] = currentPosition
        
        // 章节模式：根据滚动位置更新阅读记录
        val book = currentBook ?: return
        val chapter = chapters.getOrNull(currentChapterIndex) ?: return
        
        readingRecordService.updateReadingRecordByMode(
            readingMode = ReadingMode.CHAPTER_MODE,
            book = book,
            chapter = chapter,
            scrollPosition = currentPosition, lineNumber = 0,
            contentHeight = scrollPane.verticalScrollBar.maximum,
            viewportHeight = scrollPane.verticalScrollBar.visibleAmount
        )
    }
    
    /**
     * 自动加载下一章
     */
    private fun autoLoadNextChapter() {
        if (isAutoLoading || chapters.isEmpty() || currentChapterIndex >= chapters.size - 1) {
            return
        }
        
        isAutoLoading = true
        
        try {
            // 获取当前章节内容
            val currentChapter = chapters[currentChapterIndex]
            val currentContent = chapterParserService.getChapterContent(currentBook!!.file, currentChapter)
            val currentFormattedContent = chapterParserService.formatChapterContent(currentContent)
            val currentContentWithSpacing = applyParagraphSpacing(currentFormattedContent)
            
            // 获取下一章内容
            val nextChapter = chapters[currentChapterIndex + 1]
            val nextContent = chapterParserService.getChapterContent(currentBook!!.file, nextChapter)
            val nextFormattedContent = chapterParserService.formatChapterContent(nextContent)
            val nextContentWithSpacing = applyParagraphSpacing(nextFormattedContent)
            
            // 合并内容，添加章节分隔符
            val separator = "\n\n" + "─".repeat(60) + "\n"
            val chapterHeader = "第${nextChapter.index}章 ${nextChapter.title}"
            val headerLine = "─".repeat(60)
            val combinedContent = currentContentWithSpacing + separator + 
                                chapterHeader + "\n" + headerLine + "\n\n" + 
                                nextContentWithSpacing
            
            // 更新显示内容
            contentArea.text = combinedContent
            
            // 更新章节索引和标题
            currentChapterIndex++
            loadedChapters.add(currentChapterIndex)
            chapterTitleLabel.text = "${currentChapter.title} → ${nextChapter.title}"
            
            // 更新进度和按钮状态
            updateButtonStates()
            
            // 滚动模式：在章节切换时更新阅读记录
            val book = currentBook ?: return
            val newChapter = chapters[currentChapterIndex]
            readingRecordService.updateReadingRecordForScrollMode(book, newChapter)
            
            // 滚动到新章节开始位置
            SwingUtilities.invokeLater {
                val currentLength = currentContentWithSpacing.length
                val separatorLength = separator.length
                val headerLength = chapterHeader.length + headerLine.length + 4 // 包括换行符
                val newChapterStart = currentLength + separatorLength + headerLength
                
                try {
                    contentArea.caretPosition = newChapterStart
                    // 使用更简单的方法滚动到指定位置
                    contentArea.scrollRectToVisible(contentArea.getVisibleRect())
                } catch (e: Exception) {
                    // 如果定位失败，滚动到顶部
                    scrollPane.verticalScrollBar.value = 0
                }
            }
            
        } catch (e: Exception) {
            // 如果自动加载失败，回退到手动切换章节
            currentChapterIndex++
            loadCurrentChapter()
        } finally {
            isAutoLoading = false
        }
    }
    
    /**
     * 自动加载上一章
     */
    private fun autoLoadPreviousChapter() {
        if (isAutoLoading || chapters.isEmpty() || currentChapterIndex <= 0) {
            return
        }
        
        isAutoLoading = true
        
        try {
            // 获取上一章内容
            val previousChapter = chapters[currentChapterIndex - 1]
            val previousContent = chapterParserService.getChapterContent(currentBook!!.file, previousChapter)
            val previousFormattedContent = chapterParserService.formatChapterContent(previousContent)
            val previousContentWithSpacing = applyParagraphSpacing(previousFormattedContent)
            
            // 获取当前章节内容
            val currentChapter = chapters[currentChapterIndex]
            val currentContent = chapterParserService.getChapterContent(currentBook!!.file, currentChapter)
            val currentFormattedContent = chapterParserService.formatChapterContent(currentContent)
            val currentContentWithSpacing = applyParagraphSpacing(currentFormattedContent)
            
            // 合并内容，添加章节分隔符
            val separator = "\n\n" + "─".repeat(60) + "\n"
            val chapterHeader = "第${currentChapter.index}章 ${currentChapter.title}"
            val headerLine = "─".repeat(60)
            val combinedContent = previousContentWithSpacing + separator + 
                                chapterHeader + "\n" + headerLine + "\n\n" + 
                                currentContentWithSpacing
            
            // 更新显示内容
            contentArea.text = combinedContent
            
            // 更新章节索引和标题
            currentChapterIndex--
            loadedChapters.add(0, currentChapterIndex) // 添加到列表开头
            chapterTitleLabel.text = "${previousChapter.title} → ${currentChapter.title}"
            
            // 更新进度和按钮状态
            updateButtonStates()
            
            // 滚动模式：在章节切换时更新阅读记录
            val book = currentBook ?: return
            val newChapter = chapters[currentChapterIndex]
            readingRecordService.updateReadingRecordForScrollMode(book, newChapter)
            
            // 滚动到当前章节开始位置
            SwingUtilities.invokeLater {
                val previousLength = previousContentWithSpacing.length
                val separatorLength = separator.length
                val headerLength = chapterHeader.length + headerLine.length + 4 // 包括换行符
                val currentChapterStart = previousLength + separatorLength + headerLength
                
                try {
                    contentArea.caretPosition = currentChapterStart
                    contentArea.scrollRectToVisible(contentArea.getVisibleRect())
                } catch (e: Exception) {
                    // 如果定位失败，滚动到顶部
                    scrollPane.verticalScrollBar.value = 0
                }
            }
            
        } catch (e: Exception) {
            // 如果自动加载失败，回退到手动切换章节
            currentChapterIndex--
            loadCurrentChapter()
        } finally {
            isAutoLoading = false
        }
    }
    
    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        prevButton.addActionListener {
            if (currentChapterIndex > 0) {
                currentChapterIndex--
                loadCurrentChapter()
            }
        }
        
        nextButton.addActionListener {
            if (currentChapterIndex < chapters.size - 1) {
                currentChapterIndex++
                loadCurrentChapter()
            }
        }
        
        chapterListButton.addActionListener {
            showChapterList()
        }
        
        fontSettingsButton.addActionListener {
            showFontSettings()
        }
        
        readingModeButton.addActionListener {
            showReadingModeDialog()
        }
    }
    
    /**
     * 打开书籍进行阅读
     * @param book 要打开的书籍
     * @param useReadingRecord 是否使用阅读记录中的位置，true=从书架进入，false=从章节列表进入
     */
    fun openBook(book: Book, useReadingRecord: Boolean = true) {
        currentBook = book
        titleLabel.text = book.title
        
        try {
            // 解析章节（使用缓存）
            chapters = chapterParserService.parseChapters(book.file, book.id)
            if (chapters.isEmpty()) {
                contentArea.text = "章节内容解析为空"
                return
            }
            
            // 加载阅读设置
            currentReadingSettings = readingSettingsService.loadReadingSettings(book.id)
            
            // 加载字体设置
            currentFontSettings = fontSettingsService.loadFontSettings()
            applyFontSettings()
            
            // 加载显示设置
            val displaySettings = displaySettingsService.loadDisplaySettings()
            updateDisplaySettings(
                displaySettings.hideOperationPanel,
                displaySettings.hideTitleButton,
                displaySettings.hideProgressLabel
            )
            
            // 根据参数决定是否使用阅读记录
            if (useReadingRecord) {
                // 从书架进入：使用阅读记录中的位置
                val readingRecord = dataStorageService.loadReadingRecord(book.id)
                currentChapterIndex = readingRecord?.chapterIndex ?: book.currentChapterIndex
                
                // 确保章节索引有效
                if (currentChapterIndex >= chapters.size) {
                    currentChapterIndex = 0
                }
                
                // 更新阅读模式按钮文本
                updateReadingModeButton()
                
                // 根据阅读模式加载内容
                when (currentReadingSettings.readingMode) {
                    ReadingMode.CHAPTER_MODE -> {
                        // 章节模式：只加载当前章节
                        val savedLineNumber = readingRecord?.lineNumber ?: 0
                        val savedScrollPosition = readingRecord?.scrollPosition ?: 0
                        loadCurrentChapter(savedLineNumber, savedScrollPosition)
                    }
                    ReadingMode.SCROLL_MODE -> {
                        // 滚动模式：加载当前章节，准备自动加载
                        val savedLineNumber = readingRecord?.lineNumber ?: 0
                        val savedScrollPosition = readingRecord?.scrollPosition ?: 0
                        loadCurrentChapterForScrollMode(savedLineNumber, savedScrollPosition)
                    }
                    ReadingMode.STATUS_BAR_MODE -> {
                        // 底部状态栏模式：启动状态栏阅读
                        startStatusBarReading()
                    }
                }
            } else {
                // 从章节列表进入：使用书籍中指定的章节索引，不使用阅读记录
                currentChapterIndex = book.currentChapterIndex
                
                // 确保章节索引有效
                if (currentChapterIndex >= chapters.size) {
                    currentChapterIndex = 0
                }
                
                // 更新阅读模式按钮文本
                updateReadingModeButton()
                
                // 根据阅读模式加载内容（不使用阅读记录中的位置）
                when (currentReadingSettings.readingMode) {
                    ReadingMode.CHAPTER_MODE -> {
                        // 章节模式：只加载当前章节
                        loadCurrentChapter()
                    }
                    ReadingMode.SCROLL_MODE -> {
                        // 滚动模式：加载当前章节，准备自动加载
                        loadCurrentChapterForScrollMode()
                    }
                    ReadingMode.STATUS_BAR_MODE -> {
                        // 底部状态栏模式：启动状态栏阅读
                        startStatusBarReading()
                    }
                }
            }
            
            // 通知章节列表工具窗口同步当前书籍
            notifyChapterListSync()
            
        } catch (e: Exception) {
            contentArea.text = "章节内容解析失败: ${e.message}"
        }
    }
    
    /**
     * 跳转到指定章节
     */
    fun jumpToChapter(chapterIndex: Int) {
        if (chapters.isEmpty() || chapterIndex < 0 || chapterIndex >= chapters.size) {
            return
        }
        
        currentChapterIndex = chapterIndex
        loadCurrentChapter()
        
        // 保存阅读记录
        saveReadingRecord()
        
        // 通知章节列表工具窗口同步
        notifyChapterListSync()
    }
    
    /**
     * 跳转到指定章节的指定内容位置
     */
    fun jumpToChapterAndPosition(chapterIndex: Int, position: Int = 0) {
        if (chapters.isEmpty() || chapterIndex < 0 || chapterIndex >= chapters.size) {
            return
        }
        
        currentChapterIndex = chapterIndex
        loadCurrentChapter()
        
        // 如果指定了位置，尝试跳转到该位置
        if (position > 0) {
            SwingUtilities.invokeLater {
                try {
                    // 确保位置在有效范围内
                    val maxPosition = contentArea.document.length
                    val targetPosition = minOf(position, maxPosition)
                    
                    // 设置光标位置
                    contentArea.caretPosition = targetPosition
                    
                    // 滚动到指定位置
                    try {
                        val rect = contentArea.modelToView2D(targetPosition)
                        if (rect != null) {
                            contentArea.scrollRectToVisible(rect.bounds)
                        }
                    } catch (e: Exception) {
                        // 如果新方法失败，使用备用方法
                        contentArea.scrollRectToVisible(contentArea.getVisibleRect())
                    }
                } catch (e: Exception) {
                    // 如果定位失败，滚动到顶部
                    scrollPane.verticalScrollBar.value = 0
                }
            }
        }
        
        // 保存阅读记录
        saveReadingRecord()
        
        // 通知章节列表工具窗口同步
        notifyChapterListSync()
    }
    
    /**
     * 加载当前章节（章节模式）
     */
    private fun loadCurrentChapter(savedLineNumber: Int = 0, savedScrollPosition: Int = 0) {
        if (chapters.isEmpty() || currentChapterIndex < 0 || currentChapterIndex >= chapters.size) {
            return
        }
        
        // 重置自动加载状态
        isAutoLoading = false
        lastScrollPosition = 0
        loadedChapters.clear()
        loadedChapters.add(currentChapterIndex)
        scrollPositions.clear()
        
        val chapter = chapters[currentChapterIndex]
        chapterTitleLabel.text = chapter.getDisplayName()
        
        // 获取章节内容
        val content = chapterParserService.getChapterContent(currentBook!!.file, chapter)
        val formattedContent = chapterParserService.formatChapterContent(content)
        
        // 应用段落间距
        val contentWithSpacing = applyParagraphSpacing(formattedContent)
        contentArea.text = contentWithSpacing
        
        // 更新进度和按钮状态
        updateButtonStates()
        
        // 恢复到保存的阅读位置
        SwingUtilities.invokeLater {
            if (savedLineNumber > 0) {
                // 优先使用行号定位
                scrollToLine(savedLineNumber)
            } else if (savedScrollPosition > 0) {
                // 如果没有行号，使用滚动位置
                scrollPane.verticalScrollBar.value = savedScrollPosition
            } else {
                // 默认滚动到顶部
                scrollPane.verticalScrollBar.value = 0
            }
        }
    }
    
    /**
     * 加载当前章节（滚动模式）
     */
    private fun loadCurrentChapterForScrollMode(savedLineNumber: Int = 0, savedScrollPosition: Int = 0) {
        if (chapters.isEmpty() || currentChapterIndex < 0 || currentChapterIndex >= chapters.size) {
            return
        }
        
        // 重置自动加载状态
        isAutoLoading = false
        lastScrollPosition = 0
        loadedChapters.clear()
        loadedChapters.add(currentChapterIndex)
        scrollPositions.clear()
        
        val chapter = chapters[currentChapterIndex]
        chapterTitleLabel.text = chapter.getDisplayName()
        
        // 获取章节内容
        val content = chapterParserService.getChapterContent(currentBook!!.file, chapter)
        val formattedContent = chapterParserService.formatChapterContent(content)
        
        // 应用段落间距
        val contentWithSpacing = applyParagraphSpacing(formattedContent)
        contentArea.text = contentWithSpacing
        
        // 更新进度和按钮状态
        updateButtonStates()
        
        // 恢复到保存的阅读位置
        SwingUtilities.invokeLater {
            if (savedLineNumber > 0) {
                // 优先使用行号定位
                scrollToLine(savedLineNumber)
            } else if (savedScrollPosition > 0) {
                // 如果没有行号，使用滚动位置
                scrollPane.verticalScrollBar.value = savedScrollPosition
            } else {
                // 默认滚动到顶部
                scrollPane.verticalScrollBar.value = 0
            }
        }
    }
    
    /**
     * 滚动到指定行号
     */
    private fun scrollToLine(lineNumber: Int) {
        try {
            val doc = contentArea.document
            val rootElement = doc.defaultRootElement
            
            if (lineNumber > 0 && lineNumber <= rootElement.elementCount) {
                val element = rootElement.getElement(lineNumber - 1)
                val startOffset = element.startOffset
                
                // 设置光标位置
                contentArea.caretPosition = startOffset
                
                // 滚动到该位置
                try {
                    val rect = contentArea.modelToView2D(startOffset)
                    if (rect != null) {
                        contentArea.scrollRectToVisible(rect.bounds)
                    }
                } catch (e: Exception) {
                    // 如果新方法失败，使用备用方法
                    contentArea.scrollRectToVisible(contentArea.getVisibleRect())
                }
            }
        } catch (e: Exception) {
            // 如果行号定位失败，回退到滚动位置
            logger.warn("滚动到行号失败: $lineNumber", e)
        }
    }
    
    /**
     * 应用段落间距
     */
    private fun applyParagraphSpacing(content: String): String {
        if (currentFontSettings.paragraphSpacing <= 0) {
            return content
        }
        
        // 将段落分隔符替换为带间距的版本
        val spacing = "\n".repeat(currentFontSettings.paragraphSpacing / 10 + 1)
        return content.replace("\n\n", spacing)
    }
    
    
    /**
     * 更新阅读进度
     */
    private fun updateReadingProgress() {
        val book = currentBook ?: return
        val currentLine = getCurrentLineNumber()
        val chapter = chapters.getOrNull(currentChapterIndex) ?: return
        
        // 更新阅读设置
        currentReadingSettings = currentReadingSettings.copy(
            currentLine = currentLine,
            currentChapterIndex = currentChapterIndex,
            currentChapterTitle = chapter.title,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        // 保存到本地
        readingSettingsService.updateReadingProgress(
            book.id, currentLine, 0, currentChapterIndex, chapter.title
        )
    }
    
    /**
     * 获取当前行号
     */
    private fun getCurrentLineNumber(): Int {
        return try {
            val caretPosition = contentArea.caretPosition
            val doc = contentArea.document
            val rootElement = doc.defaultRootElement
            rootElement.getElementIndex(caretPosition) + 1 // 行号从1开始
        } catch (e: Exception) {
            0
        }
    }
    
    
    /**
     * 更新按钮状态
     */
    private fun updateButtonStates() {
        prevButton.isEnabled = currentChapterIndex > 0
        nextButton.isEnabled = currentChapterIndex < chapters.size - 1
        chapterListButton.isEnabled = chapters.isNotEmpty()
    }
    
    /**
     * 保存阅读记录
     */
    private fun saveReadingRecord() {
        val book = currentBook ?: return
        val chapter = chapters.getOrNull(currentChapterIndex) ?: return
        
        // 获取当前行号
        val currentLineNumber = getCurrentLineNumber()
        val scrollPosition = scrollPane.verticalScrollBar.value
        
        // 使用ReadingRecordService根据阅读模式更新阅读记录
        readingRecordService.updateReadingRecordByMode(
            readingMode = currentReadingSettings.readingMode,
            book = book,
            chapter = chapter,
            scrollPosition = scrollPosition,
            lineNumber = currentLineNumber,
            contentHeight = scrollPane.verticalScrollBar.maximum,
            viewportHeight = scrollPane.verticalScrollBar.visibleAmount
        )
        
        // 更新书籍的当前章节信息
        val updatedBook = book.copy(
            currentChapterIndex = currentChapterIndex,
            currentChapterTitle = chapter.title,
            lastReadTime = System.currentTimeMillis()
        )
        dataStorageService.saveBook(updatedBook)
    }
    
    /**
     * 显示章节列表
     */
    private fun showChapterList() {
        if (chapters.isEmpty()) return
        
        val dialog = ChapterListDialog(project, chapters, currentChapterIndex)
        if (dialog.showAndGet()) {
            val selectedIndex = dialog.getSelectedChapterIndex()
            if (selectedIndex >= 0 && selectedIndex < chapters.size) {
                currentChapterIndex = selectedIndex
                loadCurrentChapter()
            }
        }
    }
    
    /**
     * 显示字体设置对话框
     * 现在改为打开IDEA设置中的HelloRead配置页面
     */
    private fun showFontSettings() {
        com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "com.github.xucux.read.setting.HelloReadSettingComponent"
        )
    }
    
    
    /**
     * 显示阅读模式对话框
     */
    private fun showReadingModeDialog() {
        val dialog = ReadingModeDialog(project, currentReadingSettings.readingMode)
        if (dialog.showAndGet()) {
            val newMode = dialog.getSelectedMode()
            if (newMode != currentReadingSettings.readingMode) {
                // 切换阅读模式
                switchReadingMode(newMode)
            }
        }
    }
    
    /**
     * 切换阅读模式
     */
    private fun switchReadingMode(newMode: ReadingMode) {
        val book = currentBook ?: return
        
        // 更新阅读设置
        currentReadingSettings = currentReadingSettings.copy(
            readingMode = newMode,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        // 保存到本地
        readingSettingsService.updateReadingMode(book.id, newMode)
        
        // 更新按钮文本
        updateReadingModeButton()
        
        // 根据新模式重新加载内容
        when (newMode) {
            ReadingMode.CHAPTER_MODE -> {
                // 切换到章节模式：只显示当前章节
                stopStatusBarReading()
                loadCurrentChapter()
            }
            ReadingMode.SCROLL_MODE -> {
                // 切换到滚动模式：准备自动加载
                stopStatusBarReading()
                loadCurrentChapterForScrollMode()
            }
            ReadingMode.STATUS_BAR_MODE -> {
                // 切换到底部状态栏模式：启动状态栏阅读
                startStatusBarReading()
            }
        }
    }
    
    /**
     * 更新阅读模式按钮文本
     */
    private fun updateReadingModeButton() {
        val modeText = when (currentReadingSettings.readingMode) {
            ReadingMode.CHAPTER_MODE -> "章节模式"
            ReadingMode.SCROLL_MODE -> "滚动模式"
            ReadingMode.STATUS_BAR_MODE -> "状态栏模式"
        }
        readingModeButton.text = modeText
    }
    
    /**
     * 加载字体设置
     */
    fun loadFontSettings() {
        currentFontSettings = fontSettingsService.loadFontSettings()
        applyFontSettings()
    }
    
    /**
     * 加载显示设置
     */
    fun loadDisplaySettings() {
        val displaySettings = displaySettingsService.loadDisplaySettings()
        updateDisplaySettings(
            displaySettings.hideOperationPanel,
            displaySettings.hideTitleButton,
            displaySettings.hideProgressLabel
        )
    }
    
    /**
     * 应用字体设置
     */
    private fun applyFontSettings() {
        // 应用字体到内容区域
        contentArea.font = Font(currentFontSettings.fontFamily, Font.PLAIN, currentFontSettings.fontSize)
        
        // 设置行间距
        setLineSpacing(contentArea, currentFontSettings.lineSpacing)
        
        // 重新加载当前章节以应用新的字体设置
        if (chapters.isNotEmpty() && currentChapterIndex >= 0 && currentChapterIndex < chapters.size) {
            loadCurrentChapter()
        }
    }
    
    /**
     * 启动底部状态栏阅读模式
     */
    private fun startStatusBarReading() {
        val book = currentBook ?: return
        val chapter = chapters.getOrNull(currentChapterIndex) ?: return
        
        // 停止其他阅读模式
        contentArea.text = ""
        chapterTitleLabel.text = "状态栏阅读模式: ${chapter.getDisplayName()}"
        
        // 启动状态栏阅读服务
        statusBarReadingService.startStatusBarReading(project, book, chapter, this)
        
        // 更新按钮状态
        updateButtonStates()
    }
    
    /**
     * 停止底部状态栏阅读模式
     */
    private fun stopStatusBarReading() {
        statusBarReadingService.stopStatusBarReading()
    }
    
    /**
     * 清空阅读器
     */
    fun clear() {
        // 停止状态栏阅读
        stopStatusBarReading()
        
        currentBook = null
        chapters = emptyList()
        currentChapterIndex = 0
        titleLabel.text = "请选择一本书开始阅读"
        chapterTitleLabel.text = ""
        contentArea.text = ""
        updateButtonStates()
    }
    
    /**
     * 更新显示设置
     */
    fun updateDisplaySettings(hideOperationPanel: Boolean, hideTitle: Boolean, hideProgress: Boolean) {
        this.hideOperationPanel = hideOperationPanel
        this.hideTitleButton = hideTitle
        this.hideProgressLabel = hideProgress
        applyDisplaySettings()
    }
    
    /**
     * 应用显示设置
     */
    private fun applyDisplaySettings() {
        // 控制操作面板显示
        actionButtonPanel.isVisible = !hideOperationPanel
        
        // 控制标题按钮显示
        titleLabel.isVisible = !hideTitleButton
        
        // 控制章节标题面板显示
        chapterTitlePanel.isVisible = !hideProgressLabel
        
        // 重新布局
        revalidate()
        repaint()
    }
    
    /**
     * 设置JTextPane的行间距
     * @param textPane 要设置行间距的JTextPane
     * @param lineSpacing 行间距倍数，如1.2表示1.2倍行距
     */
    private fun setLineSpacing(textPane: JTextPane, lineSpacing: Float) {
        val doc = textPane.styledDocument
        val set = SimpleAttributeSet()
        StyleConstants.setLineSpacing(set, lineSpacing - 1.0f)
        doc.setParagraphAttributes(0, doc.length, set, false)
    }
    
    /**
     * 通知章节列表工具窗口同步当前书籍
     */
    private fun notifyChapterListSync() {
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                val contentManager = HelloReadToolWindow.contentManager
                val chapterListContent = contentManager.contents.find { it.displayName == TabConstants.CHAPTER_LIST_TAB }
                if (chapterListContent != null) {
                    val chapterListComponent = chapterListContent.component as? ChapterListToolWindow
                    chapterListComponent?.syncCurrentReadingBook()
                }
            }
        } catch (e: Exception) {
            // 静默处理错误，不影响阅读器功能
        }
    }
    
    /**
     * 导航到上一章（公开方法，供快捷键使用）
     */
    fun navigateToPreviousChapter() {
        if (currentChapterIndex > 0) {
            currentChapterIndex--
            when (currentReadingSettings.readingMode) {
                ReadingMode.CHAPTER_MODE -> loadCurrentChapter()
                ReadingMode.SCROLL_MODE -> loadCurrentChapterForScrollMode()
                ReadingMode.STATUS_BAR_MODE -> startStatusBarReading()
            }
        }
    }
    
    /**
     * 导航到下一章（公开方法，供快捷键使用）
     */
    fun navigateToNextChapter() {
        if (currentChapterIndex < chapters.size - 1) {
            currentChapterIndex++
            when (currentReadingSettings.readingMode) {
                ReadingMode.CHAPTER_MODE -> loadCurrentChapter()
                ReadingMode.SCROLL_MODE -> loadCurrentChapterForScrollMode()
                ReadingMode.STATUS_BAR_MODE -> startStatusBarReading()
            }
        }
    }
    
    /**
     * 检查是否可以导航到上一章（公开方法，供快捷键使用）
     */
    fun canNavigateToPreviousChapter(): Boolean {
        return currentChapterIndex > 0 && chapters.isNotEmpty()
    }
    
    /**
     * 检查是否可以导航到下一章（公开方法，供快捷键使用）
     */
    fun canNavigateToNextChapter(): Boolean {
        return currentChapterIndex < chapters.size - 1 && chapters.isNotEmpty()
    }
    
    /**
     * 向上滚动（公开方法，供快捷键使用）
     */
    fun scrollUp() {
        when (currentReadingSettings.readingMode) {
            ReadingMode.STATUS_BAR_MODE -> {
                // 底部状态栏模式：滚动到上一行
                statusBarReadingService.scrollToPreviousLine()
            }
            else -> {
                // 其他模式：正常滚动
                val scrollBar = scrollPane.verticalScrollBar
                val currentPosition = scrollBar.value
                val scrollAmount = scrollBar.visibleAmount / 3 // 每次滚动1/3屏幕高度
                val newPosition = maxOf(0, currentPosition - scrollAmount)
                scrollBar.value = newPosition
            }
        }
    }
    
    /**
     * 向下滚动（公开方法，供快捷键使用）
     */
    fun scrollDown() {
        when (currentReadingSettings.readingMode) {
            ReadingMode.STATUS_BAR_MODE -> {
                // 底部状态栏模式：滚动到下一行
                statusBarReadingService.scrollToNextLine()
            }
            else -> {
                // 其他模式：正常滚动
                val scrollBar = scrollPane.verticalScrollBar
                val currentPosition = scrollBar.value
                val scrollAmount = scrollBar.visibleAmount / 3 // 每次滚动1/3屏幕高度
                val maxPosition = scrollBar.maximum - scrollBar.visibleAmount
                val newPosition = minOf(maxPosition, currentPosition + scrollAmount)
                scrollBar.value = newPosition
            }
        }
    }
    
    /**
     * 检查是否可以滚动（公开方法，供快捷键使用）
     */
    fun canScroll(): Boolean {
        return chapters.isNotEmpty() && currentBook != null
    }
    
    // StatusBarReadingCallback接口实现
    override fun onNextLine() {
        statusBarReadingService.scrollToNextLine()
    }
    
    override fun onPreviousLine() {
        statusBarReadingService.scrollToPreviousLine()
    }
    
    override fun onNextChapter() {
        navigateToNextChapter()
    }
    
    override fun onPreviousChapter() {
        navigateToPreviousChapter()
    }
}
