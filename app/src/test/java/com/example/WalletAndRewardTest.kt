package com.example

import com.example.data.model.RewardSettings
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.data.model.Withdrawal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletAndRewardTest {

    @Test
    fun wallet_formatsAmountsCorrectly() {
        val wallet = Wallet(
            uid = "user_test",
            availableBalance = 25000L,
            pendingBalance = 5000L,
            totalEarned = 40000L,
            totalWithdrawn = 15000L
        )

        assertTrue(wallet.formattedAvailable.contains("25") && wallet.formattedAvailable.contains("FCFA"))
        assertTrue(wallet.formattedPending.contains("5") && wallet.formattedPending.contains("FCFA"))
        assertTrue(wallet.formattedTotalEarned.contains("40") && wallet.formattedTotalEarned.contains("FCFA"))
        assertTrue(wallet.formattedTotalWithdrawn.contains("15") && wallet.formattedTotalWithdrawn.contains("FCFA"))
    }

    @Test
    fun transaction_identifiesPositiveAndNegativeFlows() {
        val rewardTx = Transaction(
            type = Transaction.TYPE_REWARD,
            amount = 10L,
            description = "Gain vue"
        )
        assertTrue(rewardTx.isPositive)
        assertTrue(rewardTx.formattedAmount.startsWith("+"))

        val withdrawalTx = Transaction(
            type = Transaction.TYPE_WITHDRAWAL,
            amount = 5000L,
            description = "Retrait Wave"
        )
        assertFalse(withdrawalTx.isPositive)
        assertTrue(withdrawalTx.formattedAmount.startsWith("-"))

        val refundTx = Transaction(
            type = Transaction.TYPE_REFUND,
            amount = 5000L,
            description = "Remboursement retrait annulé"
        )
        assertTrue(refundTx.isPositive)
        assertTrue(refundTx.formattedAmount.startsWith("+"))
    }

    @Test
    fun withdrawal_verifiesCancellableStatus() {
        val pendingWithdrawal = Withdrawal(
            id = "w1",
            amount = 2000L,
            status = Withdrawal.STATUS_PENDING
        )
        assertTrue(pendingWithdrawal.isCancellable)

        val approvedWithdrawal = Withdrawal(
            id = "w2",
            amount = 2000L,
            status = Withdrawal.STATUS_APPROVED
        )
        assertFalse(approvedWithdrawal.isCancellable)

        val paidWithdrawal = Withdrawal(
            id = "w3",
            amount = 2000L,
            status = Withdrawal.STATUS_PAID
        )
        assertFalse(paidWithdrawal.isCancellable)
    }

    @Test
    fun withdrawal_methodsContainExpectedWestAfricanProviders() {
        assertTrue(Withdrawal.AVAILABLE_METHODS.contains("Wave"))
        assertTrue(Withdrawal.AVAILABLE_METHODS.contains("Orange Money"))
        assertTrue(Withdrawal.AVAILABLE_METHODS.contains("MTN MoMo"))
        assertTrue(Withdrawal.AVAILABLE_METHODS.contains("Moov Money"))
    }

    @Test
    fun rewardSettings_respectsSafeDefaults() {
        val settings = RewardSettings()
        assertEquals(10L, settings.viewReward)
        assertEquals(15, settings.minimumWatchSeconds)
        assertEquals(1000L, settings.minimumWithdrawal)
        assertEquals(100000L, settings.maximumDailyWithdrawal)
        assertTrue(settings.rewardEnabled)
    }
}
