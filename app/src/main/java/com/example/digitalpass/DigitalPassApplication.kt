package com.example.digitalpass

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

class DigitalPassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force the app to always use light mode, ignoring system setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        LoginUserDataHolder.appContext = this
    }
}
