package com.github.xucux.read.action

import com.github.xucux.read.model.Book
import com.github.xucux.read.service.ChapterParserService
import com.github.xucux.read.service.DataStorageService
import com.github.xucux.read.util.PopNotifyUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import org.slf4j.LoggerFactory

/**
 * 重新解析章节动作
 */
class ReparseBookAction : AnAction(), DumbAware {
    
    companion object {
        private val logger = LoggerFactory.getLogger(ReparseBookAction::class.java)
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
                            // 执行重新解析
                            reparseBook(selectedBook, bookshelfComponent)
                        } else {
                            Messages.showErrorDialog("请先在书架中选择要重新解析的书籍", "提示")
                        }
                    }
                }
            }
        } else {
            Messages.showErrorDialog("请先打开我的书架", "提示")
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
     * 重新解析书籍
     */
    private fun reparseBook(book: Book, bookshelfComponent: com.github.xucux.read.ui.BookshelfToolWindow) {
        try {
            // 检查文件是否仍然存在
            if (!book.file.exists()) {
                Messages.showErrorDialog(
                    "书籍文件不存在: ${book.filePath}",
                    "重新解析失败"
                )
                return
            }
            
            // 显示确认对话框
            val result = Messages.showYesNoDialog(
                "确定要重新解析书籍《${book.title}》吗？\n\n" +
                "这将重新分析章节结构，可能会影响当前的阅读进度。",
                "确认重新解析",
                "重新解析",
                "取消",
                Messages.getQuestionIcon()
            )
            
            if (result != 0) {
                return
            }
            
            // 显示进度提示
            Messages.showInfoMessage(
                "正在重新解析《${book.title}》...",
                "重新解析中"
            )
            
            // 重新解析章节（强制清除缓存）
            val chapterParserService = ChapterParserService.getInstance()
            val newChapters = chapterParserService.reparseChapters(book.file, book.id)
            
            if (newChapters.isEmpty()) {
                Messages.showErrorDialog(
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
            val dataStorageService = DataStorageService.getInstance()
            dataStorageService.saveBook(updatedBook)
            
            // 清除旧的阅读记录（可选，用户可能希望保留）
            val clearRecordResult = Messages.showYesNoDialog(
                "是否清除旧的阅读记录？\n\n" +
                "选择是将清除阅读进度，从第一章开始阅读\n" +
                "选择否将保留当前阅读进度",
                "清除阅读记录",
                "清除",
                "保留",
                Messages.getQuestionIcon()
            )
            
            if (clearRecordResult == 0) {
                dataStorageService.removeReadingRecord(book.id)
            }
            
            // 刷新书籍列表
            bookshelfComponent.loadBooks()
            
            Messages.showInfoMessage(
                "重新解析完成！\n\n" +
                "原章节数: ${book.totalChapters}\n" +
                "新章节数: ${newChapters.size}\n" +
                "解析状态：${if (newChapters.size != book.totalChapters) "章节数已更新" else "章节数无变化"}",
                "重新解析成功"
            )
            
        } catch (e: Exception) {
            logger.error("重新解析书籍失败: ${book.title}", e)
            PopNotifyUtil.notify("解析失败", "重新解析失败: ${e.message}")
            Messages.showErrorDialog(
                "重新解析失败: ${e.message}",
                "重新解析失败"
            )
        }
    }
}