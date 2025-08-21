package com.nextgenapps.Mahallu.App

//
// App.kt
//
// This Kotlin code demonstrates how to set up Firebase App Check with the
// Google Play Integrity provider for an Android application.
//

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        /*val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            // Use the Play Integrity provider for real devices
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )*/

        /*Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )


        FirebaseApp.initializeApp(this)*/

        /*FirebaseApp.initializeApp(this)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )*/

    }
}