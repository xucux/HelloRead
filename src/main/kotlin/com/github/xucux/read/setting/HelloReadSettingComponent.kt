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
import com.intellij.ui.ColorChooser
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.Color
import java.awt.BorderLayout
import java.awt.FlowLayout
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
    private val editPreviewCheckBox = JBCheckBox("启用预览编辑")
    
    // 界面显示选项UI组件
    private val hideOperationPanelCheckBox = JBCheckBox("隐藏阅读界面的操作面板")
    private val hideTitleButtonCheckBox = JBCheckBox("隐藏阅读界面的标题按钮")
    private val hideProgressLabelCheckBox = JBCheckBox("隐藏阅读界面的进度标签")
    private val autoSaveProgressCheckBox = JBCheckBox("自动保存阅读进度")
    private val statusBarAutoScrollCheckBox = JBCheckBox("底部状态栏自动滚动")
    private val statusBarScrollIntervalField = JBTextField()
    
    // 背景颜色设置UI组件
    private val backgroundColorButton = JButton("选择颜色")
    private val backgroundColorPreview = JLabel()
    private val presetColorButton1 = JButton("深色")
    private val presetColorButton2 = JButton("浅色")
    
    // 当前设置
    private var currentFontSettings: FontSettings = fontSettingsService.loadFontSettings()
    private var originalFontSettings: FontSettings = currentFontSettings
    private var currentDisplaySettings: DisplaySettings = displaySettingsService.loadDisplaySettings()
    private var originalDisplaySettings: DisplaySettings = currentDisplaySettings
    
    private val previewText: String = "这是字体预览文本。\n\n" +
                "在这里可以看到字体设置的效果。\n" +
                "包括字体族、字体大小、行间距和段落间距。\n\n" +
                "支持中文和英文混合显示。\n" +
                "The quick brown fox jumps over the lazy dog."

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
//        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(fontSettingsPanel, gbc)

        // 预览面板
        val previewPanel = createPreviewPanel()
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.NORTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
//        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(previewPanel, gbc)
        
        // 界面显示选项面板
        val displayOptionsPanel = createDisplayOptionsPanel()
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTH
        gbc.weightx = 1.0
//        gbc.insets = JBUI.insets(10, 10, 10, 10)
        mainPanel.add(displayOptionsPanel, gbc)
        

        
        // 添加滚动面板
        val scrollPane = JBScrollPane(mainPanel)
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
        panel.border = TitledBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),
            JBUI.Borders.empty(10)
        ), "字体设置")
        
        // 创建字体设置容器面板，使用垂直布局
        val settingsPanel = JPanel()
        settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
        
        // 字体族设置
        val fontFamilyPanel = JPanel(BorderLayout())
        val fontFamilyLabel = JBLabel("字体族:")
        fontFamilyPanel.add(fontFamilyLabel, BorderLayout.WEST)
        fontFamilyPanel.add(fontFamilyCombo, BorderLayout.CENTER)
        settingsPanel.add(fontFamilyPanel)
        settingsPanel.add(Box.createVerticalStrut(5)) // 添加间距
        
        // 字体大小设置
        val fontSizePanel = JPanel(BorderLayout())
        val fontSizeLabel = JBLabel("字体大小:")
        fontSizePanel.add(fontSizeLabel, BorderLayout.WEST)
        fontSizePanel.add(fontSizeCombo, BorderLayout.CENTER)
        settingsPanel.add(fontSizePanel)
        settingsPanel.add(Box.createVerticalStrut(5)) // 添加间距
        
        // 行间距设置
        val lineSpacingPanel = JPanel(BorderLayout())
        val lineSpacingLabel = JBLabel("行间距:")
        lineSpacingPanel.add(lineSpacingLabel, BorderLayout.WEST)
        lineSpacingPanel.add(lineSpacingField, BorderLayout.CENTER)
        settingsPanel.add(lineSpacingPanel)
        settingsPanel.add(Box.createVerticalStrut(5)) // 添加间距
        
        // 段落间距设置
        val paragraphSpacingPanel = JPanel(BorderLayout())
        val paragraphSpacingLabel = JBLabel("段落间距:")
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
        panel.border = TitledBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),
            JBUI.Borders.empty(10)
        ), "界面显示选项")
        
        // 复选框使用默认字体
        
        // 创建复选框容器面板，使用GridBagLayout确保左对齐
        val checkboxPanel = JPanel()
        checkboxPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        
        // 添加复选框到容器面板
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        checkboxPanel.add(hideOperationPanelCheckBox, gbc)
        
        gbc.gridy = 1
        checkboxPanel.add(hideTitleButtonCheckBox, gbc)
        
        gbc.gridy = 2
        checkboxPanel.add(hideProgressLabelCheckBox, gbc)
        
        gbc.gridy = 3
        checkboxPanel.add(autoSaveProgressCheckBox, gbc)
        
        gbc.gridy = 4
        checkboxPanel.add(statusBarAutoScrollCheckBox, gbc)
        
        // 添加滚动间隔设置
        gbc.gridy = 5
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        val intervalLabel = JBLabel("滚动间隔(毫秒):")
        checkboxPanel.add(intervalLabel, gbc)
        
        gbc.gridx = 1
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        statusBarScrollIntervalField.toolTipText = "底部状态栏自动滚动的间隔时间，单位毫秒"
        checkboxPanel.add(statusBarScrollIntervalField, gbc)
        
        // 添加背景颜色设置
        gbc.gridx = 0
        gbc.gridy = 6
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        val backgroundColorLabel = JBLabel("阅读器背景颜色:")
        checkboxPanel.add(backgroundColorLabel, gbc)
        
        // 颜色预览和选择按钮面板
        gbc.gridx = 1
        gbc.insets = JBUI.insets(5, 10, 5, 0)
        val colorSelectionPanel = JPanel(FlowLayout())
        backgroundColorPreview.text = "  "
        backgroundColorPreview.background = Color.WHITE
        backgroundColorPreview.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(Color.GRAY),
            JBUI.Borders.empty(2)
        )
        backgroundColorPreview.preferredSize = java.awt.Dimension(30, 20)
        backgroundColorPreview.isOpaque = true
        colorSelectionPanel.add(backgroundColorPreview)
        colorSelectionPanel.add(backgroundColorButton)
        
        // 预设颜色按钮
        val presetColorPanel = JPanel()
        presetColorPanel.layout = BoxLayout(presetColorPanel, BoxLayout.X_AXIS)
        
        presetColorButton1.background = Color.decode("#2B2B2B")
        presetColorButton1.isBorderPainted = false
//        presetColorButton1.isOpaque = true
        presetColorButton1.foreground = Color.decode("#2B2B2B")
        presetColorButton1.preferredSize = java.awt.Dimension(50, 25)
        presetColorButton1.toolTipText = "深色背景 (#2B2B2B)"
        
        presetColorButton2.background = Color.decode("#FFFFFF")
        presetColorButton2.isBorderPainted = false
        presetColorButton2.isOpaque = true
        presetColorButton2.preferredSize = java.awt.Dimension(50, 25)
        presetColorButton2.toolTipText = "浅色背景 (#FFFFFF)"
        
        presetColorPanel.add(Box.createHorizontalStrut(5))
        presetColorPanel.add(presetColorButton1)
        presetColorPanel.add(Box.createHorizontalStrut(5))
        presetColorPanel.add(presetColorButton2)

        colorSelectionPanel.add( JLabel("预设："))
        colorSelectionPanel.add(presetColorPanel)
        checkboxPanel.add(colorSelectionPanel, gbc)
        
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
        panel.border = TitledBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),
            JBUI.Borders.empty(10)
        ), "预览")
        
        // 预览标签和编辑选项
        val previewHeaderPanel = JPanel(BorderLayout())
        previewHeaderPanel.add(editPreviewCheckBox, BorderLayout.EAST)
        
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 0, 5, 0)
        panel.add(previewHeaderPanel, gbc)
        
        // 预览区域
        val scrollPane = JBScrollPane(previewArea)
        scrollPane.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),
            JBUI.Borders.empty(5)
        )
        // 设置滚动面板的固定高度，防止编辑时被撑开
        previewArea.preferredSize = java.awt.Dimension( 450,160)
        previewArea.minimumSize = java.awt.Dimension(300, 160)
        previewArea.maximumSize = java.awt.Dimension(450, 160)
        // 设置滚动面板的固定高度
        scrollPane.preferredSize = java.awt.Dimension(460, 170)
        scrollPane.minimumSize = java.awt.Dimension(310, 170)
        scrollPane.maximumSize = java.awt.Dimension(460, 170)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE  // 改为NONE，使用固定尺寸
        gbc.anchor = GridBagConstraints.CENTER
        gbc.weightx = 0.0
        gbc.weighty = 0.0
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
        previewArea.text = previewText
        previewArea.border = JBUI.Borders.empty(10)
        
        // 设置预览区域的固定高度，防止编辑时被撑开
        previewArea.preferredSize = java.awt.Dimension(450, 200)
        previewArea.minimumSize = java.awt.Dimension(300, 200)
        previewArea.maximumSize = java.awt.Dimension(450, 200)
        
        // 设置编辑模式复选框
        editPreviewCheckBox.toolTipText = "勾选后可以在预览区域编辑文本"
        
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
        
        // 加载背景颜色设置
        loadBackgroundColorSettings()
        
        // 添加字体设置事件监听器
        fontFamilyCombo.addActionListener { updatePreview() }
        fontSizeCombo.addActionListener { updatePreview() }
        lineSpacingField.addActionListener { updatePreview() }
        paragraphSpacingField.addActionListener { updatePreview() }
        
        // 添加编辑模式切换监听器
        editPreviewCheckBox.addActionListener { togglePreviewEditMode() }
        
        // 添加显示设置事件监听器
        hideOperationPanelCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        hideTitleButtonCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        hideProgressLabelCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        autoSaveProgressCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        
        // 添加背景颜色设置事件监听器
        backgroundColorButton.addActionListener { showColorChooser() }
        
        // 预设颜色按钮事件监听器
        presetColorButton1.addActionListener { setPresetColor(DisplaySettings.DARK_THEME_BACKGROUND) }
        presetColorButton2.addActionListener { setPresetColor(DisplaySettings.LIGHT_THEME_BACKGROUND) }
        
        // 初始预览
        updatePreview()
        
        // 初始编辑模式
        togglePreviewEditMode()
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
    
    /**
     * 切换预览编辑模式
     */
    private fun togglePreviewEditMode() {
        previewArea.isEditable = editPreviewCheckBox.isSelected
        if (editPreviewCheckBox.isSelected) {
            previewArea.toolTipText = "现在可以编辑预览文本"
        } else {
            previewArea.toolTipText = "预览模式，不可编辑"
        }
    }
    
    /**
     * 加载背景颜色设置
     */
    private fun loadBackgroundColorSettings() {
        val backgroundColor = currentDisplaySettings.backgroundColor
        try {
            val color = Color.decode(backgroundColor)
            backgroundColorPreview.background = color
        } catch (e: Exception) {
            // 如果颜色解析失败，使用默认颜色
            backgroundColorPreview.background = Color.decode(DisplaySettings.DEFAULT.backgroundColor)
        }
    }
    
    /**
     * 显示颜色选择器
     */
    private fun showColorChooser() {
        val currentColor = backgroundColorPreview.background
        val selectedColor = ColorChooser.chooseColor(
            backgroundColorButton,
            "选择背景颜色",
            currentColor,
            true,
            emptyList(),
            true
        )
        
        if (selectedColor != null) {
            backgroundColorPreview.background = selectedColor
        }
    }
    
    /**
     * 设置预设颜色
     */
    private fun setPresetColor(colorHex: String) {
        try {
            val color = Color.decode(colorHex)
            backgroundColorPreview.background = color
        } catch (e: Exception) {
            // 忽略颜色解析错误
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
            val backgroundColor = String.format("#%06X", backgroundColorPreview.background.rgb and 0xFFFFFF)
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                statusBarAutoScrollCheckBox.isSelected,
                statusBarInterval,
                backgroundColor
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
            val backgroundColor = String.format("#%06X", backgroundColorPreview.background.rgb and 0xFFFFFF)
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                statusBarAutoScrollCheckBox.isSelected,
                statusBarInterval,
                backgroundColor
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
                        newDisplaySettings.hideProgressLabel,
                        newDisplaySettings.backgroundColor
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
