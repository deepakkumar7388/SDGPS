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

        // Force font scale to 1.0 globally to prevent system font size changes from breaking UI
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val config = Configuration(activity.resources.configuration)
                if (config.fontScale != 1.0f) {
                    config.fontScale = 1.0f
                    @Suppress("DEPRECATION")
                    activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
