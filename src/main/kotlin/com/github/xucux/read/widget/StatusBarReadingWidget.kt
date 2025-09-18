package com.github.xucux.read.widget

import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.service.StatusBarReadingService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * 状态栏阅读Widget
 * 参考w-reader项目的实现方式，继承EditorBasedStatusBarPopup
 */
class StatusBarReadingWidget(@NotNull project: Project) : EditorBasedStatusBarPopup(project, false) {
    
    private val statusBarReadingService = StatusBarReadingService.getInstance()
    
    companion object {
        const val WIDGET_ID = "HelloRead.StatusBarReading"
        
        /**
         * 查找Widget实例
         */
        @Nullable
        private fun findWidget(@NotNull project: Project): StatusBarReadingWidget? {
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            return statusBar?.getWidget(WIDGET_ID) as? StatusBarReadingWidget
        }
        
        /**
         * 更新状态栏显示
         */
        fun update(@NotNull project: Project, content: String) {
            val widget = findWidget(project)
            widget?.update {
                widget.myStatusBar?.updateWidget(WIDGET_ID)
            }
        }
        
        /**
         * 隐藏状态栏显示
         */
        fun hide(@NotNull project: Project) {
            val widget = findWidget(project)
            widget?.update {
                widget.myStatusBar?.updateWidget(WIDGET_ID)
            }
        }
    }
    
    /**
     * 创建实例
     */
    @NotNull
    override fun createInstance(@NotNull project: Project): StatusBarWidget {
        return StatusBarReadingWidget(project)
    }
    
    /**
     * 获取Widget状态
     */
    @NotNull
    override fun getWidgetState(@Nullable virtualFile: VirtualFile?): WidgetState {
        // 检查是否激活状态栏阅读模式
        if (!statusBarReadingService.isActive()) {
            return WidgetState.HIDDEN
        }
        
        // 获取当前显示内容
        val currentText = statusBarReadingService.getCurrentLineText()
        val tooltipText = statusBarReadingService.getTooltipText()
        
        // 创建Widget状态
        val widgetState = WidgetState(tooltipText, currentText, true)
        
        return widgetState
    }
    
    /**
     * 创建弹出菜单
     */
    @Nullable
    override fun createPopup(@NotNull dataContext: DataContext): ListPopup? {
        
        // 创建ActionGroup，包含状态栏相关的操作
        val group = object : ActionGroup() {
            override fun getChildren(e: com.intellij.openapi.actionSystem.AnActionEvent?): Array<com.intellij.openapi.actionSystem.AnAction> {
                return arrayOf(
                    object : com.intellij.openapi.actionSystem.AnAction("上一行") {
                        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                            statusBarReadingService.scrollToPreviousLine()
                        }
                    },
                    object : com.intellij.openapi.actionSystem.AnAction("下一行") {
                        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                            statusBarReadingService.scrollToNextLine()
                        }
                    },
                    object : com.intellij.openapi.actionSystem.AnAction("上一章") {
                        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                            statusBarReadingService.callback?.onPreviousChapter()
                        }
                    },
                    object : com.intellij.openapi.actionSystem.AnAction("下一章") {
                        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                            statusBarReadingService.callback?.onNextChapter()
                        }
                    }
                )
            }
        }
        
        return JBPopupFactory
            .getInstance()
            .createActionGroupPopup(
                "HelloRead 状态栏阅读",
                group,
                dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
            )
    }
    
    /**
     * 获取Widget ID
     */
    @NotNull
    override fun ID(): String = WIDGET_ID
}
