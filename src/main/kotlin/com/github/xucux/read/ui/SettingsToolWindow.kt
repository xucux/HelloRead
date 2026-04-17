package com.github.xucux.read.ui

import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.DisplaySettings
import com.github.xucux.read.model.FontSettings
import com.github.xucux.read.service.DisplaySettingsService
import com.github.xucux.read.service.FontSettingsService
import com.github.xucux.read.service.notify.ReaderNotificationService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.JSpinner.NumberEditor
import javax.swing.SpinnerNumberModel
import javax.swing.text.*

/**
 * 设置工具窗口
 * 包含字体设置、界面显示选项等
 */
class SettingsToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {
    private val labelColumnWidth = 65
    private val compactComboWidth = 100
    private val compactNumberWidth = 100
    
    private val fontSettingsService = FontSettingsService.getInstance()
    private val displaySettingsService = DisplaySettingsService.getInstance()
    private val readerNotificationService = ReaderNotificationService(project)
    
    // 字体设置相关
    private lateinit var fontFamilyCombo: ComboBox<String>
    private lateinit var fontSizeCombo: ComboBox<String>
    private lateinit var lineSpacingSpinner: JSpinner
    private lateinit var paragraphSpacingSpinner: JSpinner
    private lateinit var previewArea: JTextPane
    private lateinit var applyFontButton: JButton
    private lateinit var editPreviewCheckBox: JBCheckBox
    
    // 界面显示选项
    private lateinit var hideOperationPanelCheckBox: JBCheckBox
    private lateinit var hideTitleButtonCheckBox: JBCheckBox
    private lateinit var hideProgressLabelCheckBox: JBCheckBox
    private lateinit var autoSaveProgressCheckBox: JBCheckBox
    private lateinit var statusBarAutoScrollCheckBox: JBCheckBox
    private lateinit var statusBarScrollIntervalField: JBTextField
    
    // 背景颜色设置
    private lateinit var backgroundColorButton: JButton
    private lateinit var backgroundColorPreview: JBLabel
    private lateinit var darkPresetRadio: JRadioButton
    private lateinit var lightPresetRadio: JRadioButton
    private lateinit var darkPresetSwatch: JBLabel
    private lateinit var lightPresetSwatch: JBLabel
    
    
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
        val contentBox = Box.createVerticalBox()
        contentBox.add(createFontSettingsPanel())
        contentBox.add(Box.createVerticalStrut(JBUI.scale(16)))
        contentBox.add(createDisplayOptionsPanel())

        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        contentPanel.border = JBUI.Borders.empty(12, 16)
        contentPanel.add(contentBox, BorderLayout.NORTH)

        val scrollPane = JBScrollPane(contentPanel)
        scrollPane.border = JBUI.Borders.empty()
        add(scrollPane, BorderLayout.CENTER)
    }
    
    /**
     * 创建字体设置面板
     */
    private fun createFontSettingsPanel(): JComponent {
        val settingsPanel = JBPanel<JBPanel<*>>(GridBagLayout())

        fontFamilyCombo = ComboBox<String>()
        fontFamilyCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_FONTS.toTypedArray())
        fontFamilyCombo.preferredSize = java.awt.Dimension(compactComboWidth, fontFamilyCombo.preferredSize.height)
        fontFamilyCombo.minimumSize = fontFamilyCombo.preferredSize
        fontFamilyCombo.maximumSize = fontFamilyCombo.preferredSize

        fontSizeCombo = ComboBox<String>()
        fontSizeCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_SIZES.map { it.toString() }.toTypedArray())
        fontSizeCombo.preferredSize = java.awt.Dimension(compactComboWidth, fontSizeCombo.preferredSize.height)
        fontSizeCombo.minimumSize = fontSizeCombo.preferredSize
        fontSizeCombo.maximumSize = fontSizeCombo.preferredSize

        lineSpacingSpinner = JSpinner(SpinnerNumberModel(1.2, 0.6, 3.0, 0.1))
        paragraphSpacingSpinner = JSpinner(SpinnerNumberModel(10, 0, 50, 1))
        initNumericSpinners()

        addFormRow(settingsPanel, 0, "字体族", fontFamilyCombo)
        addFormRow(settingsPanel, 1, "字体大小", fontSizeCombo)
        addFormRow(settingsPanel, 2, "行间距", lineSpacingSpinner)
        addFormRow(settingsPanel, 3, "段落间距", paragraphSpacingSpinner)

        // 应用按钮
        applyFontButton = JButton("应用字体设置")
        val buttonPanel = JBPanel<JBPanel<*>>(BorderLayout())
        buttonPanel.border = JBUI.Borders.emptyTop(4)
        buttonPanel.add(applyFontButton, BorderLayout.WEST)
        addFieldRow(settingsPanel, 4, buttonPanel)

        // 预览区域（右侧）
        val previewPanel = createPreviewPanel()

        val title = JBLabel("字体设置")
        title.border = JBUI.Borders.emptyBottom(8)

        val splitPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        splitPanel.border = JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor().brighter())
        val gbc = GridBagConstraints().apply {
            gridy = 0
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
            weighty = 0.0
            insets = JBUI.insets(10)
        }
        gbc.gridx = 0
        gbc.weightx = 0.18
        gbc.insets = JBUI.insets(10, 10, 10, 12)
        splitPanel.add(settingsPanel, gbc)
        gbc.gridx = 1
        gbc.weightx = 0.82
        gbc.insets = JBUI.insets(10, 0, 10, 10)
        splitPanel.add(previewPanel, gbc)

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(title, BorderLayout.NORTH)
            add(splitPanel, BorderLayout.CENTER)
        }
    }

    private fun createPreviewPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints()

        val previewHeaderPanel = JBPanel<JBPanel<*>>(BorderLayout())
        val previewLabel = JBLabel("预览")
        previewHeaderPanel.add(previewLabel, BorderLayout.WEST)

        editPreviewCheckBox = JBCheckBox("启用预览编辑", true)
        editPreviewCheckBox.toolTipText = "勾选后可以在预览区域编辑文本"
        previewHeaderPanel.add(editPreviewCheckBox, BorderLayout.EAST)

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insetsBottom(6)
        panel.add(previewHeaderPanel, gbc)

        previewArea = JTextPane()
        previewArea.isEditable = false
        previewArea.text = "这是字体预览文本。\n\n" +
            "在这里可以看到字体设置的效果。\n" +
            "包括字体族、字体大小、行间距和段落间距。\n\n" +
            "支持中文和英文混合显示。\n" +
            "The quick brown fox jumps over the lazy dog."
        previewArea.border = JBUI.Borders.empty(8)
        previewArea.toolTipText = "预览模式，不可编辑"

        val scrollPane = JBScrollPane(previewArea)
        scrollPane.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),
            JBUI.Borders.empty(5)
        )
        scrollPane.preferredSize = java.awt.Dimension(350, 180)
        scrollPane.minimumSize = java.awt.Dimension(240, 180)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.emptyInsets()
        panel.add(scrollPane, gbc)

        return panel
    }
    
    /**
     * 创建界面显示选项面板
     */
    private fun createDisplayOptionsPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.emptyTop(4)
        
        val title = JBLabel("界面显示选项")
        title.border = JBUI.Borders.emptyBottom(8)
        panel.add(title, BorderLayout.NORTH)

        val optionsPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            gridx = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = JBUI.insetsBottom(4)
        }
        
        hideOperationPanelCheckBox = JBCheckBox("隐藏阅读界面的操作面板")
        gbc.gridy = 0
        optionsPanel.add(hideOperationPanelCheckBox, gbc)
        
        hideTitleButtonCheckBox = JBCheckBox("隐藏阅读界面的标题按钮")
        gbc.gridy = 1
        optionsPanel.add(hideTitleButtonCheckBox, gbc)
        
        hideProgressLabelCheckBox = JBCheckBox("隐藏阅读界面的进度标签")
        gbc.gridy = 2
        optionsPanel.add(hideProgressLabelCheckBox, gbc)
        
        autoSaveProgressCheckBox = JBCheckBox("自动保存阅读进度")
        autoSaveProgressCheckBox.isSelected = true
        gbc.gridy = 3
        optionsPanel.add(autoSaveProgressCheckBox, gbc)
        
        statusBarAutoScrollCheckBox = JBCheckBox("底部状态栏自动滚动")
        statusBarScrollIntervalField = JBTextField()
        statusBarScrollIntervalField.toolTipText = "底部状态栏自动滚动的间隔时间，单位毫秒"
        // 暂时下线状态栏滚动项，保留字段用于向后兼容与配置持久化。
//        gbc.gridy = 4
//        optionsPanel.add(statusBarAutoScrollCheckBox, gbc)
//        addFormRow(optionsPanel, 5, "滚动间隔(毫秒)", statusBarScrollIntervalField, 20)

        backgroundColorPreview = JBLabel("  ")
        backgroundColorButton = JButton("选择颜色")
        initColorControls()

        darkPresetRadio = JRadioButton("深色")
        lightPresetRadio = JRadioButton("浅色")
        darkPresetSwatch = JBLabel()
        lightPresetSwatch = JBLabel()
        initPresetColorOptions()

        val colorRowPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            border = JBUI.Borders.empty()
            add(JBLabel("阅读器背景色:"))
            add(createColorSelectionPanel())
        }
        gbc.gridy = 4
        gbc.insets = JBUI.insetsTop(6)
        optionsPanel.add(colorRowPanel, gbc)
        
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
        
        lineSpacingSpinner.addChangeListener { updateFontPreview() }
        paragraphSpacingSpinner.addChangeListener { updateFontPreview() }
        
        // 编辑模式切换监听器
        editPreviewCheckBox.addActionListener {
            togglePreviewEditMode()
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
        
//        statusBarAutoScrollCheckBox.addActionListener {
//            saveDisplaySettings()
//            updateStatusBarIntervalEnabledState()
//        }
//
//        statusBarScrollIntervalField.addActionListener {
//            saveDisplaySettings()
//        }
        
        // 背景颜色设置事件监听器
        backgroundColorButton.addActionListener {
            showColorChooser()
        }
        
        darkPresetRadio.addActionListener {
            if (darkPresetRadio.isSelected) {
                setPresetColor(DisplaySettings.DARK_THEME_BACKGROUND)
            }
        }
        lightPresetRadio.addActionListener {
            if (lightPresetRadio.isSelected) {
                setPresetColor(DisplaySettings.LIGHT_THEME_BACKGROUND)
            }
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
        
        // 初始编辑模式
        togglePreviewEditMode()
        
        // 加载显示设置（这里可以从配置文件或服务中加载）
        loadDisplaySettings()
//        updateStatusBarIntervalEnabledState()
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
        lineSpacingSpinner.value = currentFontSettings.lineSpacing.toDouble()
        paragraphSpacingSpinner.value = currentFontSettings.paragraphSpacing
    }
    
    /**
     * 更新字体预览
     */
    private fun updateFontPreview() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = readLineSpacing()
            
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
     * 保存字体设置
     */
    private fun saveFontSettings() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = readLineSpacing()
            val paragraphSpacing = readParagraphSpacing()

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
        
        // 加载背景颜色设置
        loadBackgroundColorSettings()
    }

    /**
     * 保存显示设置
     */
    private fun saveDisplaySettings() {
        try {
            // 创建新的显示设置对象
            val backgroundColor = String.format("#%06X", backgroundColorPreview.background.rgb and 0xFFFFFF)
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                // 暂时下线项：沿用当前值，避免隐藏后意外覆盖。
                currentDisplaySettings.statusBarAutoScroll,
                currentDisplaySettings.statusBarScrollInterval,
                backgroundColor
            )
            
            // 保存到存储
            displaySettingsService.saveDisplaySettings(newDisplaySettings)
            currentDisplaySettings = newDisplaySettings
            
        } catch (e: Exception) {
            // 忽略错误，保持当前设置
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
            updatePresetSelection(color)
        } catch (e: Exception) {
            // 如果颜色解析失败，使用默认颜色
            val defaultColor = Color.decode(DisplaySettings.DEFAULT.backgroundColor)
            backgroundColorPreview.background = defaultColor
            updatePresetSelection(defaultColor)
        }
    }
    
    /**
     * 显示颜色选择器
     */
    private fun showColorChooser() {
        val currentColor = backgroundColorPreview.background
        val selectedColor = JColorChooser.showDialog(
            backgroundColorButton,
            "选择背景颜色",
            currentColor
        )
        
        if (selectedColor != null) {
            backgroundColorPreview.background = selectedColor
            updatePresetSelection(selectedColor)
            saveDisplaySettings()
            readerNotificationService.notifyReaderUpdateDisplay(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                String.format("#%06X", selectedColor.rgb and 0xFFFFFF)
            )
        }
    }
    
    /**
     * 设置预设颜色
     */
    private fun setPresetColor(colorHex: String) {
        try {
            val color = Color.decode(colorHex)
            backgroundColorPreview.background = color
            updatePresetSelection(color)
            saveDisplaySettings()
            readerNotificationService.notifyReaderUpdateDisplay(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                colorHex
            )
        } catch (e: Exception) {
            // 忽略颜色解析错误
        }
    }

    private fun addFormRow(
        panel: JComponent,
        row: Int,
        labelText: String,
        field: JComponent,
        leftPadding: Int = 0
    ) {
        val gbc = GridBagConstraints().apply {
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(0, leftPadding, 6, 10)
        }

        gbc.gridx = 0
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        val label = JBLabel("$labelText:")
        label.preferredSize = java.awt.Dimension(labelColumnWidth, label.preferredSize.height)
        panel.add(label, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(0, 0, 6, 0)
        panel.add(field, gbc)
    }

    private fun addFieldRow(panel: JComponent, row: Int, field: JComponent) {
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(0, 0, 6, 0)
        }
        panel.add(field, gbc)
    }

    private fun initColorControls() {
        backgroundColorPreview.border = JBUI.Borders.customLine(Color.GRAY)
        backgroundColorPreview.preferredSize = java.awt.Dimension(22, 18)
        backgroundColorPreview.minimumSize = java.awt.Dimension(22, 18)
        backgroundColorPreview.isOpaque = true
    }

    private fun initPresetColorOptions() {
        val presetGroup = ButtonGroup()
        presetGroup.add(darkPresetRadio)
        presetGroup.add(lightPresetRadio)

        darkPresetSwatch.background = Color.decode(DisplaySettings.DARK_THEME_BACKGROUND)
        darkPresetSwatch.isOpaque = true
        darkPresetSwatch.border = JBUI.Borders.customLine(Color.GRAY)
        darkPresetSwatch.preferredSize = java.awt.Dimension(16, 16)
        darkPresetSwatch.minimumSize = darkPresetSwatch.preferredSize
        darkPresetSwatch.toolTipText = "深色背景 (${DisplaySettings.DARK_THEME_BACKGROUND})"

        lightPresetSwatch.background = Color.decode(DisplaySettings.LIGHT_THEME_BACKGROUND)
        lightPresetSwatch.isOpaque = true
        lightPresetSwatch.border = JBUI.Borders.customLine(Color.GRAY)
        lightPresetSwatch.preferredSize = java.awt.Dimension(16, 16)
        lightPresetSwatch.minimumSize = lightPresetSwatch.preferredSize
        lightPresetSwatch.toolTipText = "浅色背景 (${DisplaySettings.LIGHT_THEME_BACKGROUND})"
    }

    private fun createColorSelectionPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0))
        panel.border = JBUI.Borders.empty()
        panel.add(backgroundColorPreview)
        panel.add(backgroundColorButton)
        panel.add(JBLabel("预设"))
        panel.add(createPresetOption(darkPresetRadio, darkPresetSwatch))
        panel.add(createPresetOption(lightPresetRadio, lightPresetSwatch))
        return panel
    }

    private fun createPresetOption(radio: JRadioButton, swatch: JComponent): JComponent {
        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            border = JBUI.Borders.empty()
            add(radio)
            add(swatch)
        }
    }

    private fun initNumericSpinners() {
        val lineEditor = NumberEditor(lineSpacingSpinner, "0.0")
        val lineFormatter = lineEditor.textField.formatter as? NumberFormatter
        lineFormatter?.allowsInvalid = false
        lineFormatter?.commitsOnValidEdit = true
        lineSpacingSpinner.editor = lineEditor

        val paragraphEditor = NumberEditor(paragraphSpacingSpinner, "0")
        val paragraphFormatter = paragraphEditor.textField.formatter as? NumberFormatter
        paragraphFormatter?.allowsInvalid = false
        paragraphFormatter?.commitsOnValidEdit = true
        paragraphSpacingSpinner.editor = paragraphEditor

        val lineHeight = fontSizeCombo.preferredSize.height
        lineSpacingSpinner.preferredSize = java.awt.Dimension(compactNumberWidth, lineHeight)
        lineSpacingSpinner.minimumSize = lineSpacingSpinner.preferredSize
        lineSpacingSpinner.maximumSize = lineSpacingSpinner.preferredSize
        paragraphSpacingSpinner.preferredSize = java.awt.Dimension(compactNumberWidth, lineHeight)
        paragraphSpacingSpinner.minimumSize = paragraphSpacingSpinner.preferredSize
        paragraphSpacingSpinner.maximumSize = paragraphSpacingSpinner.preferredSize
    }

    private fun readLineSpacing(): Float {
        return ((lineSpacingSpinner.value as? Number)?.toFloat() ?: currentFontSettings.lineSpacing).coerceIn(0.6f, 3.0f)
    }

    private fun readParagraphSpacing(): Int {
        return ((paragraphSpacingSpinner.value as? Number)?.toInt() ?: currentFontSettings.paragraphSpacing).coerceIn(0, 50)
    }

    private fun updatePresetSelection(color: Color) {
        val normalizedColor = String.format("#%06X", color.rgb and 0xFFFFFF)
        when {
            normalizedColor.equals(DisplaySettings.DARK_THEME_BACKGROUND, ignoreCase = true) -> darkPresetRadio.isSelected = true
            normalizedColor.equals(DisplaySettings.LIGHT_THEME_BACKGROUND, ignoreCase = true) -> lightPresetRadio.isSelected = true
            else -> {
                darkPresetRadio.isSelected = false
                lightPresetRadio.isSelected = false
            }
        }
    }

    private fun updateStatusBarIntervalEnabledState() {
        statusBarScrollIntervalField.isEnabled = statusBarAutoScrollCheckBox.isSelected
    }
    
    /**
     * 获取IDEA编辑器背景颜色
     */
    private fun getIDEAEditorBackgroundColor(): Color {
        return try {
            // 尝试获取IDEA编辑器的背景颜色
            com.intellij.util.ui.UIUtil.getEditorPaneBackground()
        } catch (e: Exception) {
            // 如果获取失败，使用系统默认背景色
            Color.WHITE
        }
    }

}