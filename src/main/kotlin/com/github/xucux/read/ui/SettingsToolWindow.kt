package com.github.xucux.read.ui

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.DisplaySettings
import com.github.xucux.read.model.FontSettings
import com.github.xucux.read.service.DisplaySettingsService
import com.github.xucux.read.service.FontSettingsService
import com.github.xucux.read.service.notify.ReaderNotificationService
import com.ibm.icu.text.CaseMap.Title
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.html.B
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.text.*

/**
 * 设置工具窗口
 * 包含字体设置、界面显示选项等
 */
class SettingsToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
    
    private val fontSettingsService = FontSettingsService.getInstance()
    private val displaySettingsService = DisplaySettingsService.getInstance()
    private val readerNotificationService = ReaderNotificationService(project)
    
    // 字体设置相关
    private lateinit var fontFamilyCombo: ComboBox<String>
    private lateinit var fontSizeCombo: ComboBox<String>
    private lateinit var lineSpacingField: JBTextField
    private lateinit var paragraphSpacingField: JBTextField
    private lateinit var previewArea: JTextPane
    private lateinit var applyFontButton: JButton
    
    // 界面显示选项
    private lateinit var hideOperationPanelCheckBox: JBCheckBox
    private lateinit var hideTitleButtonCheckBox: JBCheckBox
    private lateinit var hideProgressLabelCheckBox: JBCheckBox
    private lateinit var autoSaveProgressCheckBox: JBCheckBox
    private lateinit var statusBarAutoScrollCheckBox: JBCheckBox
    private lateinit var statusBarScrollIntervalField: JBTextField
    
    
    private var currentFontSettings: FontSettings = FontSettings.DEFAULT
    private var currentDisplaySettings: DisplaySettings = DisplaySettings.DEFAULT
    
    init {
        layout = BorderLayout()
        setupUI()
        // 先加载配置
        loadSettings()
        setupEventListeners()
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() {
        val mainPanel = JPanel()
        mainPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        
        // 字体设置区域
        val fontSettingsPanel = createFontSettingsPanel()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(fontSettingsPanel, gbc)
        
        // 界面显示选项区域
        val displayOptionsPanel = createDisplayOptionsPanel()
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(displayOptionsPanel, gbc)
        
        
        // 添加滚动面板
        val scrollPane = JBScrollPane(mainPanel)
        scrollPane.border = JBUI.Borders.empty()
        add(scrollPane, BorderLayout.WEST)
    }
    
    /**
     * 创建字体设置面板
     */
    private fun createFontSettingsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.border = TitledBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),"字体设置")
        
        // 创建设置控件面板
        val settingsPanel = JPanel()
        settingsPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        
        // 字体族设置
        val fontFamilyLabel = JLabel("字体族:")
        fontFamilyLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 10)
        settingsPanel.add(fontFamilyLabel, gbc)
        
        fontFamilyCombo = ComboBox<String>()
        fontFamilyCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_FONTS.toTypedArray())
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        settingsPanel.add(fontFamilyCombo, gbc)
        
        // 字体大小设置
        val fontSizeLabel = JLabel("字体大小:")
        fontSizeLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 10)
        settingsPanel.add(fontSizeLabel, gbc)
        
        fontSizeCombo = ComboBox<String>()
        fontSizeCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_SIZES.map { it.toString() }.toTypedArray())
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        settingsPanel.add(fontSizeCombo, gbc)
        
        // 行间距设置
        val lineSpacingLabel = JLabel("行间距:")
        lineSpacingLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 10)
        settingsPanel.add(lineSpacingLabel, gbc)
        
        lineSpacingField = JBTextField()
        lineSpacingField.toolTipText = "行间距倍数，如1.2表示1.2倍行距"
        gbc.gridx = 1
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        settingsPanel.add(lineSpacingField, gbc)
        
        // 段落间距设置
        val paragraphSpacingLabel = JLabel("段落间距:")
        paragraphSpacingLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 10)
        settingsPanel.add(paragraphSpacingLabel, gbc)
        
        paragraphSpacingField = JBTextField()
        paragraphSpacingField.toolTipText = "段落间距像素值，如10表示10像素"
        gbc.gridx = 1
        gbc.gridy = 3
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        settingsPanel.add(paragraphSpacingField, gbc)
        
        // 预览区域
        val previewLabel = JLabel("预览:")
        previewLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(10, 0, 5, 0)
        settingsPanel.add(previewLabel, gbc)
        
        previewArea = JTextPane()
        previewArea.isEditable = false
        previewArea.text = "这是字体预览文本。\n\n" +
                "在这里可以看到字体设置的效果。\n" +
                "包括字体族、字体大小、行间距和段落间距。\n\n" +
                "支持中文和英文混合显示。\n" +
                "The quick brown fox jumps over the lazy dog."
        previewArea.border = JBUI.Borders.empty(5)
        
        val scrollPane = JScrollPane(previewArea)
        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.insets = JBUI.insets(5, 0, 10, 0)
        settingsPanel.add(scrollPane, gbc)
        
        // 应用按钮
        applyFontButton = JButton("应用字体设置")
        applyFontButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 6
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.CENTER
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.insets(10, 0, 0, 0)
        settingsPanel.add(applyFontButton, gbc)
        
        // 将设置面板添加到主面板的北侧
        panel.add(settingsPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * 创建界面显示选项面板
     */
    private fun createDisplayOptionsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.border = TitledBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),"界面显示选项")
        
        // 创建选项控件面板
        val optionsPanel = JPanel()
        optionsPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        
        // 隐藏操作面板
        hideOperationPanelCheckBox = JBCheckBox("隐藏阅读界面的操作面板")
        hideOperationPanelCheckBox.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        optionsPanel.add(hideOperationPanelCheckBox, gbc)
        
        // 隐藏标题按钮
        hideTitleButtonCheckBox = JBCheckBox("隐藏阅读界面的标题按钮")
        hideTitleButtonCheckBox.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        optionsPanel.add(hideTitleButtonCheckBox, gbc)
        
        // 隐藏进度标签
        hideProgressLabelCheckBox = JBCheckBox("隐藏阅读界面的进度标签")
        hideProgressLabelCheckBox.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        optionsPanel.add(hideProgressLabelCheckBox, gbc)
        
        // 自动保存进度
        autoSaveProgressCheckBox = JBCheckBox("自动保存阅读进度")
        autoSaveProgressCheckBox.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        autoSaveProgressCheckBox.isSelected = true
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        optionsPanel.add(autoSaveProgressCheckBox, gbc)
        
        // 底部状态栏自动滚动
        statusBarAutoScrollCheckBox = JBCheckBox("底部状态栏自动滚动")
        statusBarAutoScrollCheckBox.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        optionsPanel.add(statusBarAutoScrollCheckBox, gbc)
        
        // 底部状态栏滚动间隔
        val statusBarIntervalLabel = JLabel("滚动间隔(毫秒):")
        statusBarIntervalLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 20, 5, 10)
        optionsPanel.add(statusBarIntervalLabel, gbc)
        
        statusBarScrollIntervalField = JBTextField()
        statusBarScrollIntervalField.toolTipText = "底部状态栏自动滚动的间隔时间，单位毫秒"
        gbc.gridx = 1
        gbc.gridy = 5
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        optionsPanel.add(statusBarScrollIntervalField, gbc)
        
        // 将选项面板添加到主面板的中心
        panel.add(optionsPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    
    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 字体设置变化监听 - 仅更新预览
        fontFamilyCombo.addActionListener {
            updateFontPreview()
        }
        
        fontSizeCombo.addActionListener {
            updateFontPreview()
        }
        
        lineSpacingField.addActionListener {
            updateFontPreview()
        }
        
        paragraphSpacingField.addActionListener {
            updateFontPreview()
        }
        
        // 应用字体设置按钮
        applyFontButton.addActionListener {
            saveFontSettings()
            readerNotificationService.notifyReaderUpdateFont()
        }
        
        // 界面显示选项变化
        hideOperationPanelCheckBox.addActionListener {
            saveDisplaySettings()
            readerNotificationService.notifyReaderUpdateDisplay(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected
            )
        }
        
        hideTitleButtonCheckBox.addActionListener {
            saveDisplaySettings()
            readerNotificationService.notifyReaderUpdateDisplay(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected
            )
        }
        
        hideProgressLabelCheckBox.addActionListener {
            saveDisplaySettings()
            readerNotificationService.notifyReaderUpdateDisplay(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected
            )
        }
        
        autoSaveProgressCheckBox.addActionListener {
            saveDisplaySettings()
        }
        
        statusBarAutoScrollCheckBox.addActionListener {
            saveDisplaySettings()
        }
        
        statusBarScrollIntervalField.addActionListener {
            saveDisplaySettings()
        }
        
    }
    
    /**
     * 加载设置
     */
    private fun loadSettings() {
        // 加载字体设置
        currentFontSettings = fontSettingsService.loadFontSettings()
        updateFontSettingsDisplay()
        updateFontPreview()
        
        // 加载显示设置（这里可以从配置文件或服务中加载）
        loadDisplaySettings()
    }
    
    /**
     * 重新加载设置（公开方法，供外部调用）
     */
    fun reloadSettings() {
        loadSettings()
    }
    
    /**
     * 更新字体设置显示
     */
    private fun updateFontSettingsDisplay() {
        fontFamilyCombo.selectedItem = currentFontSettings.fontFamily
        fontSizeCombo.selectedItem = currentFontSettings.fontSize.toString()
        lineSpacingField.text = currentFontSettings.lineSpacing.toString()
        paragraphSpacingField.text = currentFontSettings.paragraphSpacing.toString()
    }
    
    /**
     * 更新字体预览
     */
    private fun updateFontPreview() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = lineSpacingField.text.toFloatOrNull() ?: currentFontSettings.lineSpacing
            // 获取段落间距值但不使用变量名
            paragraphSpacingField.text.toIntOrNull() ?: currentFontSettings.paragraphSpacing
            
            // 应用字体到预览区域
            previewArea.font = Font(fontFamily, Font.PLAIN, fontSize)
            
            // 设置行间距
            val doc = previewArea.styledDocument
            val set = SimpleAttributeSet()
            StyleConstants.setLineSpacing(set, lineSpacing - 1.0f)
            doc.setParagraphAttributes(0, doc.length, set, false)
            
        } catch (e: Exception) {
            // 如果设置无效，使用当前设置
            previewArea.font = Font(currentFontSettings.fontFamily, Font.PLAIN, currentFontSettings.fontSize)
        }
    }
    
    /**
     * 保存字体设置
     */
    private fun saveFontSettings() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = lineSpacingField.text.toFloatOrNull() ?: currentFontSettings.lineSpacing
            val paragraphSpacing = paragraphSpacingField.text.toIntOrNull() ?: currentFontSettings.paragraphSpacing

            currentFontSettings = FontSettings(fontFamily, fontSize, lineSpacing, paragraphSpacing)
            fontSettingsService.saveFontSettings(currentFontSettings)

        } catch (e: Exception) {
            // 忽略错误，保持当前设置
        }
    }

    /**
     * 加载显示设置
     */
    private fun loadDisplaySettings() {
        // 从存储中加载显示设置
        currentDisplaySettings = displaySettingsService.loadDisplaySettings()
        
        // 更新UI显示
        hideOperationPanelCheckBox.isSelected = currentDisplaySettings.hideOperationPanel
        hideTitleButtonCheckBox.isSelected = currentDisplaySettings.hideTitleButton
        hideProgressLabelCheckBox.isSelected = currentDisplaySettings.hideProgressLabel
        autoSaveProgressCheckBox.isSelected = currentDisplaySettings.autoSaveProgress
        statusBarAutoScrollCheckBox.isSelected = currentDisplaySettings.statusBarAutoScroll
        statusBarScrollIntervalField.text = currentDisplaySettings.statusBarScrollInterval.toString()
    }

    /**
     * 保存显示设置
     */
    private fun saveDisplaySettings() {
        try {
            // 创建新的显示设置对象
            val statusBarInterval = statusBarScrollIntervalField.text.toIntOrNull() ?: 3000
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                statusBarAutoScrollCheckBox.isSelected,
                statusBarInterval
            )
            
            // 保存到存储
            displaySettingsService.saveDisplaySettings(newDisplaySettings)
            currentDisplaySettings = newDisplaySettings
            
        } catch (e: Exception) {
            // 忽略错误，保持当前设置
        }
    }




}