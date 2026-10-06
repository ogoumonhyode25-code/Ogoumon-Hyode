package com.example

import com.example.data.model.AppError
import com.example.data.model.AppNotification
import com.example.data.model.UserDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAndSecurityTest {

    @Test
    fun appNotification_identifiesFinancialTypesCorrectly() {
        val withdrawalCreatedNotif = AppNotification(
            type = AppNotification.TYPE_WITHDRAWAL_CREATED,
            title = "Demande reçue"
        )
        assertTrue(withdrawalCreatedNotif.isFinancial)
        assertEquals("Retrait reçu", withdrawalCreatedNotif.badgeLabel)

        val withdrawalPaidNotif = AppNotification(
            type = AppNotification.TYPE_WITHDRAWAL_PAID,
            title = "Paiement envoyé"
        )
        assertTrue(withdrawalPaidNotif.isFinancial)
        assertEquals("Paiement envoyé", withdrawalPaidNotif.badgeLabel)

        val rewardNotif = AppNotification(
            type = AppNotification.TYPE_REWARD_APPROVED,
            title = "Gain validé"
        )
        assertTrue(rewardNotif.isFinancial)
        assertEquals("Gain validé", rewardNotif.badgeLabel)

        val commentNotif = AppNotification(
            type = AppNotification.TYPE_NEW_COMMENT,
            title = "Nouveau commentaire"
        )
        assertFalse(commentNotif.isFinancial)
        assertEquals("Commentaire", commentNotif.badgeLabel)

        val likeNotif = AppNotification(
            type = AppNotification.TYPE_NEW_LIKE,
            title = "Nouveau J'aime"
        )
        assertFalse(likeNotif.isFinancial)
        assertEquals("J'aime", likeNotif.badgeLabel)
    }

    @Test
    fun userDevice_initializesValidModel() {
        val device = UserDevice(
            deviceId = "dev_12345",
            userId = "user_abc",
            fcmToken = "fake_fcm_token_xyz",
            platform = "android",
            active = true
        )

        assertEquals("dev_12345", device.deviceId)
        assertEquals("user_abc", device.userId)
        assertEquals("android", device.platform)
        assertTrue(device.active)
        assertNotNull(device.createdAt)
    }

    @Test
    fun appError_mapsControlledCodesToFrenchMessages() {
        val authErr = AppError.fromCode("AUTH_REQUIRED")
        assertEquals("AUTH_REQUIRED", authErr.code)
        assertTrue(authErr.userMessage.contains("Connexion requise"))

        val permErr = AppError.fromCode("PERMISSION_DENIED")
        assertEquals("PERMISSION_DENIED", permErr.code)

        val amountErr = AppError.fromCode("INVALID_AMOUNT")
        assertEquals("INVALID_AMOUNT", amountErr.code)

        val balanceErr = AppError.fromCode("INSUFFICIENT_BALANCE")
        assertEquals("INSUFFICIENT_BALANCE", balanceErr.code)
        assertTrue(balanceErr.userMessage.contains("Solde disponible insuffisant"))

        val existsErr = AppError.fromCode("WITHDRAWAL_EXISTS")
        assertEquals("WITHDRAWAL_EXISTS", existsErr.code)

        val suspendedErr = AppError.fromCode("ACCOUNT_SUSPENDED")
        assertEquals("ACCOUNT_SUSPENDED", suspendedErr.code)

        val videoErr = AppError.fromCode("VIDEO_NOT_FOUND")
        assertEquals("VIDEO_NOT_FOUND", videoErr.code)

        val rewardErr = AppError.fromCode("REWARD_NOT_ALLOWED")
        assertEquals("REWARD_NOT_ALLOWED", rewardErr.code)

        val rateErr = AppError.fromCode("RATE_LIMITED")
        assertEquals("RATE_LIMITED", rateErr.code)
    }
}
