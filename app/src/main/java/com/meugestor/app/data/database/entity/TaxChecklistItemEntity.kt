package com.meugestor.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tax_checklist_items")
data class TaxChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
