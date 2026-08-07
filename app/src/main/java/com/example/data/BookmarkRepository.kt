package com.example.data

import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun insert(bookmark: Bookmark) = bookmarkDao.insertBookmark(bookmark)

    suspend fun update(bookmark: Bookmark) = bookmarkDao.updateBookmark(bookmark)

    suspend fun deleteById(id: Int) = bookmarkDao.deleteBookmarkById(id)
}
