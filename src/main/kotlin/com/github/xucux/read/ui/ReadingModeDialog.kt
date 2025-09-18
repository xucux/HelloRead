package com.github.xucux.read.ui

import com.github.xucux.read.model.ReadingMode
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

/**
 * 阅读模式选择对话框
 */
class ReadingModeDialog(
    project: Project,
    currentMode: ReadingMode
) : DialogWrapper(project) {

    private val chapterModeRadio = JBRadioButton("章节阅读模式")
    private val scrollModeRadio = JBRadioButton("滚动阅读模式")
    private val statusBarModeRadio = JBRadioButton("底部状态栏阅读模式")
    private val buttonGroup = ButtonGroup()

    init {
        title = "选择阅读模式"
        init()

        // 设置单选按钮组
        buttonGroup.add(chapterModeRadio)
        buttonGroup.add(scrollModeRadio)
        buttonGroup.add(statusBarModeRadio)

        // 设置当前选中的模式
        when (currentMode) {
            ReadingMode.CHAPTER_MODE -> chapterModeRadio.isSelected = true
            ReadingMode.SCROLL_MODE -> scrollModeRadio.isSelected = true
            ReadingMode.STATUS_BAR_MODE -> statusBarModeRadio.isSelected = true
        }

        // 设置字体
        val font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        chapterModeRadio.font = font
        scrollModeRadio.font = font
        statusBarModeRadio.font = font
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(20)

        // 标题
        val titleLabel = JBLabel("请选择阅读模式：")
        titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        titleLabel.border = JBUI.Borders.empty(0, 0, 15, 0)

        // 模式说明
        val chapterModeDescription = JBLabel(
            "<html><body style='width: 300px'>" +
            "<b>章节阅读模式：</b><br/>" +
            "• 每次页面仅显示一个章节的内容<br/>" +
            "• 通过点击上一章/下一章按钮切换章节<br/>" +
            "• 适合精读和按章节阅读"
            )
        chapterModeDescription.border = JBUI.Borders.empty(5, 20, 10, 0)

        val scrollModeDescription = JBLabel(
            "<html><body style='width: 300px'>" +
            "<b>滚动阅读模式：</b><br/>" +
            "• 当滚动到内容末尾时，自动加载下一章节<br/>" +
            "• 当滚动到顶部时，自动加载上一章节<br/>" +
            "• 适合连续阅读和快速浏览"
            )
        scrollModeDescription.border = JBUI.Borders.empty(5, 20, 10, 0)

        val statusBarModeDescription = JBLabel(
            "<html><body style='width: 300px'>" +
            "<b>底部状态栏阅读模式：</b><br/>" +
            "• 在IDE底部状态栏显示阅读内容<br/>" +
            "• 每次仅展示一行，自动滚动<br/>" +
            "• 鼠标悬停显示当前章节和书名<br/>" +
            "• 适合边工作边阅读"
            )
        statusBarModeDescription.border = JBUI.Borders.empty(5, 20, 10, 0)

        // 单选按钮面板
        val radioPanel = JPanel()
        radioPanel.layout = BoxLayout(radioPanel, BoxLayout.Y_AXIS)
        radioPanel.add(chapterModeRadio)
        radioPanel.add(chapterModeDescription)
        radioPanel.add(Box.createVerticalStrut(10))
        radioPanel.add(scrollModeRadio)
        radioPanel.add(scrollModeDescription)
        radioPanel.add(Box.createVerticalStrut(10))
        radioPanel.add(statusBarModeRadio)
        radioPanel.add(statusBarModeDescription)

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(radioPanel, BorderLayout.CENTER)

        return panel
    }

    /**
     * 获取选中的阅读模式
     */
    fun getSelectedMode(): ReadingMode {
        return when {
            chapterModeRadio.isSelected -> ReadingMode.CHAPTER_MODE
            scrollModeRadio.isSelected -> ReadingMode.SCROLL_MODE
            statusBarModeRadio.isSelected -> ReadingMode.STATUS_BAR_MODE
            else -> ReadingMode.CHAPTER_MODE
        }
    }
}