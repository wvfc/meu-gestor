package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.CategoryDao
import com.meugestor.app.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    fun getByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getByType(type)

    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getSubcategories(parentId)

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun insertAll(categories: List<CategoryEntity>) = categoryDao.insertAll(categories)

    suspend fun update(category: CategoryEntity) = categoryDao.update(category)

    suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
