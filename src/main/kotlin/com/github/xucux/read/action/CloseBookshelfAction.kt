package com.github.xucux.read.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 关闭书架动作
 */
class CloseBookshelfAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 获取书架工具窗口
        val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
        val bookshelfToolWindow = toolWindowManager.getToolWindow("Bookshelf")
        
        if (bookshelfToolWindow != null) {
            // 隐藏工具窗口
            bookshelfToolWindow.hide(null)
        }
    }
}
