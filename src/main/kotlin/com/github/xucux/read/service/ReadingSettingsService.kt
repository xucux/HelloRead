package com.github.xucux.read.service

import com.github.xucux.read.constants.StorageConstants
import com.github.xucux.read.model.ReadingMode
import com.github.xucux.read.model.ReadingSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.io.*
import java.util.*

/**
 * 阅读设置服务
 * 负责管理阅读模式、进度等设置
 */
@Service
class ReadingSettingsService {
    private val logger = thisLogger()
    
    companion object {
        @JvmStatic fun getInstance(): ReadingSettingsService {
            return ApplicationManager.getApplication().getService(ReadingSettingsService::class.java)
        }
    }
    
    private val dataDir: File by lazy {
        val dir = File(System.getProperty("user.home"), StorageConstants.READBOOK_DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    private val settingsFile: File by lazy {
        File(dataDir, StorageConstants.READING_SETTINGS_FILE)
    }
    
    /**
     * 保存阅读设置
     */
    fun saveReadingSettings(bookId: String, settings: ReadingSettings) {
        try {
            val properties = loadProperties(settingsFile)
            val prefix = "${StorageConstants.SETTINGS_PREFIX}$bookId."
            
            properties.setProperty("${prefix}readingMode", settings.readingMode.name)
            properties.setProperty("${prefix}currentLine", settings.currentLine.toString())
            properties.setProperty("${prefix}totalLines", settings.totalLines.toString())
            properties.setProperty("${prefix}currentChapterIndex", settings.currentChapterIndex.toString())
            properties.setProperty("${prefix}currentChapterTitle", settings.currentChapterTitle)
            properties.setProperty("${prefix}lastUpdateTime", settings.lastUpdateTime.toString())
            
            saveProperties(properties, settingsFile)
            logger.info("保存阅读设置成功: $bookId")
        } catch (e: Exception) {
            logger.error("保存阅读设置失败: $bookId", e)
        }
    }
    
    /**
     * 加载阅读设置
     */
    fun loadReadingSettings(bookId: String): ReadingSettings {
        try {
            val properties = loadProperties(settingsFile)
            val prefix = "${StorageConstants.SETTINGS_PREFIX}$bookId."
            
            val readingMode = try {
                ReadingMode.valueOf(properties.getProperty("${prefix}readingMode", ReadingMode.CHAPTER_MODE.name))
            } catch (e: Exception) {
                ReadingMode.CHAPTER_MODE
            }
            
            val currentLine = properties.getProperty("${prefix}currentLine")?.toIntOrNull() ?: 0
            val totalLines = properties.getProperty("${prefix}totalLines")?.toIntOrNull() ?: 0
            val currentChapterIndex = properties.getProperty("${prefix}currentChapterIndex")?.toIntOrNull() ?: 0
            val currentChapterTitle = properties.getProperty("${prefix}currentChapterTitle") ?: ""
            val lastUpdateTime = properties.getProperty("${prefix}lastUpdateTime")?.toLongOrNull() ?: System.currentTimeMillis()
            
            return ReadingSettings(
                readingMode = readingMode,
                currentLine = currentLine,
                totalLines = totalLines,
                currentChapterIndex = currentChapterIndex,
                currentChapterTitle = currentChapterTitle,
                lastUpdateTime = lastUpdateTime
            )
        } catch (e: Exception) {
            logger.error("加载阅读设置失败: $bookId", e)
            return ReadingSettings() // 返回默认设置
        }
    }
    
    /**
     * 删除阅读设置
     */
    fun removeReadingSettings(bookId: String) {
        try {
            val properties = loadProperties(settingsFile)
            val prefix = "${StorageConstants.SETTINGS_PREFIX}$bookId."
            val keysToRemove = properties.stringPropertyNames()
                .filter { it.startsWith(prefix) }
            
            for (key in keysToRemove) {
                properties.remove(key)
            }
            
            saveProperties(properties, settingsFile)
            logger.info("删除阅读设置成功: $bookId")
        } catch (e: Exception) {
            logger.error("删除阅读设置失败: $bookId", e)
        }
    }
    
    /**
     * 更新阅读模式
     */
    fun updateReadingMode(bookId: String, readingMode: ReadingMode) {
        val currentSettings = loadReadingSettings(bookId)
        val updatedSettings = currentSettings.copy(
            readingMode = readingMode,
            lastUpdateTime = System.currentTimeMillis()
        )
        saveReadingSettings(bookId, updatedSettings)
    }
    
    /**
     * 更新阅读进度
     */
    fun updateReadingProgress(bookId: String, currentLine: Int, totalLines: Int, chapterIndex: Int, chapterTitle: String) {
        val currentSettings = loadReadingSettings(bookId)
        val updatedSettings = currentSettings.copy(
            currentLine = currentLine,
            totalLines = totalLines,
            currentChapterIndex = chapterIndex,
            currentChapterTitle = chapterTitle,
            lastUpdateTime = System.currentTimeMillis()
        )
        saveReadingSettings(bookId, updatedSettings)
    }
    
    /**
     * 加载Properties文件
     */
    private fun loadProperties(file: File): Properties {
        val properties = Properties()
        if (file.exists()) {
            FileInputStream(file).use { input ->
                properties.load(input)
            }
        }
        return properties
    }
    
    /**
     * 保存Properties文件
     */
    private fun saveProperties(properties: Properties, file: File) {
        FileOutputStream(file).use { output ->
            properties.store(output, "ReadBook Reading Settings - ${Date()}")
        }
    }
}
