package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.Book
import com.github.xucux.read.util.PopNotifyUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.slf4j.LoggerFactory

/**
 * 查看章节列表动作
 */
class ViewChaptersAction : AnAction(), DumbAware {
    
    companion object {
        private val logger = LoggerFactory.getLogger(ViewChaptersAction::class.java)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 获取书架工具窗口
        val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
        val bookshelfToolWindow = toolWindowManager.getToolWindow("Bookshelf")
        
        if (bookshelfToolWindow != null) {
            bookshelfToolWindow.activate {
                val content = bookshelfToolWindow.contentManager.contents.firstOrNull()
                if (content != null) {
                    val bookshelfComponent = content.component as? com.github.xucux.read.ui.BookshelfToolWindow
                    if (bookshelfComponent != null) {
                        // 获取当前选中的书籍
                        val selectedBook = getSelectedBook(bookshelfComponent)
                        if (selectedBook != null) {
                            // 显示章节列表
                            showBookChapters(selectedBook, bookshelfComponent)
                        } else {
                            com.intellij.openapi.ui.Messages.showErrorDialog("请先在书架中选择要查看章节的书籍", "提示")
                        }
                    }
                }
            }
        } else {
            com.intellij.openapi.ui.Messages.showErrorDialog("请先打开我的书架", "提示")
        }
    }
    
    /**
     * 获取当前选中的书籍
     */
    private fun getSelectedBook(bookshelfComponent: com.github.xucux.read.ui.BookshelfToolWindow): Book? {
        // 通过反射获取选中的书籍
        return try {
            val bookTableField = bookshelfComponent.javaClass.getDeclaredField("bookTable")
            bookTableField.isAccessible = true
            val bookTable = bookTableField.get(bookshelfComponent)
            
            val selectedObjectMethod = bookTable.javaClass.getMethod("getSelectedObject")
            selectedObjectMethod.invoke(bookTable) as? Book
        } catch (e: Exception) {
            logger.error("获取选中书籍时发生异常", e)
            null
        }
    }
    
    /**
     * 显示书籍章节列表
     */
    private fun showBookChapters(book: Book, bookshelfComponent: com.github.xucux.read.ui.BookshelfToolWindow) {
        try {
            val dialog = com.github.xucux.read.ui.BookChaptersDialog(bookshelfComponent.project, book)
            if (dialog.showAndGet()) {
                val selectedChapter = dialog.getSelectedChapter()
                if (selectedChapter != null) {
                    // 询问是否跳转到该章节
                    val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
                        "是否跳转到章节：${selectedChapter.getDisplayName()}？",
                        "跳转章节",
                        "跳转",
                        "取消",
                        com.intellij.openapi.ui.Messages.getQuestionIcon()
                    )
                    
                    if (result == 0) {
                        // 更新书籍的当前章节信息
                        val dataStorageService = com.github.xucux.read.service.DataStorageService.getInstance()
                        val updatedBook = book.copy(
                            currentChapterIndex = dialog.getSelectedChapterIndex(),
                            currentChapterTitle = selectedChapter.title,
                            currentChapterOriginalTitle = selectedChapter.originalTitle,
                            lastReadTime = System.currentTimeMillis()
                        )
                        dataStorageService.saveBook(updatedBook)
                        
                        // 刷新书架
                        bookshelfComponent.loadBooks()
                        
                        // 打开阅读器并跳转到指定章节
                        openBookReader(updatedBook, bookshelfComponent)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("显示章节列表失败: ${book.title}", e)
            PopNotifyUtil.notify("章节列表错误", "显示章节列表失败: ${e.message}")
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "显示章节列表失败: ${e.message}",
                "错误"
            )
        }
    }
    
    /**
     * 打开书籍阅读器
     */
    private fun openBookReader(book: Book, bookshelfComponent: com.github.xucux.read.ui.BookshelfToolWindow) {
        try {
            // 获取HelloRead工具窗口（统一使用HelloRead工具窗口）
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(bookshelfComponent.project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                // 切换到阅读标签页
                val contentManager = HelloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    contentManager.setSelectedContent(readerContent)
                    HelloReadToolWindow.activate {
                        // 获取阅读器组件并跳转到指定章节
                        val readerComponent = readerContent.component as? com.github.xucux.read.ui.BookReaderToolWindow
                        if (readerComponent != null) {
                            // 从章节列表进入，不使用阅读记录，直接跳转到指定章节
                            readerComponent.openBook(book, useReadingRecord = false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("打开阅读器失败: ${book.title}", e)
            PopNotifyUtil.notify("阅读器错误", "打开阅读器失败: ${e.message}")
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "打开阅读器失败: ${e.message}",
                "错误"
            )
        }
    }
}
