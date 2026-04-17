package com.github.xucux.read.model

import com.intellij.ui.JBColor
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
    val backgroundColor: String = getDefaultBackgroundColor(),  // 阅读器背景颜色（十六进制字符串，根据IDEA主题自动选择）
    val fontColor: String = getDefaultFontColor(),  // 阅读器字体颜色（十六进制字符串，根据IDEA主题自动选择）
    val autoContrastFontColor: Boolean = false  // 开启后根据背景色自动推荐字体色
) {
    /**
     * 验证显示设置是否有效
     */
    fun isValid(): Boolean {
        return backgroundColor.isNotEmpty() &&
            fontColor.isNotEmpty() &&
            isValidColor(backgroundColor) &&
            isValidColor(fontColor)
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
        const val DARK_THEME_FONT = "#BBBBBB"
        const val LIGHT_THEME_FONT = "#1F1F1F"
        
        /**
         * 根据IDEA主题获取默认背景色
         */
        public fun getDefaultBackgroundColor(): String {
            return try {
                if (JBColor.isBright()) {
                    LIGHT_THEME_BACKGROUND
                } else {
                    DARK_THEME_BACKGROUND
                }
            } catch (e: Exception) {
                // 如果无法检测主题，默认使用深色主题
                DARK_THEME_BACKGROUND
            }
        }

        /**
         * 根据IDEA主题获取默认字体色
         */
        fun getDefaultFontColor(): String {
            return try {
                if (JBColor.isBright()) {
                    LIGHT_THEME_FONT
                } else {
                    DARK_THEME_FONT
                }
            } catch (e: Exception) {
                LIGHT_THEME_FONT
            }
        }

        /**
         * 根据背景色推荐高对比字体色
         */
        fun getRecommendedFontColor(backgroundHex: String): String {
            return try {
                val color = Color.decode(backgroundHex)
                val brightness = (color.red * 299 + color.green * 587 + color.blue * 114) / 1000
                if (brightness < 128) DARK_THEME_FONT else LIGHT_THEME_FONT
            } catch (e: Exception) {
                getDefaultFontColor()
            }
        }
        
        /**
         * 默认显示设置
         */
        val DEFAULT = DisplaySettings()
    }
}
