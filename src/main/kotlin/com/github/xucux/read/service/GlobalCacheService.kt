package com.github.xucux.read.service

import com.github.xucux.read.model.Book
import com.github.xucux.read.model.Chapter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局统一内存缓存服务
 * 用于缓存当前正在阅读的书籍信息，确保书架页面、阅读页面和章节页面的数据同步
 */
@Service
class GlobalCacheService {
    private val logger = logger<GlobalCacheService>()
    
    companion object {
        @JvmStatic fun getInstance(): GlobalCacheService {
            return ApplicationManager.getApplication().getService(GlobalCacheService::class.java)
        }
    }
    
    // 当前正在阅读的书籍信息
    private var currentReadingBook: Book? = null
    
    // 当前书籍的章节列表缓存
    private var currentBookChapters: List<Chapter> = emptyList()
    
    // 书籍章节缓存（按bookId缓存）
    private val bookChaptersCache = ConcurrentHashMap<String, List<Chapter>>()
    
    // 书籍信息缓存（按bookId缓存）
    private val bookInfoCache = ConcurrentHashMap<String, Book>()
    
    // 缓存监听器列表
    private val cacheListeners = mutableListOf<CacheListener>()
    
    /**
     * 缓存监听器接口
     */
    interface CacheListener {
        /**
         * 当前阅读书籍发生变化时调用
         * @param oldBook 之前的书籍（可能为null）
         * @param newBook 新的书籍（可能为null）
         */
        fun onCurrentReadingBookChanged(oldBook: Book?, newBook: Book?)
        
        /**
         * 书籍章节数据发生变化时调用
         * @param bookId 书籍ID
         * @param chapters 新的章节列表
         */
        fun onBookChaptersChanged(bookId: String, chapters: List<Chapter>)
        
        /**
         * 书籍信息发生变化时调用
         * @param book 更新后的书籍信息
         */
        fun onBookInfoChanged(book: Book)
    }
    
    /**
     * 设置当前正在阅读的书籍
     */
    fun setCurrentReadingBook(book: Book?) {
        val oldBook = currentReadingBook
        currentReadingBook = book
        
        // 如果设置了新书籍，同时缓存其章节信息
        if (book != null) {
            // 从章节解析服务获取章节信息
            val chapterParserService = ChapterParserService.getInstance()
            val chapters = chapterParserService.parseChapters(book.file, book.id)
            setBookChapters(book.id, chapters)
        }
        
        // 通知监听器
        notifyCurrentReadingBookChanged(oldBook, book)
        
        logger.info("设置当前阅读书籍: ${book?.title ?: "无"}")
    }
    
    /**
     * 获取当前正在阅读的书籍
     */
    fun getCurrentReadingBook(): Book? = currentReadingBook
    
    /**
     * 设置书籍的章节列表
     */
    fun setBookChapters(bookId: String, chapters: List<Chapter>) {
        bookChaptersCache[bookId] = chapters
        
        // 如果是当前阅读的书籍，同时更新当前书籍章节
        if (currentReadingBook?.id == bookId) {
            currentBookChapters = chapters
        }
        
        // 通知监听器
        notifyBookChaptersChanged(bookId, chapters)
        
        logger.info("缓存书籍章节: $bookId, 共${chapters.size}章")
    }
    
    /**
     * 获取书籍的章节列表
     */
    fun getBookChapters(bookId: String): List<Chapter>? = bookChaptersCache[bookId]
    
    /**
     * 获取当前阅读书籍的章节列表
     */
    fun getCurrentBookChapters(): List<Chapter> = currentBookChapters
    
    /**
     * 缓存书籍信息
     */
    fun cacheBookInfo(book: Book) {
        bookInfoCache[book.id] = book
        
        // 如果是当前阅读的书籍，同时更新当前书籍信息
        if (currentReadingBook?.id == book.id) {
            currentReadingBook = book
        }
        
        // 通知监听器
        notifyBookInfoChanged(book)
        
        logger.info("缓存书籍信息: ${book.title}")
    }
    
    /**
     * 获取缓存的书籍信息
     */
    fun getCachedBookInfo(bookId: String): Book? = bookInfoCache[bookId]
    
    /**
     * 更新当前阅读书籍的章节索引
     */
    fun updateCurrentBookChapterIndex(chapterIndex: Int, chapterTitle: String, originalTitle: String = "") {
        val book = currentReadingBook ?: return
        
        val updatedBook = book.copy(
            currentChapterIndex = chapterIndex,
            currentChapterTitle = chapterTitle,
            currentChapterOriginalTitle = originalTitle,
            lastReadTime = System.currentTimeMillis()
        )
        
        // 更新缓存
        currentReadingBook = updatedBook
        bookInfoCache[book.id] = updatedBook
        
        // 通知监听器
        notifyBookInfoChanged(updatedBook)
        
        logger.info("更新当前书籍章节索引: ${book.title} -> 第${chapterIndex + 1}章 $chapterTitle")
    }
    
    /**
     * 清除指定书籍的缓存
     */
    fun clearBookCache(bookId: String) {
        bookChaptersCache.remove(bookId)
        bookInfoCache.remove(bookId)
        
        // 如果是当前阅读的书籍，清除当前书籍信息
        if (currentReadingBook?.id == bookId) {
            currentReadingBook = null
            currentBookChapters = emptyList()
        }
        
        logger.info("清除书籍缓存: $bookId")
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        currentReadingBook = null
        currentBookChapters = emptyList()
        bookChaptersCache.clear()
        bookInfoCache.clear()
        
        logger.info("清除所有缓存")
    }
    
    /**
     * 添加缓存监听器
     */
    fun addCacheListener(listener: CacheListener) {
        cacheListeners.add(listener)
    }
    
    /**
     * 移除缓存监听器
     */
    fun removeCacheListener(listener: CacheListener) {
        cacheListeners.remove(listener)
    }
    
    /**
     * 通知当前阅读书籍发生变化
     */
    private fun notifyCurrentReadingBookChanged(oldBook: Book?, newBook: Book?) {
        cacheListeners.forEach { listener ->
            try {
                listener.onCurrentReadingBookChanged(oldBook, newBook)
            } catch (e: Exception) {
                logger.error("通知当前阅读书籍变化失败", e)
            }
        }
    }
    
    /**
     * 通知书籍章节发生变化
     */
    private fun notifyBookChaptersChanged(bookId: String, chapters: List<Chapter>) {
        cacheListeners.forEach { listener ->
            try {
                listener.onBookChaptersChanged(bookId, chapters)
            } catch (e: Exception) {
                logger.error("通知书籍章节变化失败: $bookId", e)
            }
        }
    }
    
    /**
     * 通知书籍信息发生变化
     */
    private fun notifyBookInfoChanged(book: Book) {
        cacheListeners.forEach { listener ->
            try {
                listener.onBookInfoChanged(book)
            } catch (e: Exception) {
                logger.error("通知书籍信息变化失败: ${book.title}", e)
            }
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): String {
        return "当前阅读书籍: ${currentReadingBook?.title ?: "无"}, " +
               "书籍信息缓存: ${bookInfoCache.size}本, " +
               "章节缓存: ${bookChaptersCache.size}本, " +
               "监听器: ${cacheListeners.size}个"
    }
}
