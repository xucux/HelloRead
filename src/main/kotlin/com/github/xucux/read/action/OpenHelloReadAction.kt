package com.github.xucux.read.action

import com.github.xucux.read.constants.MessageConstants
import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.util.PopNotifyUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 打开HelloRead主窗口
 */
class OpenHelloReadAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                HelloReadToolWindow.activate(null)
            }
        } catch (e: Exception) {
            PopNotifyUtil.notify("打开失败", "${MessageConstants.OPEN_HELLO_READ_FAILED}: ${e.message}")
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "${MessageConstants.OPEN_HELLO_READ_FAILED}: ${e.message}",
                "错误"
            )
        }
    }
}
