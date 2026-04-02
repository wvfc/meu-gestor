package com.meugestor.app

import android.app.Application
import com.meugestor.app.data.database.AppDatabase
import com.meugestor.app.data.datastore.UserPreferences
import com.meugestor.app.data.repository.*

class MeuGestorApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val userPreferences by lazy { UserPreferences(this) }

    // Repositories
    val accountRepository by lazy { AccountRepository(database.accountDao()) }
    val creditCardRepository by lazy { CreditCardRepository(database.creditCardDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy {
        TransactionRepository(database.transactionDao(), database.accountDao())
    }
    val goalRepository by lazy { GoalRepository(database.goalDao()) }
    val taxRepository by lazy {
        TaxRepository(database.taxDocumentDao(), database.taxChecklistDao())
    }
    val attachmentRepository by lazy { AttachmentRepository(database.attachmentDao()) }
}
