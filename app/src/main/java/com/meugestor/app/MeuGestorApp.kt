package com.meugestor.app

import android.app.Application
import com.meugestor.app.data.database.AppDatabase
import com.meugestor.app.data.datastore.UserPreferences
import com.meugestor.app.data.repository.*
import com.meugestor.app.worker.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MeuGestorApp : Application() {

    /** Escopo de vida do processo, para tarefas curtas de inicialização. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this) }
    val userPreferences by lazy { UserPreferences(this) }

    // Repositories
    val accountRepository by lazy { AccountRepository(database.accountDao()) }
    val creditCardRepository by lazy {
        CreditCardRepository(database, database.creditCardDao(), database.transactionDao())
    }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy {
        TransactionRepository(database, database.transactionDao(), database.accountDao())
    }
    val goalRepository by lazy { GoalRepository(database.goalDao()) }
    val taxRepository by lazy {
        TaxRepository(database.taxDocumentDao(), database.taxChecklistDao())
    }
    val attachmentRepository by lazy { AttachmentRepository(database.attachmentDao()) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // O lembrete diário existia mas nunca era agendado.
            if (userPreferences.notificationsEnabled.first()) {
                ReminderWorker.schedule(this@MeuGestorApp)
            }
        }
    }
}
