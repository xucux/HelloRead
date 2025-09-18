package com.github.xucux.read.model

/**
 * 界面显示设置数据模型
 */
data class DisplaySettings(
    val hideOperationPanel: Boolean = false,    // 隐藏阅读界面的操作面板
    val hideTitleButton: Boolean = false,       // 隐藏阅读界面的标题按钮
    val hideProgressLabel: Boolean = false,     // 隐藏阅读界面的进度标签
    val autoSaveProgress: Boolean = true,       // 自动保存阅读进度
    val statusBarAutoScroll: Boolean = false,    // 底部状态栏自动滚动
    val statusBarScrollInterval: Int = 3000     // 底部状态栏滚动间隔（毫秒）
) {
    /**
     * 验证显示设置是否有效
     */
    fun isValid(): Boolean {
        return true // 所有布尔值都是有效的
    }
    
    companion object {
        /**
         * 默认显示设置
         */
        val DEFAULT = DisplaySettings()
    }
}
