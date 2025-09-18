package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.slf4j.LoggerFactory

/**
 * 打开章节列表标签页
 */
class OpenChapterListAction : AnAction(), DumbAware {
    
    companion object {
        private val logger = LoggerFactory.getLogger(OpenChapterListAction::class.java)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                val contentManager = HelloReadToolWindow.contentManager
                val chapterListContent = contentManager.contents.find { it.displayName == TabConstants.CHAPTER_LIST_TAB }
                if (chapterListContent != null) {
                    contentManager.setSelectedContent(chapterListContent)
                    HelloReadToolWindow.activate(null)
                }
            }
        } catch (e: Exception) {
            logger.error("打开章节列表失败", e)
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "打开章节列表失败: ${e.message}",
                "错误"
            )
        }
    }
}
