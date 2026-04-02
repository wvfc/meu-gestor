package com.meugestor.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CHECKING,
    SAVINGS,
    INVESTMENT,
    CASH,
    EMERGENCY
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "account_type")
    val accountType: AccountType,

    @ColumnInfo(name = "balance")
    val balance: Double,

    @ColumnInfo(name = "initial_balance")
    val initialBalance: Double,

    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
