package com.example.data.firebase

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.model.UserDevice
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.UUID

object DeviceTokenManager {
    private const val TAG = "DeviceTokenManager"
    private const val PREFS_NAME = "videocash_device_prefs"
    private const val KEY_DEVICE_ID = "device_unique_id"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }
            id = if (!androidId.isNullOrBlank()) androidId else UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    suspend fun registerCurrentDevice(context: Context, userId: String) {
        if (userId.isBlank()) return
        val firestore = FirebaseManager.firestore ?: return

        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val deviceId = getDeviceId(context)

            val device = UserDevice(
                deviceId = deviceId,
                userId = userId,
                fcmToken = token,
                platform = "android",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                active = true
            )

            firestore.collection("userDevices")
                .document(deviceId)
                .set(device)
                .await()

            Log.d(TAG, "Device registered successfully with FCM token: ${token.take(10)}...")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register device FCM token (google-services might be pending): ${e.message}")
        }
    }

    suspend fun unregisterDevice(context: Context) {
        val firestore = FirebaseManager.firestore ?: return
        try {
            val deviceId = getDeviceId(context)
            firestore.collection("userDevices")
                .document(deviceId)
                .update("active", false, "updatedAt", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering device: ${e.message}")
        }
    }
}
