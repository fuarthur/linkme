package com.ams.linkme

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

class LinkMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val firestore = FirebaseFirestore.getInstance()
        // Private conversations are kept in memory, never in a shared disk cache.
        firestore.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        if (BuildConfig.DEBUG && BuildConfig.USE_EMULATORS) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            firestore.useEmulator("10.0.2.2", 8080)
        }
    }
}
