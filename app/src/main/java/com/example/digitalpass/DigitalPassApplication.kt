package com.example.digitalpass

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class DigitalPassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force the app to always use light mode, ignoring system setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
