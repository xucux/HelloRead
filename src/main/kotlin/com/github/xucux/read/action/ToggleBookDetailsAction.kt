package com.github.xucux.read.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager
import com.github.xucux.read.ui.BookshelfToolWindow

/**
 * 切换书籍详情显示状态的动作
 */
class ToggleBookDetailsAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 获取书架工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val bookshelfToolWindow = toolWindowManager.getToolWindow("Bookshelf")
        
        if (bookshelfToolWindow != null) {
            bookshelfToolWindow.activate {
                val content = bookshelfToolWindow.contentManager.contents.firstOrNull()
                if (content != null) {
                    val bookshelfComponent = content.component as? BookshelfToolWindow
                    bookshelfComponent?.toggleBookDetailsVisibility()
                }
            }
        }
    }
}
