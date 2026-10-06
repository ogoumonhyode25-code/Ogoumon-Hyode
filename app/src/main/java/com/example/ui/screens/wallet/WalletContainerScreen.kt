package com.example.ui.screens.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.AppNotification
import com.example.ui.screens.notifications.NotificationsScreen

sealed interface WalletDestination {
    object Main : WalletDestination
    object Withdraw : WalletDestination
    object Transactions : WalletDestination
    object WithdrawalHistory : WalletDestination
    data class WithdrawalDetail(val withdrawalId: String) : WalletDestination
    object RewardHistory : WalletDestination
    object Notifications : WalletDestination
}

@Composable
fun WalletContainerScreen(
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf<WalletDestination>(WalletDestination.Main) }

    when (val dest = currentDestination) {
        WalletDestination.Main -> {
            WalletScreen(
                onNavigateToWithdraw = { currentDestination = WalletDestination.Withdraw },
                onNavigateToTransactions = { currentDestination = WalletDestination.Transactions },
                onNavigateToWithdrawalHistory = { currentDestination = WalletDestination.WithdrawalHistory },
                onNavigateToRewardHistory = { currentDestination = WalletDestination.RewardHistory },
                onNavigateToNotifications = { currentDestination = WalletDestination.Notifications },
                modifier = modifier
            )
        }
        WalletDestination.Withdraw -> {
            WithdrawalScreen(
                onNavigateBack = { currentDestination = WalletDestination.Main },
                onNavigateToWithdrawalHistory = { currentDestination = WalletDestination.WithdrawalHistory },
                modifier = modifier
            )
        }
        WalletDestination.Transactions -> {
            TransactionHistoryScreen(
                onNavigateBack = { currentDestination = WalletDestination.Main },
                modifier = modifier
            )
        }
        WalletDestination.WithdrawalHistory -> {
            WithdrawalHistoryScreen(
                onNavigateBack = { currentDestination = WalletDestination.Main },
                onSelectWithdrawal = { id ->
                    currentDestination = WalletDestination.WithdrawalDetail(id)
                },
                modifier = modifier
            )
        }
        is WalletDestination.WithdrawalDetail -> {
            WithdrawalDetailScreen(
                withdrawalId = dest.withdrawalId,
                onNavigateBack = { currentDestination = WalletDestination.WithdrawalHistory },
                modifier = modifier
            )
        }
        WalletDestination.RewardHistory -> {
            RewardHistoryScreen(
                onNavigateBack = { currentDestination = WalletDestination.Main },
                modifier = modifier
            )
        }
        WalletDestination.Notifications -> {
            NotificationsScreen(
                onNavigateBack = { currentDestination = WalletDestination.Main },
                onNavigateToDestination = { type, refId ->
                    when (type) {
                        AppNotification.TYPE_WITHDRAWAL_CREATED,
                        AppNotification.TYPE_WITHDRAWAL_APPROVED,
                        AppNotification.TYPE_WITHDRAWAL_REJECTED,
                        AppNotification.TYPE_WITHDRAWAL_PAID -> {
                            if (refId.isNotBlank()) {
                                currentDestination = WalletDestination.WithdrawalDetail(refId)
                            } else {
                                currentDestination = WalletDestination.WithdrawalHistory
                            }
                        }
                        AppNotification.TYPE_REWARD_APPROVED -> {
                            currentDestination = WalletDestination.RewardHistory
                        }
                        AppNotification.TYPE_NEW_COMMENT,
                        AppNotification.TYPE_NEW_LIKE -> {
                            onNavigateToHome()
                        }
                        else -> {
                            currentDestination = WalletDestination.Main
                        }
                    }
                },
                modifier = modifier
            )
        }
    }
}
