package com.github.xucux.read.service.notify

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.ui.BookReaderToolWindow
import com.intellij.openapi.project.Project

/**
 * 阅读器通知服务
 * 负责通知阅读器组件更新设置
 */
class ReaderNotificationService(private val project: Project) {
    
    /**
     * 通知阅读器更新字体设置
     */
    fun notifyReaderUpdateFont() {
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val helloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)

            if (helloReadToolWindow != null) {
                val contentManager = helloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    val readerComponent = readerContent.component as? BookReaderToolWindow
                    readerComponent?.loadFontSettings()
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 通知阅读器更新显示设置
     */
    fun notifyReaderUpdateDisplay(
        hideOperationPanel: Boolean,
        hideTitleButton: Boolean,
        hideProgressLabel: Boolean,
        backgroundColor: String = ""
    ) {
        try {
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val helloReadToolWindow = toolWindowManager.getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)

            if (helloReadToolWindow != null) {
                val contentManager = helloReadToolWindow.contentManager
                val readerContent = contentManager.contents.find { it.displayName == TabConstants.READER_TAB }
                if (readerContent != null) {
                    val readerComponent = readerContent.component as? BookReaderToolWindow
                    readerComponent?.updateDisplaySettings(
                        hideOperationPanel,
                        hideTitleButton,
                        hideProgressLabel,
                        backgroundColor
                    )
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
    }
}
