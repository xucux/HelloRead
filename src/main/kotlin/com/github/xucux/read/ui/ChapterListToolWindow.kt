package com.github.xucux.read.ui

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.service.ChapterParserService
import com.github.xucux.read.service.DataStorageService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * 章节列表工具窗口
 */
class ChapterListToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
    
    private val dataStorageService = DataStorageService.getInstance()
    private val chapterParserService = ChapterParserService.getInstance()
    
    private lateinit var chapterList: JBList<String>
    private lateinit var searchField: JBTextField
    private lateinit var bookInfoLabel: JLabel
    private lateinit var chapterCountLabel: JLabel
    
    private var currentBook: Book? = null
    private var chapters: List<Chapter> = emptyList()
    private var allChapterTitles: List<String> = emptyList()
    private var filteredChapterTitles: List<String> = emptyList()
    
    init {
        layout = BorderLayout()
        setupUI()
        setupEventListeners()
        // 自动加载最近阅读的书籍
        loadCurrentReadingBook()
    }
    
    /**
     * 设置UI界面
     */
    private fun setupUI() {
        // 顶部信息面板
        val infoPanel = JPanel(BorderLayout())
        bookInfoLabel = JLabel("请选择一本书查看章节")
        bookInfoLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        bookInfoLabel.border = JBUI.Borders.empty(5, 10, 5, 10)
        
        chapterCountLabel = JLabel("")
        chapterCountLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        chapterCountLabel.border = JBUI.Borders.empty(5, 10, 5, 10)
        
        infoPanel.add(bookInfoLabel, BorderLayout.CENTER)
        infoPanel.add(chapterCountLabel, BorderLayout.EAST)
        
        // 搜索面板
        val searchPanel = JPanel(BorderLayout())
        val searchLabel = JLabel("搜索章节:")
        searchLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        searchLabel.border = JBUI.Borders.empty(5, 10, 5, 5)
        
        searchField = JBTextField()
        searchField.border = JBUI.Borders.empty(5, 5, 5, 10)
        searchField.toolTipText = "输入关键词搜索章节标题"
        
        searchPanel.add(searchLabel, BorderLayout.WEST)
        searchPanel.add(searchField, BorderLayout.CENTER)
        
        // 章节列表
        chapterList = JBList<String>()
        chapterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapterList.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        
        val scrollPane = JBScrollPane(chapterList)
        scrollPane.border = JBUI.Borders.empty(5, 10, 5, 10)
        
        // 操作按钮面板
        val buttonPanel = JPanel()
        val openReaderButton = JButton("打开阅读器")
        val refreshButton = JButton("刷新章节")
        
        openReaderButton.addActionListener {
            openReader()
        }
        
        refreshButton.addActionListener {
            refreshChapters()
        }
        
        buttonPanel.add(openReaderButton)
        buttonPanel.add(refreshButton)
        buttonPanel.border = JBUI.Borders.empty(5, 10, 5, 10)
        
        // 组装界面
        add(infoPanel, BorderLayout.NORTH)
        add(searchPanel, BorderLayout.CENTER)
        add(scrollPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 搜索框事件
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterChapters()
            }
        })
        
        // 章节列表双击事件
        chapterList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    openReader()
                }
            }
        })
    }
    
    /**
     * 自动加载最近阅读的书籍
     */
    private fun loadCurrentReadingBook() {
        try {
            val books = dataStorageService.loadAllBooks()
            if (books.isNotEmpty()) {
                // 获取最近阅读的书籍（loadAllBooks已经按最后阅读时间排序）
                val recentBook = books.first()
                setCurrentBook(recentBook)
            } else {
                bookInfoLabel.text = "书架中暂无书籍"
                chapterCountLabel.text = ""
            }
        } catch (e: Exception) {
            bookInfoLabel.text = "加载书籍失败: ${e.message}"
            chapterCountLabel.text = ""
        }
    }
    
    /**
     * 设置当前书籍
     */
    fun setCurrentBook(book: Book) {
        currentBook = book
        bookInfoLabel.text = "《${book.title}》"
        loadChapters()
    }
    
    /**
     * 同步当前阅读的书籍（从阅读器调用）
     */
    fun syncCurrentReadingBook() {
        loadCurrentReadingBook()
    }
    
    /**
     * 加载章节列表
     */
    private fun loadChapters() {
        val book = currentBook ?: return
        
        try {
            // 检查文件是否存在
            if (!book.file.exists()) {
                showError("书籍文件不存在: ${book.filePath}")
                return
            }
            
            // 解析章节（使用缓存）
            chapters = chapterParserService.parseChapters(book.file, book.id)
            if (chapters.isEmpty()) {
                showError("无法解析章节内容")
                return
            }
            
            // 创建章节标题列表
            allChapterTitles = chapters.mapIndexed { _, chapter ->
                "${chapter.index}. ${chapter.title}"
            }
            filteredChapterTitles = allChapterTitles
            
            // 更新UI
            updateChapterList()
            updateChapterCount()
            
        } catch (e: Exception) {
            showError("加载章节失败: ${e.message}")
        }
    }
    
    /**
     * 更新章节列表显示
     */
    private fun updateChapterList() {
        val model = DefaultListModel<String>()
        filteredChapterTitles.forEach { model.addElement(it) }
        chapterList.model = model
    }
    
    /**
     * 更新章节数量显示
     */
    private fun updateChapterCount() {
        val total = chapters.size
        val filtered = filteredChapterTitles.size
        chapterCountLabel.text = if (filtered == total) "共 $total 章" else "显示 $filtered / $total 章"
    }
    
    /**
     * 过滤章节
     */
    private fun filterChapters() {
        val searchText = searchField.text.trim().lowercase()
        
        if (searchText.isEmpty()) {
            filteredChapterTitles = allChapterTitles
        } else {
            filteredChapterTitles = allChapterTitles.filter { 
                it.lowercase().contains(searchText) 
            }
        }
        
        updateChapterList()
        updateChapterCount()
    }
    
    /**
     * 获取选中的章节索引
     */
    fun getSelectedChapterIndex(): Int {
        val selectedIndex = chapterList.selectedIndex
        if (selectedIndex >= 0 && selectedIndex < filteredChapterTitles.size) {
            val selectedTitle = filteredChapterTitles[selectedIndex]
            return allChapterTitles.indexOf(selectedTitle)
        }
        return -1
    }
    
    /**
     * 打开阅读器
     */
    private fun openReader() {
        val book = currentBook ?: return
        val selectedIndex = getSelectedChapterIndex()
        
        val updatedBook = if (selectedIndex >= 0) {
            // 更新书籍的当前章节
            book.copy(
                currentChapterIndex = selectedIndex,
                currentChapterTitle = chapters[selectedIndex].title,
                currentChapterOriginalTitle = chapters[selectedIndex].originalTitle,
                lastReadTime = System.currentTimeMillis()
            )
        } else {
            book
        }
        
        // 保存更新后的书籍信息
        dataStorageService.saveBook(updatedBook)
        
        // 通知阅读器打开书籍
        notifyReaderOpenBook(updatedBook)
    }
    
    /**
     * 刷新章节
     */
    private fun refreshChapters() {
        loadChapters()
    }
    
    /**
     * 通知阅读器打开书籍
     */
    private fun notifyReaderOpenBook(book: Book) {
        try {
            // 获取HelloRead工具窗口
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                // 切换到阅读标签页
                val contentManager = HelloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    contentManager.setSelectedContent(readerContent)
                    HelloReadToolWindow.activate {
                        // 获取阅读器组件并跳转到指定章节
                        val readerComponent = readerContent.component as? BookReaderToolWindow
                        if (readerComponent != null) {
                            // 从章节列表进入，不使用阅读记录，直接跳转到指定章节
                            readerComponent.openBook(book, useReadingRecord = false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            showError("打开阅读器失败: ${e.message}")
        }
    }
    
    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        bookInfoLabel.text = message
        chapterCountLabel.text = ""
        chapterList.model = DefaultListModel<String>()
    }
    
    /**
     * 清空章节列表
     */
    fun clear() {
        currentBook = null
        chapters = emptyList()
        allChapterTitles = emptyList()
        filteredChapterTitles = emptyList()
        bookInfoLabel.text = "请选择一本书查看章节"
        chapterCountLabel.text = ""
        searchField.text = ""
        chapterList.model = DefaultListModel<String>()
    }
}
