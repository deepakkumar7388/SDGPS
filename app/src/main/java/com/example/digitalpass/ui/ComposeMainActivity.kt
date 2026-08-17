package com.example.digitalpass.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.digitalpass.*
import com.example.digitalpass.ui.screens.LoginScreen
import com.example.digitalpass.ui.screens.SplashScreen
import com.example.digitalpass.ui.theme.DigitalPassTheme

class ComposeMainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val newConfig = android.content.res.Configuration(newBase.resources.configuration)
        newConfig.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(newConfig))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DigitalPassTheme(darkTheme = false) {
                var currentScreen by remember { mutableStateOf("splash") }

                when (currentScreen) {
                    "splash" -> SplashScreen(
                        onNavigateToLogin = { currentScreen = "login" },
                        onNavigateToDashboard = { role -> navigateToDashboard(role) }
                    )
                    "login" -> LoginScreen(
                        onLoginSuccess = { role ->
                            if (role != null) {
                                navigateToDashboard(role)
                            }
                        },
                        onForgotPassword = {
                            startActivity(Intent(this@ComposeMainActivity, ForgetPassword::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun navigateToDashboard(role: String) {
        // Check onboarding
        val sharedPreferencesOnBoarding = getSharedPreferences("DigitalPassPrefsOnBoarding", Context.MODE_PRIVATE)
        val hasSeenOnboarding = sharedPreferencesOnBoarding.getBoolean("has_seen_onboarding", false)

        if (!hasSeenOnboarding) {
            val intent = Intent(this, OnboardingActivity::class.java)
            intent.putExtra("role", role)
            intent.putExtra("email", LoginUserDataHolder.loginUserData?.get("email"))
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            return
        }

        val intent = when (role.lowercase()) {
            "admin", "principal", "hod", "faculty" -> Intent(this, ManagementMember::class.java)
            "student" -> Intent(this, Student::class.java)
            "security guard" -> Intent(this, SecurityGuard::class.java)
            "reception" -> Intent(this, Reception::class.java)
            else -> null
        }

        intent?.let {
            startActivity(it)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            if (role.lowercase() != "student") SocketManager.connect()
            finish()
        }
    }
}
