package com.meugestor.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meugestor.app.data.database.converter.Converters
import com.meugestor.app.data.database.dao.*
import com.meugestor.app.data.database.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CreditCardEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        GoalEntity::class,
        AttachmentEntity::class,
        TaxDocumentEntity::class,
        TaxChecklistItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun taxDocumentDao(): TaxDocumentDao
    abstract fun taxChecklistDao(): TaxChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "meu_gestor.db"
            )
                .addCallback(PrepopulateCallback())
                .build()
        }

        private class PrepopulateCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateCategories(database.categoryDao())
                    }
                }
            }
        }

        private suspend fun prepopulateCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                // Income categories
                CategoryEntity(name = "Salario", icon = "ic_salary", color = "#4CAF50", type = CategoryType.INCOME, isDefault = true),
                CategoryEntity(name = "Freelance", icon = "ic_freelance", color = "#66BB6A", type = CategoryType.INCOME, isDefault = true),
                CategoryEntity(name = "Investimentos", icon = "ic_investment", color = "#43A047", type = CategoryType.INCOME, isDefault = true),
                CategoryEntity(name = "Outros Recebimentos", icon = "ic_other_income", color = "#388E3C", type = CategoryType.INCOME, isDefault = true),

                // Expense categories
                CategoryEntity(name = "Alimentacao", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Moradia", icon = "ic_home", color = "#E91E63", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Transporte", icon = "ic_transport", color = "#9C27B0", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Saude", icon = "ic_health", color = "#F44336", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Educacao", icon = "ic_education", color = "#3F51B5", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Lazer", icon = "ic_leisure", color = "#FF9800", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Vestuario", icon = "ic_clothing", color = "#795548", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Assinaturas", icon = "ic_subscriptions", color = "#607D8B", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Impostos", icon = "ic_taxes", color = "#B71C1C", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Seguros", icon = "ic_insurance", color = "#1565C0", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Pets", icon = "ic_pets", color = "#8D6E63", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Presentes", icon = "ic_gifts", color = "#D81B60", type = CategoryType.EXPENSE, isDefault = true),
                CategoryEntity(name = "Outras Despesas", icon = "ic_other_expense", color = "#757575", type = CategoryType.EXPENSE, isDefault = true)
            )
            categoryDao.insertAll(defaultCategories)
        }
    }
}
