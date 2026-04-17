package com.github.xucux.read.setting

import com.github.xucux.read.constants.StorageConstants
import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.DisplaySettings
import com.github.xucux.read.model.FontSettings
import com.github.xucux.read.service.DisplaySettingsService
import com.github.xucux.read.service.FontSettingsService
import com.github.xucux.read.service.notify.ReaderNotificationService
import com.github.xucux.read.ui.ToolWindowHeaderVisibilityHelper
import com.intellij.ui.JBColor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
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
 * HelloRead设置组件
 * 包含字体设置等配置选项
 */
class HelloReadSettingComponent : SearchableConfigurable {
    // 字体设置区标签列固定宽度（65）：继续收窄左侧留白，同时保持“段落间距”等较长文案不被截断。
    private val labelColumnWidth = 65
    
    private val fontSettingsService = FontSettingsService.getInstance()
    private val displaySettingsService = DisplaySettingsService.getInstance()
    
    // 获取当前项目实例
    private val currentProject: Project?
        get() = ProjectManager.getInstance().openProjects.firstOrNull()
    
    // 字体设置UI组件
    private val fontFamilyCombo = ComboBox<String>()
    private val fontSizeCombo = ComboBox<String>()
    // 数值微调控件：支持键盘上下键和按钮点击微调，并限制为数字输入。
    private val lineSpacingSpinner = JSpinner(SpinnerNumberModel(1.2, 0.6, 3.0, 0.1))
    private val paragraphSpacingSpinner = JSpinner(SpinnerNumberModel(10, 0, 50, 1))
    private val previewArea = JTextPane()
    private val editPreviewCheckBox = JBCheckBox("启用预览编辑", true)
    
    // 界面显示选项UI组件
    private val hideOperationPanelCheckBox = JBCheckBox("隐藏阅读界面的操作面板")
    private val hideTitleButtonCheckBox = JBCheckBox("隐藏阅读界面的标题按钮")
    private val hideProgressLabelCheckBox = JBCheckBox("隐藏阅读界面的进度标签")
    private val autoSaveProgressCheckBox = JBCheckBox("自动保存阅读进度")
    private val showToolWindowHeaderCheckBox = JBCheckBox("显示HelloRead顶部栏")
    private val autoContrastFontColorCheckBox = JBCheckBox("字体颜色自动对比背景")
    private val statusBarAutoScrollCheckBox = JBCheckBox("底部状态栏自动滚动")
    private val statusBarScrollIntervalField = JBTextField()
    
    // 背景颜色设置UI组件
    private val backgroundColorButton = JButton("选择颜色")
    private val backgroundColorPreview = JBLabel()
    private val darkPresetRadio = JRadioButton("深色")
    private val lightPresetRadio = JRadioButton("浅色")
    private val darkPresetSwatch = JBLabel()
    private val lightPresetSwatch = JBLabel()
    private val fontColorButton = JButton("选择颜色")
    private val fontColorPreview = JBLabel()
    private val darkFontPresetRadio = JRadioButton("浅色字")
    private val lightFontPresetRadio = JRadioButton("深色字")
    private val darkFontPresetSwatch = JBLabel()
    private val lightFontPresetSwatch = JBLabel()
    
    // 当前设置
    private var currentFontSettings: FontSettings = fontSettingsService.loadFontSettings()
    private var originalFontSettings: FontSettings = currentFontSettings
    private var currentDisplaySettings: DisplaySettings = displaySettingsService.loadDisplaySettings()
    private var originalDisplaySettings: DisplaySettings = currentDisplaySettings
    private var currentToolWindowHeaderVisible: Boolean = displaySettingsService.loadToolWindowHeaderVisible()
    private var originalToolWindowHeaderVisible: Boolean = currentToolWindowHeaderVisible
    
    private val previewText: String = "这是字体预览文本。\n\n" +
                "在这里可以看到字体设置的效果。\n" +
                "包括字体族、字体大小、行间距和段落间距。\n\n" +
                "支持中文和英文混合显示。\n" +
                "The quick brown fox jumps over the lazy dog."

    // 左侧字体设置的两个下拉框使用统一固定宽度，避免随着父容器拉伸而变得过长。
    private val compactComboWidth = 100
    private val compactNumberWidth = 100

    override fun createComponent(): JComponent? {
        val contentBox = Box.createVerticalBox()
        val storagePathHint = JBLabel(
            "配置文件位置: ${System.getProperty("user.home")}${java.io.File.separator}${StorageConstants.READBOOK_DATA_DIR} " +
                "(颜色相关保存于 ${StorageConstants.DISPLAY_SETTINGS_FILE})"
        ).apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.emptyTop(8)
        }
        contentBox.add(createFontPreviewSplitPanel())
        // 分区之间保留视觉呼吸感，避免设置项紧贴。
        contentBox.add(Box.createVerticalStrut(JBUI.scale(16)))
        contentBox.add(createDisplayOptionsPanel())

        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        // 左右增加边距，让内容靠左但不贴边；与 IntelliJ 设置页视觉风格一致。
        contentPanel.border = JBUI.Borders.empty(12, 16)
        // 固定从顶部开始堆叠，避免在大窗口中垂直居中。
        contentPanel.add(contentBox, BorderLayout.NORTH)
        contentPanel.add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(storagePathHint, BorderLayout.WEST)
        }, BorderLayout.SOUTH)

        val scrollPane = JBScrollPane(contentPanel)
        scrollPane.border = JBUI.Borders.empty()
        
        // 初始化设置
        loadCurrentSettings()
        
        return scrollPane
    }
    
    /**
     * 创建字体设置面板
     */
    private fun createFontSettingsPanel(): JComponent {
        val settingsPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        addFormRow(settingsPanel, 0, "字体族", fontFamilyCombo)
        addFormRow(settingsPanel, 1, "字体大小", fontSizeCombo)
        addFormRow(settingsPanel, 2, "行间距", lineSpacingSpinner)
        addFormRow(settingsPanel, 3, "段落间距", paragraphSpacingSpinner)
        return FormBuilder.createFormBuilder()
            .addComponent(JBLabel("字体设置"))
            .addComponent(Box.createVerticalStrut(JBUI.scale(6)) as JComponent)
            .addComponent(settingsPanel)
            .panel
    }
    
    /**
     * 创建界面显示选项面板
     */
    private fun createDisplayOptionsPanel(): JComponent {
        val optionsPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            gridx = 0
            weightx = 1.0
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insetsBottom(4)
        }

        gbc.gridy = 0
        optionsPanel.add(hideOperationPanelCheckBox, gbc)
        gbc.gridy = 1
        optionsPanel.add(hideTitleButtonCheckBox, gbc)
        gbc.gridy = 2
        optionsPanel.add(hideProgressLabelCheckBox, gbc)
        gbc.gridy = 3
        optionsPanel.add(autoSaveProgressCheckBox, gbc)
        gbc.gridy = 4
        optionsPanel.add(showToolWindowHeaderCheckBox, gbc)
//        gbc.gridy = 4
//        optionsPanel.add(statusBarAutoScrollCheckBox, gbc)
//
//        statusBarScrollIntervalField.toolTipText = "底部状态栏自动滚动的间隔时间，单位毫秒"
//        addFormRow(optionsPanel, 5, "滚动间隔(毫秒)", statusBarScrollIntervalField, 20)

        initColorButtons()
        val colorRowPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            border = JBUI.Borders.empty()
            add(JBLabel("阅读器背景色:"))
            add(createColorSelectionPanel())
        }
        gbc.gridy = 5
        gbc.insets = JBUI.insetsTop(6)
        optionsPanel.add(colorRowPanel, gbc)

        val fontColorRowPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            border = JBUI.Borders.empty()
            add(JBLabel("阅读器字体色:"))
            add(createFontColorSelectionPanel())
        }
        gbc.gridy = 6
        gbc.insets = JBUI.insetsTop(4)
        optionsPanel.add(fontColorRowPanel, gbc)

        gbc.gridy = 7
        gbc.insets = JBUI.insetsTop(2)
        optionsPanel.add(autoContrastFontColorCheckBox, gbc)
        return FormBuilder.createFormBuilder()
            .addComponent(JBLabel("界面显示选项"))
            .addComponent(Box.createVerticalStrut(JBUI.scale(6)) as JComponent)
            .addComponent(optionsPanel)
            .panel
    }
    
    /**
     * 创建预览面板
     */
    private fun createPreviewPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints()
        panel.border = JBUI.Borders.empty()

        val title = JBLabel("预览")
        title.border = JBUI.Borders.emptyBottom(8)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.emptyInsets()
        panel.add(title, gbc)
        
        // 预览标签和编辑选项
        val previewHeaderPanel = JBPanel<JBPanel<*>>(BorderLayout())
        previewHeaderPanel.add(editPreviewCheckBox, BorderLayout.EAST)
        
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insetsBottom(6)
        panel.add(previewHeaderPanel, gbc)
        
        // 预览区域
        val scrollPane = JBScrollPane(previewArea)
        scrollPane.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor()),
            JBUI.Borders.empty(5)
        )
        // 预览区域尺寸策略：
        // - preferredSize 控制常规显示宽高
        // - minimumSize 保证窗口缩小时不至于压坏排版
        // 该尺寸区间针对中英文混排做过折中，优先保证可读性。
        previewArea.preferredSize = java.awt.Dimension(300, 160)
        previewArea.minimumSize = java.awt.Dimension(220, 160)
        scrollPane.preferredSize = java.awt.Dimension(350, 180)
        scrollPane.minimumSize = java.awt.Dimension(240, 180)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 1
        // 预览框仅做横向拉伸，避免纵向无意义扩张导致内容“空洞”。
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.insets(5, 0, 0, 0)
        panel.add(scrollPane, gbc)
        
        return panel
    }

    /**
     * 字体设置与预览采用左右分栏，比例30:70
     */
    private fun createFontPreviewSplitPanel(): JComponent {
        val settingsPanel = createFontSettingsPanel()
        val previewPanel = createPreviewPanel()
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        panel.border = JBUI.Borders.customLine(JBUI.CurrentTheme.DefaultTabs.borderColor().brighter())

        val gbc = GridBagConstraints().apply {
            gridy = 0
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
            weighty = 0.0
            // 外层统一边距，避免左右两块紧贴边框线。
            insets = JBUI.insets(10)
        }

        gbc.gridx = 0
        // 左侧设置列宽度进一步压缩（较原先约减半），把更多空间让给预览区。
        gbc.weightx = 0.18
        // 左块右侧预留 12px 间距，形成清晰分栏。
        gbc.insets = JBUI.insets(10, 10, 10, 12)
        panel.add(settingsPanel, gbc)

        gbc.gridx = 1
        // 右侧预览列占主要宽度，保障长文本可读性。
        gbc.weightx = 0.82
        gbc.insets = JBUI.insets(10, 0, 10, 10)
        panel.add(previewPanel, gbc)

        return panel
    }
    
    /**
     * 加载当前设置
     */
    private fun loadCurrentSettings() {
        // 设置字体族下拉框
        fontFamilyCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_FONTS.toTypedArray())
        fontFamilyCombo.preferredSize = java.awt.Dimension(compactComboWidth, fontFamilyCombo.preferredSize.height)
        fontFamilyCombo.minimumSize = fontFamilyCombo.preferredSize
        fontFamilyCombo.maximumSize = fontFamilyCombo.preferredSize
        
        // 设置字体大小下拉框
        fontSizeCombo.model = DefaultComboBoxModel(FontSettings.AVAILABLE_SIZES.map { it.toString() }.toTypedArray())
        fontSizeCombo.preferredSize = java.awt.Dimension(compactComboWidth, fontSizeCombo.preferredSize.height)
        fontSizeCombo.minimumSize = fontSizeCombo.preferredSize
        fontSizeCombo.maximumSize = fontSizeCombo.preferredSize

        initNumericSpinner()
        
        // 设置预览区域
        previewArea.isEditable = false
        previewArea.text = previewText
        previewArea.border = JBUI.Borders.empty(10)
        
        // 设置预览区域的固定高度，防止编辑时被撑开
        previewArea.preferredSize = java.awt.Dimension(315, 200)
        previewArea.minimumSize = java.awt.Dimension(220, 200)
        previewArea.maximumSize = java.awt.Dimension(315, 200)
        
        // 设置编辑模式复选框
        editPreviewCheckBox.toolTipText = "勾选后可以在预览区域编辑文本"
        
        // 加载当前字体设置值
        fontFamilyCombo.selectedItem = currentFontSettings.fontFamily
        fontSizeCombo.selectedItem = currentFontSettings.fontSize.toString()
        lineSpacingSpinner.value = currentFontSettings.lineSpacing.toDouble()
        paragraphSpacingSpinner.value = currentFontSettings.paragraphSpacing
        
        // 加载当前显示设置值
        hideOperationPanelCheckBox.isSelected = currentDisplaySettings.hideOperationPanel
        hideTitleButtonCheckBox.isSelected = currentDisplaySettings.hideTitleButton
        hideProgressLabelCheckBox.isSelected = currentDisplaySettings.hideProgressLabel
        autoSaveProgressCheckBox.isSelected = currentDisplaySettings.autoSaveProgress
        showToolWindowHeaderCheckBox.isSelected = currentToolWindowHeaderVisible
        autoContrastFontColorCheckBox.isSelected = currentDisplaySettings.autoContrastFontColor
        statusBarAutoScrollCheckBox.isSelected = currentDisplaySettings.statusBarAutoScroll
        statusBarScrollIntervalField.text = currentDisplaySettings.statusBarScrollInterval.toString()
        
        // 加载背景颜色设置
        loadBackgroundColorSettings()
        
        // 添加字体设置事件监听器
        fontFamilyCombo.addActionListener { updatePreview() }
        fontSizeCombo.addActionListener { updatePreview() }
        lineSpacingSpinner.addChangeListener { updatePreview() }
        paragraphSpacingSpinner.addChangeListener { updatePreview() }
        
        // 添加编辑模式切换监听器
        editPreviewCheckBox.addActionListener { togglePreviewEditMode() }
        
        // 添加显示设置事件监听器
        hideOperationPanelCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        hideTitleButtonCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        hideProgressLabelCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        autoSaveProgressCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        showToolWindowHeaderCheckBox.addActionListener { /* 设置变化时不需要特殊处理 */ }
        autoContrastFontColorCheckBox.addActionListener {
            updateFontColorControlsEnabledState()
            refreshFontColorByBackgroundIfAuto()
        }
//        statusBarAutoScrollCheckBox.addActionListener { updateStatusBarIntervalEnabledState() }
        
        // 添加背景颜色设置事件监听器
        backgroundColorButton.addActionListener { showColorChooser() }
        fontColorButton.addActionListener { showFontColorChooser() }
        
        // 预设颜色单选事件监听器
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
        darkFontPresetRadio.addActionListener {
            if (darkFontPresetRadio.isSelected) {
                setPresetFontColor(DisplaySettings.DARK_THEME_FONT)
            }
        }
        lightFontPresetRadio.addActionListener {
            if (lightFontPresetRadio.isSelected) {
                setPresetFontColor(DisplaySettings.LIGHT_THEME_FONT)
            }
        }
        
        // 初始预览
        updatePreview()
        
        // 初始编辑模式
        togglePreviewEditMode()
        updateFontColorControlsEnabledState()
        refreshFontColorByBackgroundIfAuto()
//        updateStatusBarIntervalEnabledState()
    }

    private fun initNumericSpinner() {
        lineSpacingSpinner.model = SpinnerNumberModel(
            currentFontSettings.lineSpacing.toDouble(),
            0.6,
            3.0,
            0.1
        )
        paragraphSpacingSpinner.model = SpinnerNumberModel(
            currentFontSettings.paragraphSpacing,
            0,
            50,
            1
        )

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
            // 行间距 6px；支持局部左缩进（例如某些子配置项）。
            insets = JBUI.insets(0, leftPadding, 6, 10)
        }

        gbc.gridx = 0
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        val label = JBLabel("$labelText:")
        // 固定标签列宽，避免因为文本长短导致控件列左右抖动。
        label.preferredSize = java.awt.Dimension(labelColumnWidth, label.preferredSize.height)
        panel.add(label, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(0, 0, 6, 0)
        panel.add(field, gbc)
    }

    private fun initColorButtons() {
        backgroundColorPreview.text = "  "
        backgroundColorPreview.border = JBUI.Borders.customLine(Color.GRAY)
        backgroundColorPreview.preferredSize = java.awt.Dimension(22, 18)
        backgroundColorPreview.minimumSize = java.awt.Dimension(22, 18)
        backgroundColorPreview.isOpaque = true

        val presetGroup = ButtonGroup()
        presetGroup.add(darkPresetRadio)
        presetGroup.add(lightPresetRadio)
        val fontPresetGroup = ButtonGroup()
        fontPresetGroup.add(darkFontPresetRadio)
        fontPresetGroup.add(lightFontPresetRadio)

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

        fontColorPreview.text = "A"
        fontColorPreview.border = JBUI.Borders.customLine(Color.GRAY)
        fontColorPreview.preferredSize = java.awt.Dimension(22, 18)
        fontColorPreview.minimumSize = java.awt.Dimension(22, 18)
        fontColorPreview.isOpaque = true
        fontColorPreview.horizontalAlignment = SwingConstants.CENTER
        // 预置为“之前的默认字体色”（随IDE主题）。
        fontColorPreview.foreground = Color.decode(DisplaySettings.getDefaultFontColor())

        darkFontPresetSwatch.background = Color.decode(DisplaySettings.DARK_THEME_FONT)
        darkFontPresetSwatch.isOpaque = true
        darkFontPresetSwatch.border = JBUI.Borders.customLine(Color.GRAY)
        darkFontPresetSwatch.preferredSize = java.awt.Dimension(16, 16)
        darkFontPresetSwatch.minimumSize = darkFontPresetSwatch.preferredSize
        darkFontPresetSwatch.toolTipText = "浅色字 (${DisplaySettings.DARK_THEME_FONT})"

        lightFontPresetSwatch.background = Color.decode(DisplaySettings.LIGHT_THEME_FONT)
        lightFontPresetSwatch.isOpaque = true
        lightFontPresetSwatch.border = JBUI.Borders.customLine(Color.GRAY)
        lightFontPresetSwatch.preferredSize = java.awt.Dimension(16, 16)
        lightFontPresetSwatch.minimumSize = lightFontPresetSwatch.preferredSize
        lightFontPresetSwatch.toolTipText = "深色字 (${DisplaySettings.LIGHT_THEME_FONT})"
    }

    private fun createColorSelectionPanel(): JComponent {
        // 左对齐 + 固定间距，颜色块、按钮、预设项按阅读顺序排列。
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0))
        panel.border = JBUI.Borders.empty()
        panel.add(backgroundColorPreview)
        panel.add(backgroundColorButton)
        panel.add(JBLabel("预设"))
        panel.add(createPresetOption(darkPresetRadio, darkPresetSwatch))
        panel.add(createPresetOption(lightPresetRadio, lightPresetSwatch))
        return panel
    }

    private fun createFontColorSelectionPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0))
        panel.border = JBUI.Borders.empty()
        panel.add(fontColorPreview)
        panel.add(fontColorButton)
        panel.add(JBLabel("预设"))
        panel.add(createPresetOption(darkFontPresetRadio, darkFontPresetSwatch))
        panel.add(createPresetOption(lightFontPresetRadio, lightFontPresetSwatch))
        return panel
    }

    private fun createPresetOption(radio: JRadioButton, swatch: JComponent): JComponent {
        return JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            border = JBUI.Borders.empty()
            add(radio)
            add(swatch)
        }
    }

    private fun updateStatusBarIntervalEnabledState() {
        val enabled = statusBarAutoScrollCheckBox.isSelected
        statusBarScrollIntervalField.isEnabled = enabled
    }
    
    /**
     * 更新预览
     */
    private fun updatePreview() {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = readLineSpacing()
            
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
        val fontColor = currentDisplaySettings.fontColor
        try {
            val color = Color.decode(backgroundColor)
            applyBackgroundToPreview(color)
            updatePresetSelection(color)
        } catch (e: Exception) {
            // 如果颜色解析失败，使用默认颜色
            val defaultColor = Color.decode(DisplaySettings.DEFAULT.backgroundColor)
            applyBackgroundToPreview(defaultColor)
            updatePresetSelection(defaultColor)
        }

        try {
            val color = Color.decode(fontColor)
            applyFontColorToPreview(color)
            updateFontPresetSelection(color)
        } catch (e: Exception) {
            val defaultColor = Color.decode(DisplaySettings.DEFAULT.fontColor)
            applyFontColorToPreview(defaultColor)
            updateFontPresetSelection(defaultColor)
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
            applyBackgroundToPreview(selectedColor)
            refreshFontColorByBackgroundIfAuto()
            updatePresetSelection(selectedColor)
        }
    }

    private fun showFontColorChooser() {
        val currentColor = fontColorPreview.foreground
        val selectedColor = JColorChooser.showDialog(
            fontColorButton,
            "选择字体颜色",
            currentColor
        )

        if (selectedColor != null) {
            applyFontColorToPreview(selectedColor)
            updateFontPresetSelection(selectedColor)
        }
    }
    
    /**
     * 设置预设颜色
     */
    private fun setPresetColor(colorHex: String) {
        try {
            val color = Color.decode(colorHex)
            applyBackgroundToPreview(color)
            refreshFontColorByBackgroundIfAuto()
            updatePresetSelection(color)
        } catch (e: Exception) {
            // 忽略颜色解析错误
        }
    }

    private fun setPresetFontColor(colorHex: String) {
        try {
            val color = Color.decode(colorHex)
            applyFontColorToPreview(color)
            updateFontPresetSelection(color)
        } catch (e: Exception) {
            // 忽略颜色解析错误
        }
    }

    /**
     * 背景色在「配置预览色块」与「预览文本域」上保持同步。
     */
    private fun applyBackgroundToPreview(color: Color) {
        backgroundColorPreview.background = color
        previewArea.background = color
    }

    private fun applyFontColorToPreview(color: Color) {
        fontColorPreview.foreground = color
        previewArea.foreground = color
    }

    private fun updateFontColorControlsEnabledState() {
        val manualEnabled = !autoContrastFontColorCheckBox.isSelected
        fontColorButton.isEnabled = manualEnabled
        darkFontPresetRadio.isEnabled = manualEnabled
        lightFontPresetRadio.isEnabled = manualEnabled
    }

    private fun refreshFontColorByBackgroundIfAuto() {
        if (!autoContrastFontColorCheckBox.isSelected) {
            return
        }
        val backgroundHex = String.format("#%06X", backgroundColorPreview.background.rgb and 0xFFFFFF)
        val recommendedHex = DisplaySettings.getRecommendedFontColor(backgroundHex)
        setPresetFontColor(recommendedHex)
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

    private fun updateFontPresetSelection(color: Color) {
        val normalizedColor = String.format("#%06X", color.rgb and 0xFFFFFF)
        when {
            normalizedColor.equals(DisplaySettings.DARK_THEME_FONT, ignoreCase = true) -> darkFontPresetRadio.isSelected = true
            normalizedColor.equals(DisplaySettings.LIGHT_THEME_FONT, ignoreCase = true) -> lightFontPresetRadio.isSelected = true
            else -> {
                darkFontPresetRadio.isSelected = false
                lightFontPresetRadio.isSelected = false
            }
        }
    }
    


    override fun isModified(): Boolean {
        try {
            val fontFamily = fontFamilyCombo.selectedItem as? String ?: currentFontSettings.fontFamily
            val fontSize = (fontSizeCombo.selectedItem as? String)?.toIntOrNull() ?: currentFontSettings.fontSize
            val lineSpacing = readLineSpacing()
            val paragraphSpacing = readParagraphSpacing()
            
            val newFontSettings = FontSettings(fontFamily, fontSize, lineSpacing, paragraphSpacing)
            val backgroundColor = String.format("#%06X", backgroundColorPreview.background.rgb and 0xFFFFFF)
            val fontColor = String.format("#%06X", fontColorPreview.foreground.rgb and 0xFFFFFF)
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                // 临时下线设置项：沿用当前值，避免隐藏后误改配置
                currentDisplaySettings.statusBarAutoScroll,
                currentDisplaySettings.statusBarScrollInterval,
                backgroundColor,
                fontColor,
                autoContrastFontColorCheckBox.isSelected
            )
            
            return newFontSettings != originalFontSettings ||
                newDisplaySettings != originalDisplaySettings ||
                showToolWindowHeaderCheckBox.isSelected != originalToolWindowHeaderVisible
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
            val lineSpacing = readLineSpacing()
            val paragraphSpacing = readParagraphSpacing()
            
            val newFontSettings = FontSettings(fontFamily, fontSize, lineSpacing, paragraphSpacing)
            val backgroundColor = String.format("#%06X", backgroundColorPreview.background.rgb and 0xFFFFFF)
            val fontColor = String.format("#%06X", fontColorPreview.foreground.rgb and 0xFFFFFF)
            val newDisplaySettings = DisplaySettings(
                hideOperationPanelCheckBox.isSelected,
                hideTitleButtonCheckBox.isSelected,
                hideProgressLabelCheckBox.isSelected,
                autoSaveProgressCheckBox.isSelected,
                // 临时下线设置项：沿用当前值，避免隐藏后误改配置
                currentDisplaySettings.statusBarAutoScroll,
                currentDisplaySettings.statusBarScrollInterval,
                backgroundColor,
                fontColor,
                autoContrastFontColorCheckBox.isSelected
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

                val newToolWindowHeaderVisible = showToolWindowHeaderCheckBox.isSelected
                displaySettingsService.saveToolWindowHeaderVisible(newToolWindowHeaderVisible)
                currentToolWindowHeaderVisible = newToolWindowHeaderVisible
                originalToolWindowHeaderVisible = newToolWindowHeaderVisible
                
                // 通知阅读器更新设置
                currentProject?.let { project ->
                    val readerNotificationService = ReaderNotificationService(project)
                    readerNotificationService.notifyReaderUpdateFont()
                    readerNotificationService.notifyReaderUpdateDisplay(
                        newDisplaySettings.hideOperationPanel,
                        newDisplaySettings.hideTitleButton,
                        newDisplaySettings.hideProgressLabel,
                        newDisplaySettings.backgroundColor,
                        newDisplaySettings.fontColor,
                        newDisplaySettings.autoContrastFontColor
                    )

                    val toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow(TabConstants.HELLO_READ_TOOL_WINDOW_ID)
                    if (toolWindow != null) {
                        ToolWindowHeaderVisibilityHelper.apply(toolWindow, newToolWindowHeaderVisible)
                    }
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
