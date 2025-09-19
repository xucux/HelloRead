package com.github.xucux.read.ui

import com.github.xucux.read.model.Chapter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import java.util.Locale

/**
 * 章节列表选择对话框
 */
class ChapterListDialog(
    project: Project,
    private val chapters: List<Chapter>,
    private val currentChapterIndex: Int
) : DialogWrapper(project) {
    
    private lateinit var chapterList: JBList<String>
    private lateinit var searchField: JBTextField
    private var selectedIndex: Int = currentChapterIndex
    private val allChapterTitles: List<String>
    private var filteredChapterTitles: List<String> = emptyList()
    
    init {
        // 创建章节列表
        allChapterTitles = chapters.mapIndexed { _, chapter ->
            "${chapter.index}. ${chapter.title}"
        }
        filteredChapterTitles = allChapterTitles
        
        title = "章节列表"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 初始化组件
        chapterList = JBList(filteredChapterTitles)
        chapterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapterList.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        chapterList.selectedIndex = currentChapterIndex
        
        // 创建搜索框
        searchField = JBTextField()
        searchField.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        searchField.toolTipText = "输入关键词搜索章节"
        
        // 标题面板
        val titlePanel = JPanel(BorderLayout())
        val titleLabel = JLabel("选择章节:")
        titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        titleLabel.border = JBUI.Borders.empty(10, 10, 5, 10)
        titlePanel.add(titleLabel, BorderLayout.WEST)
        
        // 章节统计信息
        val statsLabel = JLabel("共 ${chapters.size} 章")
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
        
        searchField.border = JBUI.Borders.empty(5, 5, 5, 10)
        searchPanel.add(searchField, BorderLayout.CENTER)
        
        // 添加搜索功能
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterChapters()
            }
        })
        
        panel.add(searchPanel, BorderLayout.CENTER)
        
        // 章节列表
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
     * 过滤章节列表
     */
    private fun filterChapters() {
        val searchText = searchField.text.trim().toLowerCase(Locale.getDefault())
        
        if (searchText.isEmpty()) {
            filteredChapterTitles = allChapterTitles
        } else {
            filteredChapterTitles = allChapterTitles.filter { title ->
                title.toLowerCase(Locale.getDefault()).contains(searchText)
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
    
    override fun getPreferredFocusedComponent(): JComponent = searchField
    
    override fun getPreferredSize(): Dimension {
        return Dimension(450, 600)
    }
}
