package com.github.xucux.read.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 关闭阅读器动作
 */
class CloseReaderAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 获取阅读器工具窗口
        val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
        val readerToolWindow = toolWindowManager.getToolWindow("BookReader")

        if (readerToolWindow != null) {
            // 隐藏工具窗口
            readerToolWindow.hide(null)
        }
    }
}