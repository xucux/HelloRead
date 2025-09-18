package com.github.xucux.read.model

/**
 * 字体设置数据模型
 */
data class FontSettings(
    val fontFamily: String = "宋体",           // 字体类型
    val fontSize: Int = 14,                   // 字体大小
    val lineSpacing: Float = 1.2f,           // 行间距
    val paragraphSpacing: Int = 10            // 段落间距
) {
    /**
     * 获取字体显示名称
     */
    fun getDisplayName(): String {
        return "$fontFamily ${fontSize}px"
    }
    
    /**
     * 验证字体设置是否有效
     */
    fun isValid(): Boolean {
        return fontSize > 0 && fontSize <= 72 && 
               lineSpacing > 0.5f && lineSpacing <= 3.0f &&
               paragraphSpacing >= 0 && paragraphSpacing <= 50
    }
    
    companion object {
        /**
         * 默认字体设置
         */
        val DEFAULT = FontSettings()
        
        /**
         * 可用的字体族列表
         */
        val AVAILABLE_FONTS = listOf(
            "宋体", "黑体", "楷体", "仿宋", "微软雅黑", 
            "Arial", "Times New Roman", "Courier New",
            "SimSun", "SimHei", "KaiTi", "FangSong"
        )
        
        /**
         * 可用的字体大小列表
         */
        val AVAILABLE_SIZES = listOf(10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24, 26, 28, 30)
    }
}
