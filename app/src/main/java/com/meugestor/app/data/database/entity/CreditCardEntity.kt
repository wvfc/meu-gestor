package com.meugestor.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CreditCardBrand {
    VISA,
    MASTERCARD,
    ELO,
    AMEX,
    HIPERCARD,
    OTHER
}

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "brand")
    val brand: CreditCardBrand,

    @ColumnInfo(name = "total_limit")
    val totalLimit: Double,

    @ColumnInfo(name = "closing_day")
    val closingDay: Int,

    @ColumnInfo(name = "due_day")
    val dueDay: Int,

    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
