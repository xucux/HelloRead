package com.github.xucux.read.service

import com.github.xucux.read.model.Chapter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * 章节解析服务
 * 专门适配中文网络小说格式
 */
@Service
class ChapterParserService {
    private val logger = logger<ChapterParserService>()
    private val dataStorageService = com.github.xucux.read.service.DataStorageService.getInstance()
    
    companion object {

        @JvmStatic fun getInstance(): ChapterParserService {
            return ApplicationManager.getApplication().getService(ChapterParserService::class.java)
        }

        // 中文网络小说常见的章节标题模式
        @JvmStatic private val CHAPTER_PATTERNS = listOf(
            // 第X章 标题
            Pattern.compile("第[一二三四五六七八九十百千万\\d]+章\\s*[：:]?\\s*(.+)"),
            // 第X节 标题
            Pattern.compile("第[一二三四五六七八九十百千万\\d]+节\\s*[：:]?\\s*(.+)"),
            // 第X回 标题
            Pattern.compile("第[一二三四五六七八九十百千万\\d]+回\\s*[：:]?\\s*(.+)"),
            // 第X卷 标题
            Pattern.compile("第[一二三四五六七八九十百千万\\d]+卷\\s*[：:]?\\s*(.+)"),
            // 第X集 标题
            Pattern.compile("第[一二三四五六七八九十百千万\\d]+集\\s*[：:]?\\s*(.+)"),
            // 第X部分 标题
            Pattern.compile("第[一二三四五六七八九十百千万\\d]+部分\\s*[：:]?\\s*(.+)"),
            // 纯数字章节
//            Pattern.compile("(\\d+)\\s*[、.]\\s*(.+)"),
//            Pattern.compile("^\\s*(\\d+)\\..(.+)$"),
            // 中文数字章节
//            Pattern.compile("([一二三四五六七八九十百千万]+)\\s*[、.]\\s*(.+)"),
            // 简单标题（没有章节号）
//            Pattern.compile("^(.+)$")
        )
        
        // 需要跳过的行模式
        @JvmStatic private val SKIP_PATTERNS = listOf(
            Pattern.compile("^\\s*$"),                    // 空行
            Pattern.compile("^\\s*[\\-\\*\\+]{3,}\\s*$"), // 分隔线
            Pattern.compile("^\\s*作者[:：]"),             // 作者信息
            Pattern.compile("^\\s*书名[:：]"),             // 书名信息
            Pattern.compile("^\\s*简介[:：]"),             // 简介信息
            Pattern.compile("^\\s*目录[:：]"),             // 目录信息
            Pattern.compile("^\\s*[\\d\\-\\s]{8,}$"),     // 日期格式
            Pattern.compile("^\\s*[\\u4e00-\\u9fa5]{1,3}[:：]\\s*$") // 简单标签
        )
    }
    
    /**
     * 解析文件中的所有章节（带缓存）
     */
    fun parseChapters(file: File, bookId: String? = null): List<Chapter> {
        // 如果有bookId，先尝试从缓存加载
        if (bookId != null) {
            val cachedChapters = dataStorageService.loadChapterCache(bookId)
            if (cachedChapters != null) {
                logger.info("从缓存加载章节: ${file.name}, 共${cachedChapters.size}章")
                return cachedChapters
            }
        }
        
        // 缓存不存在或无效，重新解析
        val chapters = parseChaptersFromFile(file)
        
        // 如果解析成功且有bookId，保存到缓存
        if (chapters.isNotEmpty() && bookId != null) {
            dataStorageService.saveChapterCache(bookId, chapters)
        }
        
        return chapters
    }
    
    /**
     * 从文件解析章节（原始解析逻辑）
     */
    private fun parseChaptersFromFile(file: File): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        
        try {
            if (!file.exists() || !file.isFile) {
                logger.warn("文件不存在或不是文件: ${file.absolutePath}")
                return chapters
            }
            
            val content = file.readText(StandardCharsets.UTF_8)
            if (content.isEmpty()) {
                logger.warn("文件内容为空: ${file.absolutePath}")
                return chapters
            }
            
            val lines = content.split("\n")
            var currentChapter: ChapterBuilder? = null
            var chapterIndex = 0
            
            for ((lineIndex, line) in lines.withIndex()) {
                val trimmedLine = line.trim()
                
                // 跳过空行和不需要的行
                if (shouldSkipLine(trimmedLine)) {
                    continue
                }
                
                // 检查是否是章节标题
                val chapterTitleInfo = extractChapterTitle(trimmedLine)
                if (chapterTitleInfo != null) {
                    // 保存上一个章节
                    currentChapter?.let { builder ->
                        chapters.add(builder.build())
                    }
                    
                    // 开始新章节
                    currentChapter = ChapterBuilder(
                        index = chapterIndex++,
                        title = chapterTitleInfo.title,
                        originalTitle = chapterTitleInfo.originalTitle,
                        chapterNumber = chapterTitleInfo.chapterNumber,
                        startLine = lineIndex
                    )
                } else {
                    // 添加到当前章节内容
                    currentChapter?.addContent(line)
                }
            }
            
            // 保存最后一个章节
            currentChapter?.let { builder ->
                chapters.add(builder.build())
            }
            
            logger.info("解析章节完成: ${file.name}, 共${chapters.size}章")
            
        } catch (e: Exception) {
            logger.error("解析章节失败: ${file.absolutePath}", e)
        }
        
        return chapters
    }
    
    /**
     * 提取章节标题信息
     */
    private fun extractChapterTitle(line: String): ChapterTitleInfo? {
        for (pattern in CHAPTER_PATTERNS) {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                val title = if (matcher.groupCount() >= 2) {
                    matcher.group(2)?.trim()
                } else {
                    matcher.group(1)?.trim()
                }
                
                if (!title.isNullOrEmpty() && title.length > 1 && title.length < 100) {
                    // 提取章节序列号
                    val chapterNumber = extractChapterNumber(line)
                    return ChapterTitleInfo(
                        title = title,
                        originalTitle = line.trim(),
                        chapterNumber = chapterNumber
                    )
                }
            }
        }
        return null
    }
    
    /**
     * 提取章节序列号
     */
    private fun extractChapterNumber(line: String): String {
        // 匹配各种章节号格式
        val patterns = listOf(
            "第([一二三四五六七八九十百千万\\d]+)章",
            "第([一二三四五六七八九十百千万\\d]+)节", 
            "第([一二三四五六七八九十百千万\\d]+)回",
            "第([一二三四五六七八九十百千万\\d]+)卷",
            "第([一二三四五六七八九十百千万\\d]+)集",
            "第([一二三四五六七八九十百千万\\d]+)部分"
        )
        
        for (patternStr in patterns) {
            val pattern = Pattern.compile(patternStr)
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                val numberStr = matcher.group(1)
                if (numberStr != null) {
                    // 将中文数字转换为阿拉伯数字
                    val arabicNumber = convertChineseNumberToArabic(numberStr)
                    return "第${arabicNumber}章"
                }
            }
        }
        
        return ""
    }
    
    /**
     * 将中文数字转换为阿拉伯数字
     */
    private fun convertChineseNumberToArabic(chineseNumber: String): String {
        val chineseToArabic = mapOf(
            "一" to "1", "二" to "2", "三" to "3", "四" to "4", "五" to "5",
            "六" to "6", "七" to "7", "八" to "8", "九" to "9", "十" to "10",
            "百" to "100", "千" to "1000", "万" to "10000"
        )
        
        // 如果是纯阿拉伯数字，直接返回
        if (chineseNumber.matches("\\d+".toRegex())) {
            return chineseNumber
        }
        
        // 简单的中文数字转换（可以后续扩展更复杂的逻辑）
        var result = chineseNumber
        for ((chinese, arabic) in chineseToArabic) {
            result = result.replace(chinese, arabic)
        }
        
        // 处理简单的组合，如"三十五" -> "35"
        if (result.contains("10") && result.length > 2) {
            result = result.replace("10", "")
        }
        
        return result
    }
    
    /**
     * 章节标题信息数据类
     */
    private data class ChapterTitleInfo(
        val title: String,        // 提取的标题
        val originalTitle: String, // 原始章节名称
        val chapterNumber: String  // 章节序列号
    )
    
    /**
     * 判断是否应该跳过该行
     */
    private fun shouldSkipLine(line: String): Boolean {
        if (line.isEmpty()) return true
        
        for (pattern in SKIP_PATTERNS) {
            if (pattern.matcher(line).matches()) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 章节构建器
     */
    private class ChapterBuilder(
        val index: Int,
        val title: String,
        val originalTitle: String,
        val chapterNumber: String,
        val startLine: Int
    ) {
        private val contentLines = mutableListOf<String>()
        
        fun addContent(line: String) {
            contentLines.add(line)
        }
        
        fun build(): Chapter {
            val content = contentLines.joinToString("\n")
            return Chapter(
                index = index,
                title = title,
                content = content,
                startPosition = startLine.toLong(),
                endPosition = (startLine + contentLines.size).toLong(),
                originalTitle = originalTitle,
                chapterNumber = chapterNumber
            )
        }
    }
    
    /**
     * 获取章节内容（用于阅读器显示）
     */
    fun getChapterContent(file: File, chapter: Chapter): String {
        try {
            val content = file.readText(StandardCharsets.UTF_8)
            val lines = content.split("\n")
            
            val startLine = chapter.startPosition.toInt()
            val endLine = chapter.endPosition.toInt()
            
            if (startLine >= 0 && endLine <= lines.size && startLine < endLine) {
                return lines.subList(startLine, endLine).joinToString("\n")
            }
            
            return chapter.content
        } catch (e: Exception) {
            logger.error("获取章节内容失败", e)
            return chapter.content
        }
    }
    
    /**
     * 格式化章节内容（添加段落间距等）
     */
    fun formatChapterContent(content: String): String {
        return content
            .replace("\n\n+".toRegex(), "\n\n") // 合并多个空行
            .replace("([。！？])\\s*\n".toRegex(), "$1\n\n") // 在句号后添加空行
            .trim()
    }
    
    /**
     * 强制重新解析章节（清除缓存）
     */
    fun reparseChapters(file: File, bookId: String): List<Chapter> {
        // 清除缓存
        dataStorageService.removeChapterCache(bookId)
        
        // 重新解析
        val chapters = parseChaptersFromFile(file)
        
        // 保存到缓存
        if (chapters.isNotEmpty()) {
            dataStorageService.saveChapterCache(bookId, chapters)
        }
        
        return chapters
    }
    
    /**
     * 检查是否有章节缓存
     */
    fun hasChapterCache(bookId: String): Boolean {
        return dataStorageService.hasValidChapterCache(bookId)
    }
    
    /**
     * 计算文件的总行数
     */
    fun calculateTotalLines(file: File): Int {
        return try {
            if (!file.exists() || !file.isFile) {
                logger.warn("文件不存在或不是文件: ${file.absolutePath}")
                return 0
            }
            
            val content = file.readText(StandardCharsets.UTF_8)
            if (content.isEmpty()) {
                logger.warn("文件内容为空: ${file.absolutePath}")
                return 0
            }
            
            val lines = content.split("\n")
            logger.info("计算文件总行数: ${file.name}, 共${lines.size}行")
            lines.size
        } catch (e: Exception) {
            logger.error("计算文件总行数失败: ${file.absolutePath}", e)
            0
        }
    }

}
