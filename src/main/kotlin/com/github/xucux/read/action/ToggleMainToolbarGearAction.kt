package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.service.DisplaySettingsService
import com.github.xucux.read.ui.ToolWindowHeaderVisibilityHelper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 工具窗口齿轮菜单：隐藏/显示顶部栏
 */
class ToggleMainToolbarGearAction : AnAction(), DumbAware {

    override fun update(e: AnActionEvent) {
        val isVisible = DisplaySettingsService.getInstance().loadToolWindowHeaderVisible()
        e.presentation.text = if (isVisible) "隐藏顶部栏" else "显示顶部栏"
        e.presentation.description = if (isVisible) "隐藏HelloRead工具窗口顶部栏" else "显示HelloRead工具窗口顶部栏"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val displaySettingsService = DisplaySettingsService.getInstance()
        val newVisible = !displaySettingsService.loadToolWindowHeaderVisible()

        runCatching {
            displaySettingsService.saveToolWindowHeaderVisible(newVisible)
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
                ?: throw IllegalStateException("未找到HelloRead工具窗口")

            val success = ToolWindowHeaderVisibilityHelper.apply(toolWindow, newVisible)
            if (!success) {
                throw IllegalStateException("当前IDE版本不支持切换HelloRead顶部栏可见性")
            }
        }.onFailure { error ->
            Messages.showErrorDialog(
                project,
                "切换顶部栏显示状态失败: ${error.message ?: "未知错误"}",
                "HelloRead"
            )
        }
    }
}
