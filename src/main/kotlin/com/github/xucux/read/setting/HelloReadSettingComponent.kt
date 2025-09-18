package com.github.xucux.read.setting

import com.github.xucux.read.model.DisplaySettings
import com.github.xucux.read.model.FontSettings
import com.github.xucux.read.service.DisplaySettingsService
import com.github.xucux.read.service.FontSettingsService
import com.github.xucux.read.service.notify.ReaderNotificationService
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.text.*

/**
 * HelloRead设置组件
 * 包含字体设置等配置选项
 */
class HelloReadSettingComponent : SearchableConfigurable {
    
    private val fontSettingsService = FontSettingsService.getInstance()
    private val displaySettingsService = DisplaySettingsService.getInstance()
    
    // 获取当前项目实例
    private val currentProject: Project?
        get() = ProjectManager.getInstance().openProjects.firstOrNull()
    
    // 字体设置UI组件
    private val fontFamilyCombo = ComboBox<String>()
    private val fontSizeCombo = ComboBox<String>()
    private val lineSpacingField = JBTextField()
    private val paragraphSpacingField = JBTextField()
    private val previewArea = JTextPane()
    
    // 界面显示选项UI组件
    private val hideOperationPanelCheckBox = JBCheckBox("隐藏阅读界面的操作面板")
    private val hideTitleButtonCheckBox = JBCheckBox("隐藏阅读界面的标题按钮")
    private val hideProgressLabelCheckBox = JBCheckBox("隐藏阅读界面的进度标签")
    private val autoSaveProgressCheckBox = JBCheckBox("自动保存阅读进度")
    private val statusBarAutoScrollCheckBox = JBCheckBox("底部状态栏自动滚动")
    private val statusBarScrollIntervalField = JBTextField()
    
    // 当前设置
    private var currentFontSettings: FontSettings = fontSettingsService.loadFontSettings()
    private var originalFontSettings: FontSettings = currentFontSettings
    private var currentDisplaySettings: DisplaySettings = displaySettingsService.loadDisplaySettings()
    private var originalDisplaySettings: DisplaySettings = currentDisplaySettings
    
    override fun createComponent(): JComponent? {
        val mainPanel = JPanel()
        mainPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        
        // 字体设置面板
        val fontSettingsPanel = createFontSettingsPanel()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTH
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(fontSettingsPanel, gbc)
        
        // 界面显示选项面板
        val displayOptionsPanel = createDisplayOptionsPanel()
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTH
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(displayOptionsPanel, gbc)
        
        // 预览面板
        val previewPanel = createPreviewPanel()
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(previewPanel, gbc)
        
        // 添加滚动面板
        val scrollPane = JScrollPane(mainPanel)
        scrollPane.border = JBUI.Borders.empty()
        
        // 初始化设置
        loadCurrentSettings()
        
        return scrollPane
    }
    
    /**
     * 创建字体设置面板
     */
    private fun createFontSettingsPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BorderLayout()
        
        // 设置带标题的边框
        panel.border = TitledBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()), "字体设置")
        
        // 创建字体设置容器面板，使用垂直布局
        val settingsPanel = JPanel()
        settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
        
        // 字体族设置
        val fontFamilyPanel = JPanel(BorderLayout())
        val fontFamilyLabel = JBLabel("字体族:")
//        fontFamilyLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        fontFamilyPanel.add(fontFamilyLabel, BorderLayout.WEST)
        fontFamilyPanel.add(fontFamilyCombo, BorderLayout.CENTER)
        settingsPanel.add(fontFamilyPanel)
        settingsPanel.add(Box.createVerticalStrut(5)) // 添加间距
        
        // 字体大小设置
        val fontSizePanel = JPanel(BorderLayout())
        val fontSizeLabel = JBLabel("字体大小:")
//        fontSizeLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        fontSizePanel.add(fontSizeLabel, BorderLayout.WEST)
        fontSizePanel.add(fontSizeCombo, BorderLayout.CENTER)
        settingsPanel.add(fontSizePanel)
        settingsPanel.add(Box.createVerticalStrut(5)) // 添加间距
        
        // 行间距设置
        val lineSpacingPanel = JPanel(BorderLayout())
        val lineSpacingLabel = JBLabel("行间距:")
//        lineSpacingLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        lineSpacingPanel.add(lineSpacingLabel, BorderLayout.WEST)
        lineSpacingPanel.add(lineSpacingField, BorderLayout.CENTER)
        settingsPanel.add(lineSpacingPanel)
        settingsPanel.add(Box.createVerticalStrut(5)) // 添加间距
        
        // 段落间距设置
        val paragraphSpacingPanel = JPanel(BorderLayout())
        val paragraphSpacingLabel = JBLabel("段落间距:")
//        paragraphSpacingLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        paragraphSpacingPanel.add(paragraphSpacingLabel, BorderLayout.WEST)
        paragraphSpacingPanel.add(paragraphSpacingField, BorderLayout.CENTER)
        settingsPanel.add(paragraphSpacingPanel)
        
        // 将设置面板添加到主面板的中央
        panel.add(settingsPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * 创建界面显示选项面板
     */
    private fun createDisplayOptionsPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BorderLayout()
        
        // 设置带标题的边框
        panel.border = TitledBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()), "界面显示选项")
        
        // 设置复选框字体
//        val checkboxFont = Font( Font.PLAIN, 12)
//        hideOperationPanelCheckBox.font = checkboxFont
//        hideTitleButtonCheckBox.font = checkboxFont
//        hideProgressLabelCheckBox.font = checkboxFont
//        autoSaveProgressCheckBox.font = checkboxFont
        
        // 创建复选框容器面板，使用垂直布局
        val checkboxPanel = JPanel()
        checkboxPanel.layout = BoxLayout(checkboxPanel, BoxLayout.Y_AXIS)
        
        // 添加复选框到容器面板
        checkboxPanel.add(hideOperationPanelCheckBox)
        checkboxPanel.add(Box.createVerticalStrut(5)) // 添加间距
        checkboxPanel.add(hideTitleButtonCheckBox)
        checkboxPanel.add(Box.createVerticalStrut(5)) // 添加间距
        checkboxPanel.add(hideProgressLabelCheckBox)
        checkboxPanel.add(Box.createVerticalStrut(5)) // 添加间距
        checkboxPanel.add(autoSaveProgressCheckBox)
        checkboxPanel.add(Box.createVerticalStrut(5)) // 添加间距
        checkboxPanel.add(statusBarAutoScrollCheckBox)
        
        // 添加滚动间隔设置
        val intervalPanel = JPanel(BorderLayout())
        val intervalLabel = JBLabel("滚动间隔(毫秒):")
        intervalLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        intervalPanel.add(intervalLabel, BorderLayout.WEST)
        statusBarScrollIntervalField.toolTipText = "底部状态栏自动滚动的间隔时间，单位毫秒"
        intervalPanel.add(statusBarScrollIntervalField, BorderLayout.CENTER)
        checkboxPanel.add(intervalPanel)
        
        // 将复选框面板添加到主面板的中央
        panel.add(checkboxPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * 创建预览面板
     */
    private fun createPreviewPanel(): JComponent {
        val panel = JPanel()
        panel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        
        // 设置带标题的边框
        panel.border = TitledBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()), "预览")
        
        // 预览标签
        val previewLabel = JBLabel("字体效果预览:")
//        previewLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        panel.add(previewLabel, gbc)
        
        // 预览区域
        val scrollPane = JScrollPane(previewArea)
        scrollPane.border = JBUI.Borders.empty(5)
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.insets = JBUI.insets(5, 0, 0, 0)
        panel.add(scrollPane, gbc)
        
        return panel
    }
    
    /**
     * 加载当前设置
     */
    private fun loadCurrentSettings() {
        // 设置字体族下拉框
        fontFamilyCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_FONTS.toTypedArray())
        
        // 设置字体大小下拉框
        fontSizeCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_SIZES.map { it.toString() }.toTypedArray())
        
        // 设置预览区域
        previewArea.isEditable = false
        previewArea.text = "这是字体预览文本。\n\n" +
                "在这里可以看到字体设置的效果。\n" +
                "包括字体族、字体大小、行间距和段落间距。\n\n" +
                "支持中文和英文混合显示。\n" +
                "The quick brown fox jumps over the lazy dog."
        previewArea.border = JBUI.Borders.empty(10)
        
        // 加载当前字体设置值
        fontFamilyCombo.selectedItem = currentFontSettings.fontFamily
        fontSizeCombo.selectedItem = currentFontSettings.fontSize.toString()
        lineSpacingField.text = currentFontSettings.lineSpacing.toString()
        paragraphSpacingField.text = currentFontSettings.paragraphSpacing.toString()
        
        // 加载当前显示设置值
        hideOperationPanelCheckBox.isSelected = currentDisplaySettings.hideOperationPanel
        hideTitleButtonCheckBox.isSelected = currentDisplaySettings.hideTitleButton
        hideProgressLabelCheckBox.isSelected = currentDisplaySettings.hideProgressLabel
        autoSaveProgressCheckBox.isSelected = currentDisplaySettings.autoSaveProgress
        statusBarAutoScrollCheckBox.isSelected = currentDisplaySettings.statusBarAutoScroll
        statusBarScrollIntervalField.text = currentDisplaySettings.statusBarScrollInterval.toString()
        
        // 添加字体设置事件监听器
        fontFamilyCombo.addActionListener { updatePreview() }
        fontSizeCombo.addActionListener { updatePreview() }
        lineSpacingField.addActionListener { updatePreview() }
        paragraphSpacingField.addActionListener { updatePreview() }
        
        // 添加显示设置事件监听器
        hideOperationPanelCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        hideTitleButtonCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        hideProgressLabelCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        autoSaveProgressCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        
        // 初始预览
        updatePreview()
    }
    
    /**
     * 更新预览
     */
    private fun updatePreview() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = lineSpacingField.text.toFloatOrNull() ?: currentFontSettings.lineSpacing
            val paragraphSpacing = paragraphSpacingField.text.toIntOrNull() ?: currentFontSettings.paragraphSpacing
            
            // 应用字体设置到预览区域
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

    override fun isModified(): Boolean {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = lineSpacingField.text.toFloatOrNull() ?: currentFontSettings.lineSpacing
            val paragraphSpacing = paragraphSpacingField.text.toIntOrNull() ?: currentFontSettings.paragraphSpacing
            
            val newFontSettings = FontSettings(fontFamily, fontSize, lineSpacing, paragraphSpacing)
            val statusBarInterval = statusBarScrollIntervalField.text.toIntOrNull() ?: 3000
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                statusBarAutoScrollCheckBox.isSelected,
                statusBarInterval
            )
            
            return newFontSettings != originalFontSettings || newDisplaySettings != originalDisplaySettings
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 应用设置
     */
    @Throws(ConfigurationException::class)
    override fun apply() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = lineSpacingField.text.toFloatOrNull() ?: currentFontSettings.lineSpacing
            val paragraphSpacing = paragraphSpacingField.text.toIntOrNull() ?: currentFontSettings.paragraphSpacing
            
            val newFontSettings = FontSettings(fontFamily, fontSize, lineSpacing, paragraphSpacing)
            val statusBarInterval = statusBarScrollIntervalField.text.toIntOrNull() ?: 3000
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                statusBarAutoScrollCheckBox.isSelected,
                statusBarInterval
            )
            
            if (newFontSettings.isValid() && newDisplaySettings.isValid()) {
                // 保存字体设置
                fontSettingsService.saveFontSettings(newFontSettings)
                currentFontSettings = newFontSettings
                originalFontSettings = newFontSettings
                
                // 保存显示设置
                displaySettingsService.saveDisplaySettings(newDisplaySettings)
                currentDisplaySettings = newDisplaySettings
                originalDisplaySettings = newDisplaySettings
                
                // 通知阅读器更新设置
                currentProject?.let { project ->
                    val readerNotificationService = ReaderNotificationService(project)
                    readerNotificationService.notifyReaderUpdateFont()
                    readerNotificationService.notifyReaderUpdateDisplay(
                        newDisplaySettings.hideOperationPanel,
                        newDisplaySettings.hideTitleButton,
                        newDisplaySettings.hideProgressLabel
                    )
                }
            } else {
                throw ConfigurationException("设置无效，请检查输入值")
            }
        } catch (e: Exception) {
            throw ConfigurationException("保存设置失败: ${e.message}")
        }
    }

    override fun getDisplayName(): String {
        return "HelloRead"
    }

    override fun getId(): String {
        return "com.github.xucux.read.setting.HelloReadSettingComponent"
    }
}
