package com.neo.downloader.shared.util.category

interface CategoryStorage {
    suspend fun setCategories(categories: List<Category>)
    suspend fun getCategories(): List<Category>
    suspend fun isCategoriesSet(): Boolean
}
