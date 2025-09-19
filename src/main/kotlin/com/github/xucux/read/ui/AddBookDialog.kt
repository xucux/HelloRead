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
import org.mozilla.universalchardet.UniversalDetector
import java.awt.Dimension
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.BoxLayout
import javax.swing.JButton

/**
 * 添加书籍对话框
 */
class AddBookDialog(project: Project) : DialogWrapper(project) {
    
    companion object {
        private val logger = logger<AddBookDialog>()
    }
    
    private val dataStorageService = DataStorageService.getInstance()
    private val chapterParserService = ChapterParserService.getInstance()
    private val descField = JBLabel("请选择【UTF-8】编码的txt文件")
    private val titleField = JBTextField()
    private val filePathField = JBTextField()
    private val filePathLabel = JBLabel("文件路径:")
    private val titleLabel = JBLabel("书籍标题:")
    private val convertButton = JButton("转换为UTF-8")
    
    private var selectedFile: File? = null
    
    init {
        title = "添加新书籍"
        init()

        titleField.toolTipText = "选择文件后自动填写"
        // 设置文件路径字段为只读
        filePathField.isEditable = false
        filePathField.toolTipText = "双击选择文件"
        
        // 添加文件选择按钮
        setupFileChooser()
        
        // 设置转换按钮
        setupConvertButton()
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
            .withDescription("请优先选择utf-8编码的txt文件")
        
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
            
            // 更新转换按钮状态
            updateConvertButtonState()
        }
    }
    
    /**
     * 设置转换按钮
     */
    private fun setupConvertButton() {
        convertButton.addActionListener {
            convertFileToUtf8()
        }
        convertButton.isEnabled = false
    }
    
    /**
     * 更新转换按钮状态
     */
    private fun updateConvertButtonState() {
        val file = selectedFile
        if (file != null && file.exists() && file.extension.toLowerCase(Locale.getDefault()) == "txt") {
            // 检查文件是否已经是UTF-8编码
            val fileEncoding = detectFileEncoding(file)
            convertButton.isEnabled = !isUtf8File(file)
            convertButton.text = if (isUtf8File(file)) "已是UTF-8编码" else "${fileEncoding}转换为UTF-8"
        } else {
            convertButton.isEnabled = false
            convertButton.text = "转换为UTF-8"
        }
    }
    
    /**
     * 转换文件为UTF-8编码
     */
    private fun convertFileToUtf8() {
        val file = selectedFile ?: return
        
        try {
            // 检测文件编码
            val charset = Charset.forName(detectFileEncoding(file))
            if (charset == Charsets.UTF_8) {
                PopNotifyUtil.notify("转换完成", "文件已经是UTF-8编码")
                updateConvertButtonState()
                return
            }
            
            // 读取原文件内容
            val content = file.readText(charset)
            
            // 创建临时文件
            val tempFile = File.createTempFile("temp_utf8_", ".txt")
            tempFile.writeText(content, Charsets.UTF_8)
            
            // 备份原文件
            val backupFile = File(file.parent, "${file.nameWithoutExtension}_backup_${System.currentTimeMillis()}.txt")
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            
            // 用UTF-8内容覆盖原文件
            Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            
            // 删除临时文件
            tempFile.delete()
            
            PopNotifyUtil.notify("转换成功", "文件已转换为UTF-8编码，原文件已备份为: ${backupFile.name}")
            Messages.showInfoMessage("文件转换成功！\n原编码: ${charset.name()}\n备份文件: ${backupFile.name}", "转换成功")
            
            // 更新按钮状态
            updateConvertButtonState()
            
        } catch (e: Exception) {
            logger.error("转换文件编码失败: ${file.absolutePath}", e)
            PopNotifyUtil.notify("转换失败", "转换文件编码失败: ${e.message}")
            Messages.showErrorDialog("转换文件编码失败: ${e.message}", "错误")
        }
    }

    
    override fun createCenterPanel(): JComponent {
        // 创建文件路径和转换按钮的水平布局
        val filePathPanel = JPanel()
        filePathPanel.layout = BoxLayout(filePathPanel, BoxLayout.X_AXIS)
        filePathPanel.add(filePathField)

        val panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("备注:",descField)
            .addLabeledComponent(titleLabel, titleField, 1, false)
            .addLabeledComponent(filePathLabel, filePathPanel, 1, false)
            .addLabeledComponent(JBLabel("转换:"), convertButton, false)
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
            
            // 解析作者信息
            val author = chapterParserService.parseAuthor(file)
            
            // 创建书籍对象
            val book = Book(
                id = UUID.randomUUID().toString(),
                title = title,
                author = author,
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
        return detectFileEncoding( file) == Charsets.UTF_8.toString()
//        return try {
//            file.readText(Charsets.UTF_8)
//            // 简单检查：尝试读取为UTF-8，如果成功则认为是UTF-8
//            true
//        } catch (e: Exception) {
//            logger.warn("检查文件编码时发生异常: ${file.absolutePath}", e)
//            false
//        }
    }

    /**
     * 检测txt文件编码，优先BOM检测，若无BOM则用juniversalchardet检测，最多仅读取前1kb
     */
    private fun detectFileEncoding(file: File): String {
        val maxBytes = 1024
        val buffer = ByteArray(maxBytes)
        var readLen = 0
        file.inputStream().use { input ->
            readLen = input.read(buffer)
        }
        if (readLen <= 0) return "UTF-8" // 默认

        // BOM检测
        if (readLen >= 3 && buffer[0] == 0xEF.toByte() && buffer[1] == 0xBB.toByte() && buffer[2] == 0xBF.toByte()) {
            return "UTF-8"
        }
        if (readLen >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xFE.toByte()) {
            return "UTF-16LE"
        }
        if (readLen >= 2 && buffer[0] == 0xFE.toByte() && buffer[1] == 0xFF.toByte()) {
            return "UTF-16BE"
        }

        // juniversalchardet 检测
        try {
            val detector = UniversalDetector(null)
            detector.handleData(buffer, 0, readLen)
            detector.dataEnd()
            val encoding = detector.detectedCharset
            detector.reset()
            return encoding ?: "UTF-8"
        } catch (e: Exception) {
            logger.warn("自动检测文件编码失败: ${file.absolutePath}", e)
            PopNotifyUtil.notify("HelloRead", "自动检测文件编码失败: ${e.message}")
            return "UTF-8"
        }
    }
    
    
    override fun getPreferredFocusedComponent(): JComponent = titleField
}