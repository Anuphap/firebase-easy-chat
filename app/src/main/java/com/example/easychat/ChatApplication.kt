package com.example.easychat

import android.app.Application
import android.support.v7.app.AppCompatDelegate
import com.google.firebase.database.FirebaseDatabase

class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}