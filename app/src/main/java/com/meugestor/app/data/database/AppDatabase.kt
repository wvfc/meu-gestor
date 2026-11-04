package com.meugestor.app.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meugestor.app.data.database.converter.Converters
import com.meugestor.app.data.database.dao.*
import com.meugestor.app.data.database.entity.*
import com.meugestor.app.util.DateUtils

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
    version = 2,
    exportSchema = false
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
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .build()
        }

        /**
         * v1 -> v2: adiciona `balance_applied`.
         * Na v1 todo lançamento fora do cartão alterava o saldo no momento da criação
         * (independente do status) e compras no cartão nunca alteravam. A migração registra
         * exatamente isso para que as próximas edições/exclusões revertam o valor correto.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN balance_applied INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE transactions SET balance_applied = 1 WHERE credit_card_id IS NULL")

                ACCENTED_CATEGORY_NAMES.forEach { (oldName, newName) ->
                    db.execSQL(
                        "UPDATE categories SET name = ? WHERE name = ? AND is_default = 1",
                        arrayOf<Any?>(newName, oldName)
                    )
                }
            }
        }

        /** Nomes das categorias padrão da v1, que foram gravados sem acentuação. */
        private val ACCENTED_CATEGORY_NAMES = listOf(
            "Salario" to "Salário",
            "Alimentacao" to "Alimentação",
            "Saude" to "Saúde",
            "Educacao" to "Educação",
            "Vestuario" to "Vestuário"
        )

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                insertDefaultAccount(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Antes as categorias eram inseridas de forma assíncrona e podiam não existir
                // na primeira abertura. Agora a inserção é síncrona e idempotente.
                ensureDefaultCategories(db)
            }
        }

        private fun insertDefaultAccount(db: SupportSQLiteDatabase) {
            val values = ContentValues().apply {
                put("name", "Conta Principal")
                put("bank_name", "Minha conta")
                put("account_type", AccountType.CHECKING.name)
                put("balance", 0.0)
                put("initial_balance", 0.0)
                put("color", "#2E7D52")
                put("icon", "account_balance")
                putNull("notes")
                put("is_active", 1)
                put("created_at", DateUtils.today())
            }
            db.insert("accounts", SQLiteDatabase.CONFLICT_IGNORE, values)
        }

        private fun ensureDefaultCategories(db: SupportSQLiteDatabase) {
            val count = db.query("SELECT COUNT(*) FROM categories").use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            if (count > 0) return

            DEFAULT_CATEGORIES.forEach { category ->
                val values = ContentValues().apply {
                    put("name", category.name)
                    put("icon", category.icon)
                    put("color", category.color)
                    put("type", category.type.name)
                    putNull("parent_id")
                    put("is_default", 1)
                    put("is_active", 1)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }

        private val DEFAULT_CATEGORIES = listOf(
            // Receitas
            CategoryEntity(name = "Salário", icon = "ic_salary", color = "#4CAF50", type = CategoryType.INCOME, isDefault = true),
            CategoryEntity(name = "Freelance", icon = "ic_freelance", color = "#66BB6A", type = CategoryType.INCOME, isDefault = true),
            CategoryEntity(name = "Investimentos", icon = "ic_investment", color = "#43A047", type = CategoryType.INCOME, isDefault = true),
            CategoryEntity(name = "Outros Recebimentos", icon = "ic_other_income", color = "#388E3C", type = CategoryType.INCOME, isDefault = true),

            // Despesas
            CategoryEntity(name = "Alimentação", icon = "ic_food", color = "#FF5722", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Moradia", icon = "ic_home", color = "#E91E63", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Transporte", icon = "ic_transport", color = "#9C27B0", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Saúde", icon = "ic_health", color = "#F44336", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Educação", icon = "ic_education", color = "#3F51B5", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Lazer", icon = "ic_leisure", color = "#FF9800", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Vestuário", icon = "ic_clothing", color = "#795548", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Assinaturas", icon = "ic_subscriptions", color = "#607D8B", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Impostos", icon = "ic_taxes", color = "#B71C1C", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Seguros", icon = "ic_insurance", color = "#1565C0", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Pets", icon = "ic_pets", color = "#8D6E63", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Presentes", icon = "ic_gifts", color = "#D81B60", type = CategoryType.EXPENSE, isDefault = true),
            CategoryEntity(name = "Outras Despesas", icon = "ic_other_expense", color = "#757575", type = CategoryType.EXPENSE, isDefault = true)
        )
    }
}
