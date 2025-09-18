package com.github.xucux.read.model

/**
 * 阅读模式枚举
 */
enum class ReadingMode {
    /**
     * 章节阅读模式
     * 每次页面仅显示一个章节的内容，通过点击上一章/下一章按钮切换章节
     */
    CHAPTER_MODE,
    
    /**
     * 滚动阅读模式
     * 当界面展示到文章内容末尾时，自动加载下一章节内容
     * 当从下往上浏览到顶部时，自动加载上一章的内容
     */
    SCROLL_MODE,
    
    /**
     * 底部状态栏阅读模式
     * 在IDE底部状态栏显示阅读内容，每次仅展示一行
     * 鼠标悬停时显示当前阅读章节和书名
     */
    STATUS_BAR_MODE
}
