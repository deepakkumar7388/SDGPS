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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class splashScreen : AppCompatActivity() {
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

        // Total delay before session check to allow animations to be fully appreciated
        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 3200)
    }

    private fun checkSession() {
        val sharedPreferences = getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token != null) {
            val loginWithToken = RetrofitClient.instance.loginUser(LoginData("", token))
            loginWithToken.enqueue(object : Callback<HashMap<String, String>> {
                override fun onResponse(call: Call<HashMap<String, String>?>, response: Response<HashMap<String, String>?>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        LoginUserDataHolder.loginUserData = responseBody
                        LoginUserDataHolder.token = token
                        // Persist full user state so it survives process death
                        LoginUserDataHolder.saveState(this@splashScreen)
                        navigateToDashboard(responseBody!!["role"])
                    } else {
                        navigateToLogin()
                    }
                }
                override fun onFailure(call: Call<HashMap<String, String>?>, t: Throwable) {
                    navigateToLogin()
                }
            })
        } else {
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