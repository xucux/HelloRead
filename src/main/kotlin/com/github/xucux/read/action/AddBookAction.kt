package com.github.xucux.read.action

import com.github.xucux.read.ui.AddBookDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 添加书籍动作
 */
class AddBookAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val dialog = AddBookDialog(project)
        if (dialog.showAndGet()) {
            // 刷新书架
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val bookshelfToolWindow = toolWindowManager.getToolWindow("Bookshelf")
            
            bookshelfToolWindow?.activate {
                val content = bookshelfToolWindow.contentManager.contents.firstOrNull()
                if (content != null) {
                    val bookshelfComponent = content.component as? com.github.xucux.read.ui.BookshelfToolWindow
                    bookshelfComponent?.loadBooks()
                }
            }
        }
    }
}
