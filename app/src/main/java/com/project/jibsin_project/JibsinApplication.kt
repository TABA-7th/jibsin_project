package com.project.jibsin_project

import android.app.Application
import com.google.firebase.FirebaseApp

class JibsinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}