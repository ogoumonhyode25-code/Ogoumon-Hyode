package com.example.data.model

data class Withdrawal(
    val id: String = "",
    val userId: String = "",
    val amount: Long = 0L,
    val currency: String = "XOF",
    val method: String = METHOD_WAVE,
    val account: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val adminNote: String = "",
    val providerReference: String = ""
) {
    companion object {
        // Méthodes de paiement (Côte d'Ivoire / Afrique de l'Ouest)
        const val METHOD_WAVE = "Wave"
        const val METHOD_ORANGE = "Orange Money"
        const val METHOD_MTN = "MTN MoMo"
        const val METHOD_MOOV = "Moov Money"

        val AVAILABLE_METHODS = listOf(
            METHOD_WAVE,
            METHOD_ORANGE,
            METHOD_MTN,
            METHOD_MOOV
        )

        // Statuts du cycle de vie du retrait
        const val STATUS_PENDING = "pending"
        const val STATUS_REVIEWING = "reviewing"
        const val STATUS_APPROVED = "approved"
        const val STATUS_PROCESSING = "processing"
        const val STATUS_PAID = "paid"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_CANCELLED = "cancelled"
    }

    val isCancellable: Boolean
        get() = status == STATUS_PENDING

    val formattedAmount: String
        get() = Wallet.formatCurrency(amount)

    val rejectionReason: String
        get() = adminNote
}
