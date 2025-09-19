package com.github.xucux.read.model

import com.intellij.util.ui.UIUtil
import java.awt.Color

/**
 * 界面显示设置数据模型
 */
data class DisplaySettings(
    val hideOperationPanel: Boolean = false,    // 隐藏阅读界面的操作面板
    val hideTitleButton: Boolean = false,       // 隐藏阅读界面的标题按钮
    val hideProgressLabel: Boolean = false,     // 隐藏阅读界面的进度标签
    val autoSaveProgress: Boolean = true,       // 自动保存阅读进度
    val statusBarAutoScroll: Boolean = false,    // 底部状态栏自动滚动
    val statusBarScrollInterval: Int = 3000,    // 底部状态栏滚动间隔（毫秒）
    val backgroundColor: String = getDefaultBackgroundColor()  // 阅读器背景颜色（十六进制字符串，根据IDEA主题自动选择）
) {
    /**
     * 验证显示设置是否有效
     */
    fun isValid(): Boolean {
        return backgroundColor.isNotEmpty() && isValidColor(backgroundColor)
    }
    
    /**
     * 验证颜色字符串是否有效
     */
    private fun isValidColor(colorHex: String): Boolean {
        return try {
            Color.decode(colorHex)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        /**
         * 深色主题背景色
         */
        const val DARK_THEME_BACKGROUND = "#2B2B2B"
        
        /**
         * 浅色主题背景色
         */
        const val LIGHT_THEME_BACKGROUND = "#FFFFFF"
        
        /**
         * 根据IDEA主题获取默认背景色
         */
        public fun getDefaultBackgroundColor(): String {
            return try {
                // 检查是否为深色主题
                if (UIUtil.isUnderDarcula()) {
                    DARK_THEME_BACKGROUND
                } else {
                    LIGHT_THEME_BACKGROUND
                }
            } catch (e: Exception) {
                // 如果无法检测主题，默认使用深色主题
                DARK_THEME_BACKGROUND
            }
        }
        
        /**
         * 默认显示设置
         */
        val DEFAULT = DisplaySettings()
    }
}
