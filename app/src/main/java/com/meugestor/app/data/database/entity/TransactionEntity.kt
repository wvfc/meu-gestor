package com.meugestor.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    ADJUSTMENT
}

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

enum class TransactionStatus {
    PAID,
    PENDING,
    OVERDUE,
    CANCELLED
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategory_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["credit_card_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_transaction_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["subcategory_id"]),
        Index(value = ["account_id"]),
        Index(value = ["credit_card_id"]),
        Index(value = ["parent_transaction_id"]),
        Index(value = ["date"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "subcategory_id")
    val subcategoryId: Long? = null,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "credit_card_id")
    val creditCardId: Long? = null,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "due_date")
    val dueDate: String? = null,

    @ColumnInfo(name = "payment_date")
    val paymentDate: String? = null,

    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,

    @ColumnInfo(name = "recurrence_interval")
    val recurrenceInterval: Int? = null,

    @ColumnInfo(name = "status")
    val status: TransactionStatus,

    @ColumnInfo(name = "is_fixed")
    val isFixed: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "attachment_uri")
    val attachmentUri: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String? = null,

    @ColumnInfo(name = "origin")
    val origin: String? = null,

    @ColumnInfo(name = "is_projection")
    val isProjection: Boolean = false,

    @ColumnInfo(name = "parent_transaction_id")
    val parentTransactionId: Long? = null,

    @ColumnInfo(name = "installment_number")
    val installmentNumber: Int? = null,

    @ColumnInfo(name = "total_installments")
    val totalInstallments: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
