package com.herdmanager.app.domain.model

data class Transaction(
    val id: String,
    val type: TransactionType,
    /** Amount in cents (e.g. 1999 = $19.99). */
    val amountCents: Long,
    /** Transaction date as epoch day (days since 1970-01-01). */
    val dateEpochDay: Long,
    val farmId: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    /**
     * Optional weight associated with the transaction (e.g. live or carcass weight in kg).
     * Used primarily for Sales/Purchases to derive price per kilogram and analytics.
     */
    val weightKg: Double? = null,
    /**
     * Optional price per kilogram in cents (e.g. 1250 = R12.50/kg).
     * When both amountCents and weightKg are present, this can be derived,
     * but is stored explicitly for analytics and interoperability.
     */
    val pricePerKgCents: Long? = null,
    // Sale: link to sold animal and buyer contact
    val animalId: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    // Expense: category and description
    val categoryId: String? = null,
    val description: String? = null
) {
    /** For Sales: buyer contact name. For Purchases: seller contact name. */
    val displayContactName: String? get() = contactName

    /** For Sales this is buyer; for Purchases this is seller (provenance). */
    val isSale: Boolean get() = type == TransactionType.SALE
    val isPurchase: Boolean get() = type == TransactionType.PURCHASE
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
}

enum class TransactionType { SALE, PURCHASE, EXPENSE }
