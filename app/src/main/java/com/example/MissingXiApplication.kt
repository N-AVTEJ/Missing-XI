package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MissingXiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDummyApiKeyForSandboxMode")
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setProjectId("sandbox-project-id")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MissingXiApplication", "Firebase initialized successfully with sandbox options.")
            }
        } catch (e: Exception) {
            Log.e("MissingXiApplication", "Failed to initialize Firebase manually: ${e.localizedMessage}")
        }
    }
}
