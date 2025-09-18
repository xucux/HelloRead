package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.slf4j.LoggerFactory

/**
 * 打开我的书架动作
 */
class OpenBookshelfAction : AnAction(), DumbAware {
    
    companion object {
        private val logger = LoggerFactory.getLogger(OpenBookshelfAction::class.java)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                val contentManager = HelloReadToolWindow.contentManager
                val bookshelfContent = contentManager.contents.find { it.displayName == TabConstants.BOOKSHELF_TAB }
                if (bookshelfContent != null) {
                    contentManager.setSelectedContent(bookshelfContent)
                    HelloReadToolWindow.activate(null)
                }
            }
        } catch (e: Exception) {
            logger.error("打开书架失败", e)
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "打开书架失败: ${e.message}",
                "错误"
            )
        }
    }
}
