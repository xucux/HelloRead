package com.github.xucux.read.ui

import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.service.ChapterParserService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.openapi.diagnostic.logger
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import java.util.Locale

/**
 * 书籍章节列表对话框
 */
class BookChaptersDialog(
    project: Project,
    private val book: Book
) : DialogWrapper(project) {
    
    companion object {
        private val logger = logger<BookChaptersDialog>()
    }
    
    private val chapterParserService = ChapterParserService.getInstance()
    
    private lateinit var chapterList: JBList<String>
    private lateinit var searchField: JBTextField
    private var selectedIndex: Int = book.currentChapterIndex
    private val allChapterTitles: List<String>
    private var filteredChapterTitles: List<String> = emptyList()
    private var chapters: List<Chapter> = emptyList()
    
    init {
        title = "《${book.title}》章节列表"
        init()
        
        // 解析章节（使用缓存）
        try {
            chapters = chapterParserService.parseChapters(book.file, book.id)
            if (chapters.isEmpty()) {
                // 如果解析失败，尝试显示错误信息
                if (!book.file.exists()) {
                    chapters = listOf(Chapter(0, "错误：书籍文件不存在", "", 0, 0, "错误：书籍文件不存在", ""))
                } else {
                    chapters = listOf(Chapter(0, "错误：无法解析章节结构，请尝试重新解析书籍", "", 0, 0, "错误：无法解析章节结构，请尝试重新解析书籍", ""))
                }
            }
        } catch (e: Exception) {
            logger.error("解析章节时发生异常: ${book.title}", e)
            chapters = listOf(Chapter(0, "错误：解析章节时发生异常 - ${e.message}", "", 0, 0, "错误：解析章节时发生异常 - ${e.message}", ""))
        }
        
        allChapterTitles = chapters.mapIndexed { index, chapter ->
            "${index + 1}. ${chapter.title}"
        }
        filteredChapterTitles = allChapterTitles
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 标题面板
        val titlePanel = JPanel(BorderLayout())
        val titleLabel = JLabel("章节列表:")
        titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        titleLabel.border = JBUI.Borders.empty(10, 10, 5, 10)
        titlePanel.add(titleLabel, BorderLayout.WEST)
        
        // 章节统计信息
        val isErrorState = chapters.isNotEmpty() && chapters.first().title.startsWith("错误：")
        val statsLabel = JLabel(if (isErrorState) "解析失败" else "共 ${chapters.size} 章")
        statsLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        statsLabel.border = JBUI.Borders.empty(10, 10, 5, 10)
        titlePanel.add(statsLabel, BorderLayout.EAST)
        
        panel.add(titlePanel, BorderLayout.NORTH)
        
        // 搜索面板
        val searchPanel = JPanel(BorderLayout())
        val searchLabel = JLabel("搜索:")
        searchLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        searchLabel.border = JBUI.Borders.empty(5, 10, 5, 5)
        searchPanel.add(searchLabel, BorderLayout.WEST)
        
        searchField = JBTextField()
        searchField.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        searchField.toolTipText = "输入关键词搜索章节"
        searchField.border = JBUI.Borders.empty(5, 5, 5, 10)
        
        // 添加搜索功能
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterChapters()
            }
        })
        
        searchPanel.add(searchField, BorderLayout.CENTER)
        panel.add(searchPanel, BorderLayout.CENTER)
        
        // 章节列表
        chapterList = JBList(filteredChapterTitles)
        chapterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapterList.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        chapterList.selectedIndex = if (isErrorState) -1 else selectedIndex
        chapterList.isEnabled = !isErrorState
        
        val scrollPane = JBScrollPane(chapterList)
        scrollPane.border = JBUI.Borders.empty(5, 10, 5, 10)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 添加双击事件监听器
        chapterList.addListSelectionListener {
            selectedIndex = getOriginalIndex(chapterList.selectedIndex)
        }
        
        // 添加双击事件
        chapterList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    selectedIndex = getOriginalIndex(chapterList.selectedIndex)
                    if (selectedIndex >= 0) {
                        doOKAction()
                    }
                }
            }
        })
        
        return panel
    }
    
    override fun doOKAction() {
        // 检查是否是错误状态
        val isErrorState = chapters.isNotEmpty() && chapters.first().title.startsWith("错误：")
        if (isErrorState) {
            // 在错误状态下，只允许取消
            return
        }
        
        selectedIndex = getOriginalIndex(chapterList.selectedIndex)
        if (selectedIndex >= 0 && selectedIndex < chapters.size) {
            super.doOKAction()
        }
    }
    
    /**
     * 获取选中的章节索引
     */
    fun getSelectedChapterIndex(): Int {
        return selectedIndex
    }
    
    /**
     * 获取选中的章节
     */
    fun getSelectedChapter(): Chapter? {
        // 检查是否是错误状态
        val isErrorState = chapters.isNotEmpty() && chapters.first().title.startsWith("错误：")
        if (isErrorState) {
            return null
        }
        
        return if (selectedIndex >= 0 && selectedIndex < chapters.size) {
            chapters[selectedIndex]
        } else {
            null
        }
    }
    
    /**
     * 过滤章节列表
     */
    private fun filterChapters() {
        val searchText = searchField.text.trim().lowercase(Locale.getDefault())
        
        if (searchText.isEmpty()) {
            filteredChapterTitles = allChapterTitles
        } else {
            filteredChapterTitles = allChapterTitles.filter { title ->
                title.lowercase(Locale.getDefault()).contains(searchText)
            }
        }
        
        // 更新列表
        chapterList.setListData(filteredChapterTitles.toTypedArray())
        
        // 如果有搜索结果，选择第一个
        if (filteredChapterTitles.isNotEmpty()) {
            chapterList.selectedIndex = 0
        }
    }
    
    /**
     * 获取原始章节索引（考虑过滤后的索引映射）
     */
    private fun getOriginalIndex(filteredIndex: Int): Int {
        if (filteredIndex < 0 || filteredIndex >= filteredChapterTitles.size) {
            return -1
        }
        
        val filteredTitle = filteredChapterTitles[filteredIndex]
        return allChapterTitles.indexOf(filteredTitle)
    }
    
    override fun getPreferredSize(): Dimension {
        return Dimension(500, 600)
    }
    
    override fun getPreferredFocusedComponent(): JComponent = searchField
}