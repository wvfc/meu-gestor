package com.meugestor.app.data.database.converter

import androidx.room.TypeConverter
import com.meugestor.app.data.database.entity.*

class Converters {

    // AccountType converters
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    // CreditCardBrand converters
    @TypeConverter
    fun fromCreditCardBrand(value: CreditCardBrand): String = value.name

    @TypeConverter
    fun toCreditCardBrand(value: String): CreditCardBrand = CreditCardBrand.valueOf(value)

    // CategoryType converters
    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    // TransactionType converters
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    // RecurrenceType converters
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)

    // TransactionStatus converters
    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus): String = value.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    // GoalPeriod converters
    @TypeConverter
    fun fromGoalPeriod(value: GoalPeriod): String = value.name

    @TypeConverter
    fun toGoalPeriod(value: String): GoalPeriod = GoalPeriod.valueOf(value)

    // AttachmentType converters
    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)

    // TaxDocumentCategory converters
    @TypeConverter
    fun fromTaxDocumentCategory(value: TaxDocumentCategory): String = value.name

    @TypeConverter
    fun toTaxDocumentCategory(value: String): TaxDocumentCategory = TaxDocumentCategory.valueOf(value)

    // Tags (comma-separated string list) converters
    @TypeConverter
    fun fromTagsList(tags: List<String>?): String? = tags?.joinToString(",")

    @TypeConverter
    fun toTagsList(value: String?): List<String>? = value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
}
