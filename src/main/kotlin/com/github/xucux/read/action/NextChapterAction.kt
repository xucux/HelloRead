package com.github.xucux.read.action

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.ReadingMode
import com.github.xucux.read.service.StatusBarReadingService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.slf4j.LoggerFactory

/**
 * 下一章快捷键动作
 */
class NextChapterAction : AnAction(), DumbAware {
    
    companion object {
        private val logger = LoggerFactory.getLogger(NextChapterAction::class.java)
        private val statusBarReadingService = StatusBarReadingService.getInstance()
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            // 检查是否处于状态栏阅读模式
            if (statusBarReadingService.isActive()) {
                // 状态栏阅读模式：通过工具窗口执行下一章操作
                val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
                
                if (HelloReadToolWindow != null) {
                    val contentManager = HelloReadToolWindow.contentManager
                    val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                    if (readerContent != null) {
                        val readerComponent = readerContent.component as? com.github.xucux.read.ui.BookReaderToolWindow
                        readerComponent?.navigateToNextChapter()
                    }
                }
                return
            }
            
            // 获取HelloRead工具窗口
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
            
            if (HelloReadToolWindow != null) {
                // 激活工具窗口
                HelloReadToolWindow.activate(null)
                
                // 获取阅读器组件并执行下一章操作
                val contentManager = HelloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    val readerComponent = readerContent.component as? com.github.xucux.read.ui.BookReaderToolWindow
                    if (readerComponent != null) {
                        readerComponent.navigateToNextChapter()
                    } else {
                        logger.warn("阅读器组件为空，无法执行下一章操作")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("执行下一章操作时发生异常", e)
        }
    }
    
    override fun update(e: AnActionEvent) {
        // 可以根据当前状态动态启用/禁用快捷键
        val project = e.project
        if (project != null) {
            try {
                val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                val HelloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
                
                if (HelloReadToolWindow != null) {
                    val contentManager = HelloReadToolWindow.contentManager
                    val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                    if (readerContent != null) {
                        val readerComponent = readerContent.component as? com.github.xucux.read.ui.BookReaderToolWindow
                        val canNavigate = readerComponent?.canNavigateToNextChapter() ?: false
                        e.presentation.isEnabled = canNavigate
                    } else {
                        e.presentation.isEnabled = false
                    }
                } else {
                    e.presentation.isEnabled = false
                }
            } catch (ex: Exception) {
                e.presentation.isEnabled = false
                logger.warn("更新下一章按钮状态时发生异常", ex)
            }
        } else {
            e.presentation.isEnabled = false
        }
    }
}
