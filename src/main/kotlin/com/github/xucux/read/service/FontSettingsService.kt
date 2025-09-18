package com.github.xucux.read.service

import com.github.xucux.read.constants.StorageConstants
import com.github.xucux.read.model.FontSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.io.*
import java.util.*

/**
 * 字体设置服务
 * 使用Properties文件存储字体设置
 */
@Service
class FontSettingsService {
    private val logger = thisLogger()
    
    companion object {
        @JvmStatic fun getInstance(): FontSettingsService {
            return ApplicationManager.getApplication().getService(FontSettingsService::class.java)
        }
    }
    
    private val dataDir: File by lazy {
        val dir = File(System.getProperty("user.home"), StorageConstants.READBOOK_DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    private val fontSettingsFile: File by lazy {
        File(dataDir, StorageConstants.FONT_SETTINGS_FILE)
    }
    
    /**
     * 保存字体设置
     */
    fun saveFontSettings(settings: FontSettings) {
        try {
            val properties = loadProperties(fontSettingsFile)
            
            properties.setProperty("${StorageConstants.FONT_PREFIX}family", settings.fontFamily)
            properties.setProperty("${StorageConstants.FONT_PREFIX}size", settings.fontSize.toString())
            properties.setProperty("${StorageConstants.FONT_PREFIX}lineSpacing", settings.lineSpacing.toString())
            properties.setProperty("${StorageConstants.FONT_PREFIX}paragraphSpacing", settings.paragraphSpacing.toString())
            
            saveProperties(properties, fontSettingsFile)
            logger.info("保存字体设置成功: ${settings.getDisplayName()}")
        } catch (e: Exception) {
            logger.error("保存字体设置失败", e)
        }
    }
    
    /**
     * 加载字体设置
     */
    fun loadFontSettings(): FontSettings {
        try {
            val properties = loadProperties(fontSettingsFile)
            
            val fontFamily = properties.getProperty("${StorageConstants.FONT_PREFIX}family") ?: FontSettings.DEFAULT.fontFamily
            val fontSize = properties.getProperty("${StorageConstants.FONT_PREFIX}size")?.toIntOrNull() ?: FontSettings.DEFAULT.fontSize
            val lineSpacing = properties.getProperty("${StorageConstants.FONT_PREFIX}lineSpacing")?.toFloatOrNull() ?: FontSettings.DEFAULT.lineSpacing
            val paragraphSpacing = properties.getProperty("${StorageConstants.FONT_PREFIX}paragraphSpacing")?.toIntOrNull() ?: FontSettings.DEFAULT.paragraphSpacing
            
            val settings = FontSettings(fontFamily, fontSize, lineSpacing, paragraphSpacing)
            
            if (settings.isValid()) {
                logger.info("加载字体设置成功: ${settings.getDisplayName()}")
                return settings
            } else {
                logger.warn("加载的字体设置无效，使用默认设置")
                return FontSettings.DEFAULT
            }
        } catch (e: Exception) {
            logger.error("加载字体设置失败，使用默认设置", e)
            return FontSettings.DEFAULT
        }
    }
    
    /**
     * 重置为默认字体设置
     */
    fun resetToDefault() {
        try {
            saveFontSettings(FontSettings.DEFAULT)
            logger.info("重置字体设置为默认值")
        } catch (e: Exception) {
            logger.error("重置字体设置失败", e)
        }
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
            properties.store(output, "ReadBook Font Settings - ${Date()}")
        }
    }
}
