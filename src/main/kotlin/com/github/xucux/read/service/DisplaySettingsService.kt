package com.github.xucux.read.service

import com.github.xucux.read.constants.StorageConstants
import com.github.xucux.read.model.DisplaySettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.io.*
import java.util.*

/**
 * 界面显示设置服务
 * 使用Properties文件存储界面显示设置
 */
@Service
class DisplaySettingsService {
    private val logger = thisLogger()
    
    companion object {
        @JvmStatic fun getInstance(): DisplaySettingsService {
            return ApplicationManager.getApplication().getService(DisplaySettingsService::class.java)
        }
    }
    
    private val dataDir: File by lazy {
        val dir = File(System.getProperty("user.home"), StorageConstants.READBOOK_DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    private val displaySettingsFile: File by lazy {
        File(dataDir, StorageConstants.DISPLAY_SETTINGS_FILE)
    }
    
    /**
     * 保存界面显示设置
     */
    fun saveDisplaySettings(settings: DisplaySettings) {
        try {
            val properties = loadProperties(displaySettingsFile)
            
            properties.setProperty("${StorageConstants.DISPLAY_PREFIX}hideOperationPanel", settings.hideOperationPanel.toString())
            properties.setProperty("${StorageConstants.DISPLAY_PREFIX}hideTitleButton", settings.hideTitleButton.toString())
            properties.setProperty("${StorageConstants.DISPLAY_PREFIX}hideProgressLabel", settings.hideProgressLabel.toString())
            properties.setProperty("${StorageConstants.DISPLAY_PREFIX}autoSaveProgress", settings.autoSaveProgress.toString())
            
            saveProperties(properties, displaySettingsFile)
            logger.info("保存界面显示设置成功")
        } catch (e: Exception) {
            logger.error("保存界面显示设置失败", e)
        }
    }
    
    /**
     * 加载界面显示设置
     */
    fun loadDisplaySettings(): DisplaySettings {
        try {
            val properties = loadProperties(displaySettingsFile)
            
            val hideOperationPanel = properties.getProperty("${StorageConstants.DISPLAY_PREFIX}hideOperationPanel")?.toBoolean() ?: DisplaySettings.DEFAULT.hideOperationPanel
            val hideTitleButton = properties.getProperty("${StorageConstants.DISPLAY_PREFIX}hideTitleButton")?.toBoolean() ?: DisplaySettings.DEFAULT.hideTitleButton
            val hideProgressLabel = properties.getProperty("${StorageConstants.DISPLAY_PREFIX}hideProgressLabel")?.toBoolean() ?: DisplaySettings.DEFAULT.hideProgressLabel
            val autoSaveProgress = properties.getProperty("${StorageConstants.DISPLAY_PREFIX}autoSaveProgress")?.toBoolean() ?: DisplaySettings.DEFAULT.autoSaveProgress
            
            val settings = DisplaySettings(hideOperationPanel, hideTitleButton, hideProgressLabel, autoSaveProgress)
            
            if (settings.isValid()) {
                logger.info("加载界面显示设置成功")
                return settings
            } else {
                logger.warn("加载的界面显示设置无效，使用默认设置")
                return DisplaySettings.DEFAULT
            }
        } catch (e: Exception) {
            logger.error("加载界面显示设置失败，使用默认设置", e)
            return DisplaySettings.DEFAULT
        }
    }
    
    /**
     * 重置为默认界面显示设置
     */
    fun resetToDefault() {
        try {
            saveDisplaySettings(DisplaySettings.DEFAULT)
            logger.info("重置界面显示设置为默认值")
        } catch (e: Exception) {
            logger.error("重置界面显示设置失败", e)
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
            properties.store(output, "ReadBook Display Settings - ${Date()}")
        }
    }
}
