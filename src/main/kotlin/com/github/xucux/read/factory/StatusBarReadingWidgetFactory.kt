package com.github.xucux.read.factory

import com.github.xucux.read.service.StatusBarReadingService
import com.github.xucux.read.widget.StatusBarReadingWidget
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.openapi.wm.WindowManager
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

/**
 * 状态栏阅读Widget工厂
 * 参考w-reader项目的实现方式
 */
class StatusBarReadingWidgetFactory : StatusBarWidgetFactory {
    
    companion object {
        const val WIDGET_ID = "HelloRead.StatusBarReading"
        const val DISPLAY_NAME = "HelloRead Status Bar Reading"
    }
    
    override fun getId(): @NotNull @NonNls String = WIDGET_ID
    
    override fun getDisplayName(): @NotNull @NlsContexts.ConfigurableName String = DISPLAY_NAME
    
    /**
     * 创建状态栏Widget
     */
    override fun createWidget(@NotNull project: Project): @NotNull StatusBarWidget {
        return StatusBarReadingWidget(project)
    }
    
    /**
     * 销毁Widget
     */
    override fun disposeWidget(@NotNull widget: StatusBarWidget) {
        widget.dispose()
    }
    
    /**
     * 检查Widget是否可用
     * 只有在状态栏阅读模式激活时才可用
     */
    override fun isAvailable(@NotNull project: Project): Boolean {
        return StatusBarReadingService.getInstance().isActive()
    }
    
    /**
     * 检查Widget是否可以在指定状态栏上启用
     */
    override fun canBeEnabledOn(@NotNull statusBar: StatusBar): Boolean {
        return true
    }
    
    /**
     * 启用状态栏Widget
     * @param project 项目
     * @param isStartupApp 是否是启动项目
     */
    fun setEnabled(@NotNull project: Project, isStartupApp: Boolean) {
        if (!isStartupApp) {
            // 检查是否已存在状态栏Widget
            val windowManager = WindowManager.getInstance()
            val statusBar = windowManager.getStatusBar(project)
            var isExistStatusBarWidget = false
            
            if (statusBar != null) {
                val existingWidget = statusBar.getWidget(WIDGET_ID)
                if (existingWidget != null) {
                    isExistStatusBarWidget = true
                }
            }
            
            // 如果状态栏Widget不存在，则创建
            if (!isExistStatusBarWidget) {
                val statusBarWidgetsManager = project.getService(StatusBarWidgetsManager::class.java)
                statusBarWidgetsManager.updateWidget(this)
            }
        }
    }
}
