package com.github.xucux.read.factory

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.ui.BookReaderToolWindow
import com.github.xucux.read.ui.BookshelfToolWindow
import com.github.xucux.read.ui.ChapterListToolWindow
import com.github.xucux.read.ui.SettingsToolWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * HelloRead 主工具窗口工厂
 * 整合书架、阅读器、章节列表和设置功能
 */
class HelloReadToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 创建各个组件
        val bookshelfToolWindow = BookshelfToolWindow(project)
        val bookReaderToolWindow = BookReaderToolWindow(project)
        val chapterListWindow = ChapterListToolWindow(project)
        val settingWindows = SettingsToolWindow(project)
        
        // 创建内容
        val bookshelfContent = ContentFactory.getInstance().createContent(bookshelfToolWindow, TabConstants.BOOKSHELF_TAB, false)
        val bookReaderContent = ContentFactory.getInstance().createContent(bookReaderToolWindow, TabConstants.READER_TAB, false)
        val chapterListContent = ContentFactory.getInstance().createContent(chapterListWindow, TabConstants.CHAPTER_LIST_TAB, false)
        val settingsContent = ContentFactory.getInstance().createContent(settingWindows, TabConstants.SETTINGS_TAB, false)
        
        // 添加到工具窗口
        toolWindow.contentManager.addContent(bookshelfContent)
        toolWindow.contentManager.addContent(bookReaderContent)
        toolWindow.contentManager.addContent(chapterListContent)
        toolWindow.contentManager.addContent(settingsContent)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean = true
}
