package com.neo.downloader.shared.util.category

interface ICategoryItemProvider {
    suspend fun getAll(): List<CategoryItemWithId>
}
