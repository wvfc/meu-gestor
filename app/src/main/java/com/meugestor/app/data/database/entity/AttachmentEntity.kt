package com.meugestor.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttachmentType {
    IMAGE,
    PDF
}

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaxDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tax_document_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["tax_document_id"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,

    @ColumnInfo(name = "tax_document_id")
    val taxDocumentId: Long? = null,

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "type")
    val type: AttachmentType,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
