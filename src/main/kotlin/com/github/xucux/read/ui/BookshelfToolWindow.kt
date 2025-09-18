package com.github.xucux.read.ui

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.Book
import com.github.xucux.read.service.DataStorageService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import com.intellij.ui.table.JBTable
import java.awt.FlowLayout
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel


/**
 * 我的书架工具窗口
 */
class BookshelfToolWindow(val project: Project) : SimpleToolWindowPanel(true, true) {

    private val dataStorageService = DataStorageService.getInstance()
    private val bookTable: JBTable
    private val tableModel: DefaultTableModel
    private val books: MutableList<Book> = mutableListOf()

    // 书籍详情显示状态
    private var isBookDetailsVisible = true
    private val toolbar: JPanel

    init {
        layout = BorderLayout()

        // 创建工具栏
        toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)

        // 创建表格模型
        val columnNames = arrayOf("书名", "当前章节", "进度", "最后阅读")
        tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
        bookTable = JBTable(tableModel)

        // 配置表格
        setupTable()

        // 创建滚动面板
        val scrollPane = JBScrollPane(bookTable)
        add(scrollPane, BorderLayout.CENTER)

        // 加载书籍数据
        loadBooks()
    }

    /**
     * 创建工具栏
     */
    private fun createToolbar(): JPanel {
        val toolbar = JPanel()
        toolbar.layout = FlowLayout(FlowLayout.LEFT)
        val addButton = JButton("添加书籍")
        addButton.addActionListener {
            val dialog = AddBookDialog(project)
            if (dialog.showAndGet()) {
                loadBooks() // 重新加载书籍列表
            }
        }

        val refreshButton = JButton("刷新")
        refreshButton.addActionListener {
            loadBooks()
        }

        val reparseButton = JButton("重新解析")
        reparseButton.addActionListener {
            val selectedRow = bookTable.selectedRow
            if (selectedRow >= 0 && selectedRow < books.size) {
                val selectedBook = books[selectedRow]
                reparseBook(selectedBook)
            } else {
                com.intellij.openapi.ui.Messages.showErrorDialog("请先选择要重新解析的书籍", "提示")
            }
        }


        val removeButton = JButton("删除书籍")
        removeButton.addActionListener {
            val selectedRow = bookTable.selectedRow
            if (selectedRow >= 0 && selectedRow < books.size) {
                val selectedBook = books[selectedRow]
                val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
                    "确定要删除书籍《${selectedBook.title}》吗？",
                    "确认删除",
                    "删除",
                    "取消",
                    com.intellij.openapi.ui.Messages.getQuestionIcon()
                )
                if (result == 0) {
                    dataStorageService.removeBook(selectedBook.id)
                    dataStorageService.removeReadingRecord(selectedBook.id)
                    loadBooks()
                }
            } else {
                com.intellij.openapi.ui.Messages.showErrorDialog("请先选择要删除的书籍", "提示")
            }
        }

        toolbar.add(addButton)
        toolbar.add(refreshButton)
        toolbar.add(reparseButton)
        toolbar.add(removeButton)

        return toolbar
    }

    /**
     * 配置表格
     */
    private fun setupTable() {
        bookTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        bookTable.setShowGrid(true)
        bookTable.rowHeight = 30
        bookTable.tableHeader.resizingAllowed = true
        bookTable.autoResizeMode = JBTable.AUTO_RESIZE_LAST_COLUMN

        // 设置列宽
        setupColumnWidths()

        // 添加双击事件监听器
        bookTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedRow = bookTable.selectedRow
                    if (selectedRow >= 0 && selectedRow < books.size) {
                        val selectedBook = books[selectedRow]
                        openBookReader(selectedBook)
                    }
                }
            }
        })

        // 添加列宽调整监听器
        bookTable.tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                saveColumnWidths()
            }
        })
    }

    /**
     * 设置列宽
     */
    private fun setupColumnWidths() {
        val columnModel = bookTable.columnModel
        val columnWidths = dataStorageService.loadColumnWidths()

        // 设置各列的默认宽度
        val defaultWidths = intArrayOf(200, 250, 80, 120)
        val columnNames = arrayOf("书名", "当前章节", "进度", "最后阅读")

        for (i in 0 until columnModel.columnCount) {
            val column = columnModel.getColumn(i)
            val savedWidth = columnWidths[columnNames[i]]
            column.preferredWidth = savedWidth ?: defaultWidths[i]
            column.minWidth = 50 // 设置最小宽度
        }
    }

    /**
     * 保存列宽设置
     */
    private fun saveColumnWidths() {
        val columnModel = bookTable.columnModel
        val columnNames = arrayOf("书名", "当前章节", "进度", "最后阅读")
        val columnWidths = mutableMapOf<String, Int>()

        for (i in 0 until columnModel.columnCount) {
            val column = columnModel.getColumn(i)
            columnWidths[columnNames[i]] = column.width
        }

        dataStorageService.saveColumnWidths(columnWidths)
    }

    /**
     * 加载书籍数据
     */
    fun loadBooks() {
        books.clear()
        books.addAll(dataStorageService.loadAllBooks())

        // 清空表格
        tableModel.rowCount = 0

        // 添加书籍数据到表格
        for (book in books) {
            val rowData = arrayOf(
                book.title,
                book.getDisplayChapterInfo(),
                "${(book.getReadingProgress() * 100).toInt()}%",
                formatLastReadTime(book.lastReadTime)
            )
            tableModel.addRow(rowData)
        }

        bookTable.repaint()
    }

    /**
     * 格式化最后阅读时间
     */
    private fun formatLastReadTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("MM-dd HH:mm")
        return formatter.format(date)
    }

    /**
     * 重新解析书籍章节
     */
    private fun reparseBook(book: Book) {
        try {
            // 检查文件是否仍然存在
            if (!book.file.exists()) {
                com.intellij.openapi.ui.Messages.showErrorDialog(
                    "书籍文件不存在: ${book.filePath}",
                    "重新解析失败"
                )
                return
            }

            // 显示确认对话框
            val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
                "确定要重新解析书籍《${book.title}》吗？\n\n" +
                "这将重新分析章节结构，可能会影响当前的阅读进度。",
                "确认重新解析",
                "重新解析",
                "取消",
                com.intellij.openapi.ui.Messages.getQuestionIcon()
            )

            if (result != 0) {
                return
            }

            // 显示进度提示
            com.intellij.openapi.ui.Messages.showInfoMessage(
                "正在重新解析《${book.title}》...",
                "重新解析中"
            )

            // 重新解析章节（强制清除缓存）
            val chapterParserService = com.github.xucux.read.service.ChapterParserService.getInstance()
            val newChapters = chapterParserService.reparseChapters(book.file, book.id)

            if (newChapters.isEmpty()) {
                com.intellij.openapi.ui.Messages.showErrorDialog(
                    "重新解析失败：无法识别章节结构",
                    "重新解析失败"
                )
                return
            }

            // 计算文件总行数
            val totalLines = chapterParserService.calculateTotalLines(book.file)
            
            // 更新书籍信息
            val updatedBook = book.copy(
                totalChapters = newChapters.size,
                totalLines = totalLines,
                currentChapterIndex = 0, // 重置到第一章
                currentChapterTitle = newChapters.firstOrNull()?.title ?: "",
                currentChapterOriginalTitle = newChapters.firstOrNull()?.originalTitle ?: ""
            )

            // 保存更新后的书籍信息
            dataStorageService.saveBook(updatedBook)

            // 清除旧的阅读记录（可选，用户可能希望保留）
            val clearRecordResult = com.intellij.openapi.ui.Messages.showYesNoDialog(
                "是否清除旧的阅读记录？\n\n" +
                "选择'是'将清除阅读进度，从第一章开始阅读\n" +
                "选择'否'将保留当前阅读进度",
                "清除阅读记录",
                "清除",
                "保留",
                com.intellij.openapi.ui.Messages.getQuestionIcon()
            )

            if (clearRecordResult == 0) {
                dataStorageService.removeReadingRecord(book.id)
            }

            // 刷新书籍列表
            loadBooks()

            com.intellij.openapi.ui.Messages.showInfoMessage(
                "重新解析完成！\n\n" +
                "原章节数：${book.totalChapters}\n" +
                "新章节数：${newChapters.size}\n" +
                "解析状态：${if (newChapters.size != book.totalChapters) "章节数已更新" else "章节数无变化"}",
                "重新解析成功"
            )

        } catch (e: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "重新解析失败: ${e.message}",
                "重新解析失败"
            )
        }
    }


    /**
     * 打开书籍阅读器
     */
    private fun openBookReader(book: Book) {
        try {
            // 获取HelloRead工具窗口
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val helloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)

            if (helloReadToolWindow != null) {
                // 切换到阅读标签页
                val contentManager = helloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    contentManager.setSelectedContent(readerContent)
                    helloReadToolWindow.activate {
                        // 获取阅读器组件并打开书籍（从书架进入，使用阅读记录）
                        val readerComponent = readerContent.component as? BookReaderToolWindow
                        readerComponent?.openBook(book, useReadingRecord = true)
                    }
                }
            }
        } catch (e: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "打开阅读器失败: ${e.message}",
                "错误"
            )
        }
    }


    /**
     * 切换书籍详情显示状态
     */
    fun toggleBookDetailsVisibility() {
        isBookDetailsVisible = !isBookDetailsVisible
        toolbar.isVisible = isBookDetailsVisible
        revalidate()
        repaint()
    }

    /**
     * 当书架窗口显示时调用此方法
     * 这个方法会在ContentManagerListener中被调用
     */
    fun onWindowShown() {
        loadBooks()
    }
}