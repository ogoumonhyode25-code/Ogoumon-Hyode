package com.example.data.model

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val type: String = TYPE_REWARD,
    val amount: Long = 0L,
    val currency: String = "XOF",
    val source: String = "",
    val status: String = STATUS_COMPLETED,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val referenceId: String = ""
) {
    companion object {
        // Types possibles
        const val TYPE_REWARD = "reward"
        const val TYPE_WITHDRAWAL = "withdrawal"
        const val TYPE_ADJUSTMENT = "adjustment"
        const val TYPE_REFUND = "refund"

        // Statuts possibles
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_COMPLETED = "completed"
    }

    val isPositive: Boolean
        get() = type == TYPE_REWARD || type == TYPE_REFUND || (type == TYPE_ADJUSTMENT && amount >= 0)

    val formattedAmount: String
        get() {
            val prefix = if (isPositive) "+" else "-"
            val absVal = kotlin.math.abs(amount)
            return "$prefix${Wallet.formatCurrency(absVal)}"
        }
}
