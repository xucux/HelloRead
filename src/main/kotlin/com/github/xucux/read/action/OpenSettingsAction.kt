package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 打开设置标签页
 */
class OpenSettingsAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                val contentManager = HelloReadToolWindow.contentManager
                val settingsContent = contentManager.contents.find { it.displayName == TabConstants.SETTINGS_TAB }
                if (settingsContent != null) {
                    contentManager.setSelectedContent(settingsContent)
                    HelloReadToolWindow.activate {
                        // 获取设置组件并重新加载设置
                        val settingsComponent = settingsContent.component as? com.github.xucux.read.ui.SettingsToolWindow
                        settingsComponent?.reloadSettings()
                    }
                }
            }
        } catch (e: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "打开设置失败: ${e.message}",
                "错误"
            )
        }
    }
}
