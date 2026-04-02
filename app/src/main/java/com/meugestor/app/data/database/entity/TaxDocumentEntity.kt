package com.meugestor.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaxDocumentCategory {
    RENDIMENTOS_TRIBUTAVEIS,
    RENDIMENTOS_ISENTOS,
    RENDIMENTOS_EXCLUSIVOS,
    DESPESAS_MEDICAS,
    EDUCACAO,
    PREVIDENCIA,
    DEPENDENTES,
    PENSAO,
    BENS_DIREITOS,
    DIVIDAS,
    DESPESAS_PROFISSIONAIS,
    ALUGUEL,
    DOACOES,
    INVESTIMENTOS,
    MOVIMENTACOES_BANCARIAS
}

@Entity(tableName = "tax_documents")
data class TaxDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "category")
    val category: TaxDocumentCategory,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "document_date")
    val documentDate: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "has_attachment")
    val hasAttachment: Boolean = false,

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
