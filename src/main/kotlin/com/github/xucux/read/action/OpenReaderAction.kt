package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 打开阅读器动作
 */
class OpenReaderAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                val contentManager = HelloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    contentManager.setSelectedContent(readerContent)
                    HelloReadToolWindow.activate(null)
                }
            }
        } catch (e: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "打开阅读器失败: ${e.message}",
                "错误"
            )
        }
    }
}
