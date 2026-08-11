package com.example.reroplero.domain

/** One line of a receipt. [gross] is net + VAT, i.e. what was actually paid for it. */
data class ReceiptLine(
    val number: Int,
    val gross: Double,
    val description: String
)

data class ReceiptInfo(
    val uid: String,
    val grossValue: Double,
    val issueDate: String,   // ISO-8601, e.g. 2025-08-01
    val issuerVat: String,
    val lines: List<ReceiptLine>
)

interface ReceiptRepository {
    /**
     * Reads a receipt from the QR code printed on it.
     *
     * @throws IllegalArgumentException if the QR isn't a supported provider link.
     */
    suspend fun fetch(qr: String): ReceiptInfo
}
