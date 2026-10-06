package com.example

import android.app.Application
import com.example.data.firebase.FirebaseManager

/**
 * Point d'entrée de l'application VidéoCash.
 * Initialise les composants centraux (Firebase).
 */
class VideoCashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseManager.init(this)
    }
}
