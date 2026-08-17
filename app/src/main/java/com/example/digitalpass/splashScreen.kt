package com.example.digitalpass

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class splashScreen : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val newConfig = android.content.res.Configuration(newBase.resources.configuration)
        newConfig.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(newConfig))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        val mainView = findViewById<View>(R.id.main)
        val logo = findViewById<ImageView>(R.id.logo)
        val appName = findViewById<TextView>(R.id.appName)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Initial States: Invisible and transformed
        logo.alpha = 0f
        logo.scaleX = 0.3f
        logo.scaleY = 0.3f
        logo.rotation = -90f

        appName.alpha = 0f
        appName.scaleX = 0.8f
        appName.scaleY = 0.8f
        appName.translationY = 100f

        // 2. Background Color Animation (Blue -> White)
        // This creates a vibrant "liquid" reveal effect
        val brandBlue = ContextCompat.getColor(this, R.color.blue)
        val white = ContextCompat.getColor(this, R.color.white)

        val colorAnim = ObjectAnimator.ofInt(mainView, "backgroundColor", brandBlue, white)
        colorAnim.setDuration(2200)
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.start()

        // 3. Logo Animation: Dynamic pop and spin with Overshoot
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(0f)
            .setDuration(1300)
            .setInterpolator(AnticipateOvershootInterpolator(1.2f))
            .withEndAction {
                // Subtle breathing effect to keep the logo "alive" during session check
                val breathe = ObjectAnimator.ofPropertyValuesHolder(
                    logo,
                    PropertyValuesHolder.ofFloat("scaleX", 1.05f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.05f)
                ).apply {
                    duration = 1500
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                breathe.start()
            }
            .start()

        // 4. App Name Animation: Bouncy slide up with delay
        appName.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(1000)
            .setStartDelay(800)
            .setInterpolator(OvershootInterpolator(2.0f))
            .start()

        // Quick 1.1s smooth entry - then proceed immediately
        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 1100)
    }

    private fun checkSession() {
        val hasSession = LoginUserDataHolder.loadState(this)
        val role = LoginUserDataHolder.loginUserData?.get("role")
        val token = LoginUserDataHolder.token

        if (hasSession && token.isNotEmpty() && !role.isNullOrBlank()) {
            // User is already logged in locally! Open dashboard immediately (0 lag)
            navigateToDashboard(role)

            // Sync/verify token in background asynchronously without blocking UI
            RetrofitClient.instance.loginUser(LoginData("", token)).enqueue(object : Callback<HashMap<String, String>> {
                override fun onResponse(call: Call<HashMap<String, String>?>, response: Response<HashMap<String, String>?>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            LoginUserDataHolder.loginUserData = it
                            LoginUserDataHolder.token = token
                            LoginUserDataHolder.saveState(this@splashScreen)
                        }
                    }
                }
                override fun onFailure(call: Call<HashMap<String, String>?>, t: Throwable) {
                    // Offline or server asleep, user continues working seamlessly with local state
                }
            })
        } else {
            // No saved session, go directly to Login screen
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun navigateToDashboard(role: String?) {
        val intent = when (role?.lowercase()) {
            "admin", "principal", "hod", "faculty" -> Intent(this, ManagementMember::class.java)
            "student" -> Intent(this, Student::class.java)
            "security guard" -> Intent(this, SecurityGuard::class.java)
            "reception" -> Intent(this, Reception::class.java)
            else -> Intent(this, MainActivity::class.java)
        }

        if (role?.lowercase() != "student" && intent.component?.className != MainActivity::class.java.name) {
            SocketManager.connect()
        }

        startActivity(intent)
        // Smooth fade transition for a professional entry
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}