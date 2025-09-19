package com.github.xucux.read.ui

import com.github.xucux.read.model.Book
import com.github.xucux.read.service.ChapterParserService
import com.github.xucux.read.service.DataStorageService
import com.github.xucux.read.util.PopNotifyUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.openapi.diagnostic.logger
import java.awt.Dimension
import java.io.File
import java.util.*
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 添加书籍对话框
 */
class AddBookDialog(project: Project) : DialogWrapper(project) {
    
    companion object {
        private val logger = logger<AddBookDialog>()
    }
    
    private val dataStorageService = DataStorageService.getInstance()
    private val chapterParserService = ChapterParserService.getInstance()
    private val descField = JBLabel("请选择utf-8编码的txt文件")
    private val titleField = JBTextField()
    private val filePathField = JBTextField()
    private val filePathLabel = JBLabel("文件路径:")
    private val titleLabel = JBLabel("书籍标题:")
    
    private var selectedFile: File? = null
    
    init {
        title = "添加新书籍"
        init()
        
        // 设置文件路径字段为只读
        filePathField.isEditable = false
        
        // 添加文件选择按钮
        setupFileChooser()
    }
    
    /**
     * 设置文件选择器
     */
    private fun setupFileChooser() {
        filePathField.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    chooseFile()
                }
            }
        })
    }
    
    /**
     * 选择文件
     */
    private fun chooseFile() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withFileFilter { file ->
                file.extension?.toLowerCase(Locale.getDefault()) == "txt"
            }
            .withTitle("选择小说文件")
            .withDescription("请选择UTF-8编码的txt小说文件")
        
        val fileChooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
            descriptor,
            null,
            null
        )
        
        if (fileChooser != null) {
            selectedFile = File(fileChooser.path)
            filePathField.text = selectedFile!!.absolutePath
            
            // 自动设置书籍标题
            if (titleField.text.isEmpty()) {
                titleField.text = selectedFile!!.nameWithoutExtension
            }
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("备注:",descField)
            .addLabeledComponent(titleLabel, titleField, 1, false)
            .addLabeledComponent(filePathLabel, filePathField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        return panel
    }
    
    override fun doOKAction() {
        val title = titleField.text.trim()
        val file = selectedFile
        
        if (title.isEmpty()) {
            PopNotifyUtil.notify("输入错误", "请输入书籍标题")
            Messages.showErrorDialog("请输入书籍标题", "错误")
            return
        }
        
        if (file == null || !file.exists()) {
            PopNotifyUtil.notify("文件错误", "请选择有效的txt文件")
            Messages.showErrorDialog("请选择有效的txt文件", "错误")
            return
        }
        
        if (!file.extension.toLowerCase(Locale.getDefault()).endsWith("txt")) {
            PopNotifyUtil.notify("格式错误", "请选择txt格式的文件")
            Messages.showErrorDialog("请选择txt格式的文件", "错误")
            return
        }
        
        // 检查文件编码
        if (!isUtf8File(file)) {
            PopNotifyUtil.notify("编码错误", "文件不是UTF-8编码，请转换后重试")
            Messages.showErrorDialog("文件不是UTF-8编码，请转换后重试", "错误")
            return
        }
        
        try {
            // 解析章节（添加书籍时不需要缓存，因为还没有bookId）
            val chapters = chapterParserService.parseChapters(file)
            if (chapters.isEmpty()) {
                PopNotifyUtil.notify("解析错误", "无法解析章节，请检查文件格式")
                Messages.showErrorDialog("无法解析章节，请检查文件格式", "错误")
                return
            }
            
            // 计算文件总行数
            val totalLines = chapterParserService.calculateTotalLines(file)
            
            // 创建书籍对象
            val book = Book(
                id = UUID.randomUUID().toString(),
                title = title,
                filePath = file.absolutePath,
                file = file,
                totalChapters = chapters.size,
                totalLines = totalLines,
                currentChapterIndex = 0,
                currentChapterTitle = chapters.firstOrNull()?.title ?: "",
                currentChapterOriginalTitle = chapters.firstOrNull()?.originalTitle ?: "",
                lastReadTime = System.currentTimeMillis(),
                addTime = System.currentTimeMillis()
            )
            
            // 保存书籍
            dataStorageService.saveBook(book)
            
            Messages.showInfoMessage("书籍添加成功！共解析出 ${chapters.size} 章", "成功")
            super.doOKAction()
            
        } catch (e: Exception) {
            logger.error("添加书籍失败: $title", e)
            PopNotifyUtil.notify("添加失败", "添加书籍失败: ${e.message}")
            Messages.showErrorDialog("添加书籍失败: ${e.message}", "错误")
        }
    }
    
    /**
     * 检查文件是否为UTF-8编码
     */
    private fun isUtf8File(file: File): Boolean {
        return try {
            file.readText(Charsets.UTF_8)
            // 简单检查：尝试读取为UTF-8，如果成功则认为是UTF-8
            true
        } catch (e: Exception) {
            logger.warn("检查文件编码时发生异常: ${file.absolutePath}", e)
            false
        }
    }
    
    override fun getPreferredFocusedComponent(): JComponent = titleField
}