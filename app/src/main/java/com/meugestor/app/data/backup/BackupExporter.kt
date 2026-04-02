package com.meugestor.app.data.backup

import com.meugestor.app.MeuGestorApp
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

object BackupExporter {

    suspend fun generateBackupJson(app: MeuGestorApp): String {
        val db = app.database

        val accounts = db.accountDao().getAll().first()
        val cards = db.creditCardDao().getAll().first()
        val categories = db.categoryDao().getAll().first()
        val transactions = db.transactionDao().getAll().first()
        val goals = db.goalDao().getAll().first()
        val attachments = db.attachmentDao().getAll().first()

        val taxYears = db.taxDocumentDao().getYears().first()
        val taxDocuments = taxYears.flatMap { year ->
            db.taxDocumentDao().getByYear(year).first()
        }
        val checklistItems = taxYears.flatMap { year ->
            db.taxChecklistDao().getByYear(year).first()
        }

        val root = JSONObject()

        root.put("app", "Meu Gestor")
        root.put("version", "1.0.0")
        root.put("exported_at", java.time.OffsetDateTime.now().toString())

        val preferences = JSONObject()
        preferences.put("is_dark_mode", app.userPreferences.isDarkMode.first())
        preferences.put("has_completed_onboarding", app.userPreferences.hasCompletedOnboarding.first())
        preferences.put("user_name", app.userPreferences.userName.first())
        preferences.put("default_currency", app.userPreferences.defaultCurrency.first())
        preferences.put("last_backup_date", app.userPreferences.lastBackupDate.first())
        preferences.put("is_biometric_enabled", app.userPreferences.isBiometricEnabled.first())
        root.put("preferences", preferences)

        root.put("accounts", JSONArray().apply {
            accounts.forEach { account ->
                put(JSONObject().apply {
                    put("id", account.id)
                    put("name", account.name)
                    put("bank_name", account.bankName)
                    put("account_type", account.accountType.name)
                    put("balance", account.balance)
                    put("initial_balance", account.initialBalance)
                    put("color", account.color)
                    put("icon", account.icon)
                    put("notes", account.notes)
                    put("is_active", account.isActive)
                    put("created_at", account.createdAt)
                })
            }
        })

        root.put("credit_cards", JSONArray().apply {
            cards.forEach { card ->
                put(JSONObject().apply {
                    put("id", card.id)
                    put("name", card.name)
                    put("bank_name", card.bankName)
                    put("brand", card.brand.name)
                    put("total_limit", card.totalLimit)
                    put("closing_day", card.closingDay)
                    put("due_day", card.dueDay)
                    put("color", card.color)
                    put("is_active", card.isActive)
                    put("created_at", card.createdAt)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            categories.forEach { category ->
                put(JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("icon", category.icon)
                    put("color", category.color)
                    put("type", category.type.name)
                    put("parent_id", category.parentId)
                    put("is_default", category.isDefault)
                    put("is_active", category.isActive)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            transactions.forEach { tx ->
                put(JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type.name)
                    put("description", tx.description)
                    put("amount", tx.amount)
                    put("category_id", tx.categoryId)
                    put("subcategory_id", tx.subcategoryId)
                    put("account_id", tx.accountId)
                    put("credit_card_id", tx.creditCardId)
                    put("date", tx.date)
                    put("due_date", tx.dueDate)
                    put("payment_date", tx.paymentDate)
                    put("recurrence_type", tx.recurrenceType.name)
                    put("recurrence_interval", tx.recurrenceInterval)
                    put("status", tx.status.name)
                    put("is_fixed", tx.isFixed)
                    put("notes", tx.notes)
                    put("attachment_uri", tx.attachmentUri)
                    put("tags", tx.tags)
                    put("origin", tx.origin)
                    put("is_projection", tx.isProjection)
                    put("parent_transaction_id", tx.parentTransactionId)
                    put("installment_number", tx.installmentNumber)
                    put("total_installments", tx.totalInstallments)
                    put("created_at", tx.createdAt)
                    put("updated_at", tx.updatedAt)
                })
            }
        })

        root.put("goals", JSONArray().apply {
            goals.forEach { goal ->
                put(JSONObject().apply {
                    put("id", goal.id)
                    put("name", goal.name)
                    put("category_id", goal.categoryId)
                    put("target_amount", goal.targetAmount)
                    put("current_amount", goal.currentAmount)
                    put("period", goal.period.name)
                    put("start_date", goal.startDate)
                    put("end_date", goal.endDate)
                    put("is_active", goal.isActive)
                    put("created_at", goal.createdAt)
                })
            }
        })

        root.put("attachments", JSONArray().apply {
            attachments.forEach { attachment ->
                put(JSONObject().apply {
                    put("id", attachment.id)
                    put("transaction_id", attachment.transactionId)
                    put("tax_document_id", attachment.taxDocumentId)
                    put("uri", attachment.uri)
                    put("type", attachment.type.name)
                    put("description", attachment.description)
                    put("created_at", attachment.createdAt)
                })
            }
        })

        root.put("tax_documents", JSONArray().apply {
            taxDocuments.forEach { doc ->
                put(JSONObject().apply {
                    put("id", doc.id)
                    put("year", doc.year)
                    put("category", doc.category.name)
                    put("description", doc.description)
                    put("amount", doc.amount)
                    put("source", doc.source)
                    put("document_date", doc.documentDate)
                    put("notes", doc.notes)
                    put("has_attachment", doc.hasAttachment)
                    put("is_verified", doc.isVerified)
                    put("created_at", doc.createdAt)
                })
            }
        })

        root.put("tax_checklist_items", JSONArray().apply {
            checklistItems.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("year", item.year)
                    put("description", item.description)
                    put("category", item.category)
                    put("is_completed", item.isCompleted)
                    put("notes", item.notes)
                })
            }
        })

        return root.toString(2)
    }
}
