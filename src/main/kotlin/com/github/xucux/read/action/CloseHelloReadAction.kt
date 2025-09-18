package com.github.xucux.read.action

import com.github.xucux.read.constants.MessageConstants
import com.github.xucux.read.constants.TabConstants
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 关闭HelloRead工具窗口
 */
class CloseHelloReadAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                HelloReadToolWindow.hide(null)
            }
        } catch (e: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "${MessageConstants.CLOSE_HELLO_READ_FAILED}: ${e.message}",
                "错误"
            )
        }
    }
}
