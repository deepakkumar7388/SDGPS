package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var onboardingPagerAdapter: OnboardingPagerAdapter
    private lateinit var indicatorsContainer: LinearLayout
    private var role: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        role = intent.getStringExtra("role")
        email = intent.getStringExtra("email")

        setupOnboardingItems()
        setupIndicators()
        setCurrentIndicator(0)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val btnSkip = findViewById<MaterialButton>(R.id.btnSkip)
        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        val btnFinish = findViewById<MaterialButton>(R.id.btnFinish)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                if (position == onboardingPagerAdapter.itemCount - 1) {
                    btnNext.visibility = View.GONE
                    btnFinish.visibility = View.VISIBLE
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.visibility = View.VISIBLE
                    btnFinish.visibility = View.GONE
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < onboardingPagerAdapter.itemCount) {
                viewPager.currentItem += 1
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        btnFinish.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupOnboardingItems() {
        val onboardingItems = OnboardingData.getOnboardingDataForRole(role)
        onboardingPagerAdapter = OnboardingPagerAdapter(onboardingItems)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = onboardingPagerAdapter
    }

    private fun setupIndicators() {
        indicatorsContainer = findViewById(R.id.indicatorsContainer)
        val indicators = arrayOfNulls<ImageView>(onboardingPagerAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i].apply {
                this?.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.bg_circle_pending // Using an existing drawable or we can create a generic one
                    )
                )
                this?.layoutParams = layoutParams
            }
            indicatorsContainer.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.bg_circle_active // Use existing drawable for active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.bg_circle_pending // Use existing drawable for inactive
                    )
                )
            }
        }
    }

    private fun finishOnboarding() {
        val sharedPreferences = getSharedPreferences("DigitalPassPrefsOnBoarding", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        // Mark onboarding as seen for this specific user
        val safeEmail = email ?: LoginUserDataHolder.loginUserData?.get("email") ?: "unknown"
        editor.putBoolean("has_seen_onboarding", true)
        editor.apply()

        actualNavigateToDashboard()
    }

    private fun actualNavigateToDashboard() {
        val intent = when (role?.lowercase()) {
            "admin", "principal", "hod", "faculty" -> Intent(this, ManagementMember::class.java)
            "student" -> Intent(this, Student::class.java)
            "security guard" -> Intent(this, SecurityGuard::class.java)
            "reception" -> Intent(this, Reception::class.java)
            else -> null
        }

        if (intent != null) {
            // Valid role — connect socket for non-students and navigate
            if (role?.lowercase() != "student") SocketManager.connect()
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()  // Only finish once we have confirmed a valid destination
        } else {
            // Role was null or unrecognised — fall back to login screen
            // so the user is never left with a blank screen
            val fallback = Intent(this, MainActivity::class.java)
            fallback.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(fallback)
            finish()
        }
    }
}
