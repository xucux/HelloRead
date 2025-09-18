package com.github.xucux.read.constants

/**
 * 存储相关常量
 * 统一管理所有服务类的文件路径和配置常量
 */
object StorageConstants {
    
    // 数据目录
    const val DATA_DIR = "hallo_read_data"
    const val READBOOK_DATA_DIR = "hallo_read_data"
    
    // 文件名称
    const val BOOKSHELF_FILE = "hallo_read_bookshelf.properties"
    const val READING_RECORDS_FILE = "hallo_read_records.properties"
    const val CHAPTER_CACHE_FILE = "hallo_read_chapters.properties"
    const val COLUMN_WIDTHS_FILE = "hallo_read_column_widths.properties"
    const val READING_SETTINGS_FILE = "hallo_read_reading_settings.properties"
    const val FONT_SETTINGS_FILE = "hallo_read_font_settings.properties"
    const val DISPLAY_SETTINGS_FILE = "hallo_read_display_settings.properties"
    
    // 缓存过期时间（120天）
    const val CACHE_EXPIRE_TIME = 120 * 24 * 60 * 60 * 1000L
    
    // 属性键前缀
    const val BOOK_PREFIX = "book."
    const val RECORD_PREFIX = "record."
    const val CHAPTERS_PREFIX = "chapters."
    const val SETTINGS_PREFIX = "settings."
    const val COLUMN_PREFIX = "column."
    const val FONT_PREFIX = "font."
    const val DISPLAY_PREFIX = "display."
    
    // 章节缓存相关
    const val CHAPTER_COUNT_KEY = "count"
    const val CHAPTER_CACHED_TIME_KEY = "cachedTime"
    const val CHAPTER_PREFIX = "chapter."
}
