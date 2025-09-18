package com.github.xucux.read.service

import com.github.xucux.read.constants.StorageConstants
import com.github.xucux.read.constants.TabConstants
import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.github.xucux.read.model.ReadingRecord
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import java.io.*
import java.util.*

/**
 * 数据存储服务
 * 使用Properties文件存储书架和阅读记录数据
 */
@Service
class DataStorageService {
    private val logger = logger<DataStorageService>()
    
    companion object {
        @JvmStatic fun getInstance(): DataStorageService {
            return ApplicationManager.getApplication().getService(DataStorageService::class.java)
        }
    }
    
    private val dataDir: File by lazy {
        val dir = File(System.getProperty("user.home"), StorageConstants.DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    private val bookshelfFile: File by lazy {
        File(dataDir, StorageConstants.BOOKSHELF_FILE)
    }
    
    private val readingRecordsFile: File by lazy {
        File(dataDir, StorageConstants.READING_RECORDS_FILE)
    }
    
    private val chapterCacheFile: File by lazy {
        File(dataDir, StorageConstants.CHAPTER_CACHE_FILE)
    }
    
    private val columnWidthsFile: File by lazy {
        File(dataDir, StorageConstants.COLUMN_WIDTHS_FILE)
    }
    
    /**
     * 保存书籍到书架
     */
    fun saveBook(book: Book) {
        try {
            val properties = loadProperties(bookshelfFile)
            val prefix = "${StorageConstants.BOOK_PREFIX}${book.id}."
            
            properties.setProperty("${prefix}title", book.title)
            properties.setProperty("${prefix}filePath", book.filePath)
            properties.setProperty("${prefix}totalChapters", book.totalChapters.toString())
            properties.setProperty("${prefix}totalLines", book.totalLines.toString())
            properties.setProperty("${prefix}currentChapterIndex", book.currentChapterIndex.toString())
            properties.setProperty("${prefix}currentChapterTitle", book.currentChapterTitle)
            properties.setProperty("${prefix}currentChapterOriginalTitle", book.currentChapterOriginalTitle)
            properties.setProperty("${prefix}lastReadTime", book.lastReadTime.toString())
            properties.setProperty("${prefix}addTime", book.addTime.toString())
            
            saveProperties(properties, bookshelfFile)
            logger.info("保存书籍成功: ${book.title}")
        } catch (e: Exception) {
            logger.error("保存书籍失败: ${book.title}", e)
        }
    }
    
    /**
     * 加载所有书籍
     */
    fun loadAllBooks(): List<Book> {
        val books = mutableListOf<Book>()
        try {
            val properties = loadProperties(bookshelfFile)
            val bookIds = properties.stringPropertyNames()
                .filter { it.startsWith(StorageConstants.BOOK_PREFIX) && it.endsWith(".title") }
                .map { it.substring(StorageConstants.BOOK_PREFIX.length, it.lastIndexOf(".")) }
            
            for (bookId in bookIds) {
                val prefix = "${StorageConstants.BOOK_PREFIX}$bookId."
                val title = properties.getProperty("${prefix}title") ?: continue
                val filePath = properties.getProperty("${prefix}filePath") ?: continue
                val file = File(filePath)
                
                if (!file.exists()) {
                    logger.warn("书籍文件不存在，跳过: $filePath")
                    continue
                }
                
                val book = Book(
                    id = bookId,
                    title = title,
                    filePath = filePath,
                    file = file,
                    totalChapters = properties.getProperty("${prefix}totalChapters")?.toIntOrNull() ?: 0,
                    totalLines = properties.getProperty("${prefix}totalLines")?.toIntOrNull() ?: 0,
                    currentChapterIndex = properties.getProperty("${prefix}currentChapterIndex")?.toIntOrNull() ?: 0,
                    currentChapterTitle = properties.getProperty("${prefix}currentChapterTitle") ?: "",
                    currentChapterOriginalTitle = properties.getProperty("${prefix}currentChapterOriginalTitle") ?: "",
                    lastReadTime = properties.getProperty("${prefix}lastReadTime")?.toLongOrNull() ?: System.currentTimeMillis(),
                    addTime = properties.getProperty("${prefix}addTime")?.toLongOrNull() ?: System.currentTimeMillis()
                )
                books.add(book)
            }
            
            // 按最后阅读时间排序
            books.sortByDescending { it.lastReadTime }
            logger.info("加载书籍成功，共${books.size}本")
        } catch (e: Exception) {
            logger.error("加载书籍失败", e)
        }
        return books
    }
    
    /**
     * 删除书籍
     */
    fun removeBook(bookId: String) {
        try {
            val properties = loadProperties(bookshelfFile)
            val prefix = "${StorageConstants.BOOK_PREFIX}$bookId."
            val keysToRemove = properties.stringPropertyNames()
                .filter { it.startsWith(prefix) }
            
            for (key in keysToRemove) {
                properties.remove(key)
            }
            
            saveProperties(properties, bookshelfFile)
            logger.info("删除书籍成功: $bookId")
        } catch (e: Exception) {
            logger.error("删除书籍失败: $bookId", e)
        }
    }
    
    /**
     * 保存阅读记录
     */
    fun saveReadingRecord(record: ReadingRecord) {
        try {
            val properties = loadProperties(readingRecordsFile)
            val key = "${StorageConstants.RECORD_PREFIX}${record.bookId}"
            
            properties.setProperty("$key.chapterIndex", record.chapterIndex.toString())
            properties.setProperty("$key.chapterTitle", record.chapterTitle)
            properties.setProperty("$key.chapterStartLineNumber", record.chapterStartLineNumber.toString())
            properties.setProperty("$key.chapterEndLineNumber", record.chapterEndLineNumber.toString())
            properties.setProperty("$key.totalLines", record.totalLines.toString())
            properties.setProperty("$key.bookReadLineNumber", record.bookReadLineNumber.toString())
            properties.setProperty("$key.readTime", record.readTime.toString())
            properties.setProperty("$key.scrollPosition", record.scrollPosition.toString())
            properties.setProperty("$key.lineNumber", record.lineNumber.toString())
            
            saveProperties(properties, readingRecordsFile)
            logger.info("保存阅读记录成功: ${record.bookId}")
        } catch (e: Exception) {
            logger.error("保存阅读记录失败: ${record.bookId}", e)
        }
    }
    
    /**
     * 加载阅读记录
     */
    fun loadReadingRecord(bookId: String): ReadingRecord? {
        try {
            val properties = loadProperties(readingRecordsFile)
            val key = "${StorageConstants.RECORD_PREFIX}$bookId"
            
            val chapterIndex = properties.getProperty("$key.chapterIndex")?.toIntOrNull() ?: return null
            val chapterTitle = properties.getProperty("$key.chapterTitle") ?: return null
            val chapterStartLineNumber = properties.getProperty("$key.chapterStartLineNumber")?.toLongOrNull() ?: 0L
            val chapterEndLineNumber = properties.getProperty("$key.chapterEndLineNumber")?.toLongOrNull() ?: 0L
            val totalLines = properties.getProperty("$key.totalLines")?.toIntOrNull() ?: 0
            val bookReadLineNumber = properties.getProperty("$key.bookReadLineNumber")?.toIntOrNull() ?: 0
            val readTime = properties.getProperty("$key.readTime")?.toLongOrNull() ?: return null
            val scrollPosition = properties.getProperty("$key.scrollPosition")?.toIntOrNull() ?: 0
            val lineNumber = properties.getProperty("$key.lineNumber")?.toIntOrNull() ?: 0
            
            return ReadingRecord(
                bookId = bookId,
                chapterIndex = chapterIndex,
                chapterTitle = chapterTitle,
                chapterStartLineNumber = chapterStartLineNumber,
                chapterEndLineNumber = chapterEndLineNumber,
                totalLines = totalLines,
                bookReadLineNumber = bookReadLineNumber,
                readTime = readTime,
                scrollPosition = scrollPosition,
                lineNumber = lineNumber
            )
        } catch (e: Exception) {
            logger.error("加载阅读记录失败: $bookId", e)
            return null
        }
    }
    
    /**
     * 删除阅读记录
     */
    fun removeReadingRecord(bookId: String) {
        try {
            val properties = loadProperties(readingRecordsFile)
            val key = "${StorageConstants.RECORD_PREFIX}$bookId"
            val keysToRemove = properties.stringPropertyNames()
                .filter { it.startsWith(key) }
            
            for (keyToRemove in keysToRemove) {
                properties.remove(keyToRemove)
            }
            
            saveProperties(properties, readingRecordsFile)
            logger.info("删除阅读记录成功: $bookId")
        } catch (e: Exception) {
            logger.error("删除阅读记录失败: $bookId", e)
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
            properties.store(output, "${TabConstants.HELLO_READ_TOOL_WINDOW_ID} Data - ${Date()}")
        }
    }
    
    /**
     * 保存章节缓存
     */
    fun saveChapterCache(bookId: String, chapters: List<Chapter>) {
        try {
            val properties = loadProperties(chapterCacheFile)
            val prefix = "${StorageConstants.CHAPTERS_PREFIX}$bookId."
            
            // 清除旧的章节缓存
            val keysToRemove = properties.stringPropertyNames()
                .filter { it.startsWith(prefix) }
            for (key in keysToRemove) {
                properties.remove(key)
            }
            
            // 保存新的章节缓存
            properties.setProperty("${prefix}${StorageConstants.CHAPTER_COUNT_KEY}", chapters.size.toString())
            properties.setProperty("${prefix}${StorageConstants.CHAPTER_CACHED_TIME_KEY}", System.currentTimeMillis().toString())
            
            for ((index, chapter) in chapters.withIndex()) {
                val chapterPrefix = "${prefix}${StorageConstants.CHAPTER_PREFIX}$index."
                properties.setProperty("${chapterPrefix}title", chapter.title)
                properties.setProperty("${chapterPrefix}originalTitle", chapter.originalTitle)
                properties.setProperty("${chapterPrefix}chapterNumber", chapter.chapterNumber)
                properties.setProperty("${chapterPrefix}startPosition", chapter.startPosition.toString())
                properties.setProperty("${chapterPrefix}endPosition", chapter.endPosition.toString())
                // 注意：不缓存章节内容，因为内容可能很大，只在需要时从文件读取
            }
            
            saveProperties(properties, chapterCacheFile)
            logger.info("保存章节缓存成功: $bookId, 共${chapters.size}章")
        } catch (e: Exception) {
            logger.error("保存章节缓存失败: $bookId", e)
        }
    }
    
    /**
     * 加载章节缓存
     */
    fun loadChapterCache(bookId: String): List<Chapter>? {
        try {
            val properties = loadProperties(chapterCacheFile)
            val prefix = "${StorageConstants.CHAPTERS_PREFIX}$bookId."
            
            val count = properties.getProperty("${prefix}${StorageConstants.CHAPTER_COUNT_KEY}")?.toIntOrNull() ?: return null
            val cachedTime = properties.getProperty("${prefix}${StorageConstants.CHAPTER_CACHED_TIME_KEY}")?.toLongOrNull() ?: return null
            
            // 检查缓存是否过期（7天）
            val cacheExpireTime = StorageConstants.CACHE_EXPIRE_TIME
            if (System.currentTimeMillis() - cachedTime > cacheExpireTime) {
                logger.info("章节缓存已过期: $bookId")
                return null
            }
            
            val chapters = mutableListOf<Chapter>()
            for (i in 0 until count) {
                val chapterPrefix = "${prefix}${StorageConstants.CHAPTER_PREFIX}$i."
                val title = properties.getProperty("${chapterPrefix}title") ?: continue
                val originalTitle = properties.getProperty("${chapterPrefix}originalTitle") ?: title
                val chapterNumber = properties.getProperty("${chapterPrefix}chapterNumber") ?: ""
                val startPosition = properties.getProperty("${chapterPrefix}startPosition")?.toLongOrNull() ?: continue
                val endPosition = properties.getProperty("${chapterPrefix}endPosition")?.toLongOrNull() ?: continue
                
                val chapter = Chapter(
                    index = i,
                    title = title,
                    content = "", // 内容不缓存，需要时从文件读取
                    startPosition = startPosition,
                    endPosition = endPosition,
                    originalTitle = originalTitle,
                    chapterNumber = chapterNumber
                )
                chapters.add(chapter)
            }
            
            if (chapters.size == count) {
                logger.info("加载章节缓存成功: $bookId, 共${chapters.size}章")
                return chapters
            } else {
                logger.warn("章节缓存数据不完整: $bookId, 期望${count}章，实际${chapters.size}章")
                return null
            }
        } catch (e: Exception) {
            logger.error("加载章节缓存失败: $bookId", e)
            return null
        }
    }
    
    /**
     * 删除章节缓存
     */
    fun removeChapterCache(bookId: String) {
        try {
            val properties = loadProperties(chapterCacheFile)
            val prefix = "${StorageConstants.CHAPTERS_PREFIX}$bookId."
            val keysToRemove = properties.stringPropertyNames()
                .filter { it.startsWith(prefix) }
            
            for (key in keysToRemove) {
                properties.remove(key)
            }
            
            saveProperties(properties, chapterCacheFile)
            logger.info("删除章节缓存成功: $bookId")
        } catch (e: Exception) {
            logger.error("删除章节缓存失败: $bookId", e)
        }
    }
    
    /**
     * 检查章节缓存是否存在且有效
     */
    fun hasValidChapterCache(bookId: String): Boolean {
        return loadChapterCache(bookId) != null
    }
    
    /**
     * 保存列宽设置
     */
    fun saveColumnWidths(columnWidths: Map<String, Int>) {
        try {
            val properties = loadProperties(columnWidthsFile)
            
            // 清除旧的列宽设置
            val keysToRemove = properties.stringPropertyNames()
                .filter { it.startsWith(StorageConstants.COLUMN_PREFIX) }
            for (key in keysToRemove) {
                properties.remove(key)
            }
            
            // 保存新的列宽设置
            for ((columnName, width) in columnWidths) {
                properties.setProperty("${StorageConstants.COLUMN_PREFIX}$columnName", width.toString())
            }
            
            saveProperties(properties, columnWidthsFile)
            logger.info("保存列宽设置成功")
        } catch (e: Exception) {
            logger.error("保存列宽设置失败", e)
        }
    }
    
    /**
     * 加载列宽设置
     */
    fun loadColumnWidths(): Map<String, Int> {
        val columnWidths = mutableMapOf<String, Int>()
        try {
            val properties = loadProperties(columnWidthsFile)
            
            for (key in properties.stringPropertyNames()) {
                if (key.startsWith(StorageConstants.COLUMN_PREFIX)) {
                    val columnName = key.substring(StorageConstants.COLUMN_PREFIX.length) // 移除列前缀
                    val width = properties.getProperty(key)?.toIntOrNull()
                    if (width != null) {
                        columnWidths[columnName] = width
                    }
                }
            }
            
            logger.info("加载列宽设置成功，共${columnWidths.size}列")
        } catch (e: Exception) {
            logger.error("加载列宽设置失败", e)
        }
        return columnWidths
    }

}
