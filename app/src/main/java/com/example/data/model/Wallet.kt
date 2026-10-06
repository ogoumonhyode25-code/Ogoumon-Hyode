package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Modèle pour le Portefeuille VidéoCash.
 * IMPORTANT : Les montants ne sont jamais modifiables directement par le client.
 */
data class Wallet(
    val uid: String = "",
    val availableBalance: Long = 0L,
    val pendingBalance: Long = 0L,
    val totalEarned: Long = 0L,
    val totalWithdrawn: Long = 0L,
    val currency: String = "XOF",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedAvailable: String
        get() = formatCurrency(availableBalance)

    val formattedPending: String
        get() = formatCurrency(pendingBalance)

    val formattedTotalEarned: String
        get() = formatCurrency(totalEarned)

    val formattedTotalWithdrawn: String
        get() = formatCurrency(totalWithdrawn)

    companion object {
        fun formatCurrency(amount: Long): String {
            val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
            return "${formatter.format(amount)} FCFA"
        }
    }
}
