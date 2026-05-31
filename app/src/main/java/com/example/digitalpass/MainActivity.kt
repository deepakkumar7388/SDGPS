package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    lateinit var progressBar: CustomProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                bottomPadding
            )
            insets
        }

        progressBar = findViewById(R.id.customProgressBar)
        val sharedPreferences = getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val email = findViewById<EditText>(R.id.loginEmail)
        val password = findViewById<EditText>(R.id.loginPassword)
        val forgetPassButton = findViewById<TextView>(R.id.forgetPassword)
        
        forgetPassButton.setOnClickListener {
            startActivity(Intent(this, ForgetPassword::class.java))
        }

        loginButton.setOnClickListener {
            val emailSt = email.text.toString()
            val passwordSt = password.text.toString()

            if (emailSt.trim() == "" || passwordSt.trim() == "") {
                Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.startProgressBar()
            val loginData = LoginData(emailSt, passwordSt)
            val call = RetrofitClient.instance.loginUser(loginData)
            call.enqueue(object : Callback<HashMap<String, String>> {
                override fun onResponse(
                    call: Call<HashMap<String, String>>,
                    response: Response<HashMap<String, String>>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            val receivedToken = responseBody["token"]
                            responseBody.remove("token")
                            LoginUserDataHolder.loginUserData = responseBody

                            val editor = sharedPreferences.edit()
                            editor.putString("token", receivedToken)
                            editor.apply()
                            
                            if (receivedToken != null) {
                                LoginUserDataHolder.token = receivedToken
                                // Persist full user state so it survives process death
                                LoginUserDataHolder.saveState(this@MainActivity)
                                LoginUserDataHolder.storeFCMToken()
                                createNotificationChannel()
                                getPermission()
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                    progressBar.stopAnimation()
                }

                override fun onFailure(call: Call<HashMap<String, String>>, t: Throwable) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun triggerSecureGatewayAnimation(role: String?) {
        val gatewayOverlay = findViewById<View>(R.id.gatewayOverlay)
        val leftGate = findViewById<View>(R.id.leftGate)
        val rightGate = findViewById<View>(R.id.rightGate)
        val scannerBeam = findViewById<View>(R.id.scannerBeam)
        val gatewayStatus = findViewById<TextView>(R.id.gatewayStatus)
        val accessGrantedText = findViewById<TextView>(R.id.accessGrantedText)
        val scrollView = findViewById<View>(R.id.scrollView2)

        val leftEmblemText = findViewById<TextView>(R.id.leftEmblemText)
        val rightEmblemText = findViewById<TextView>(R.id.rightEmblemText)
        val leftEmblemPlate = findViewById<View>(R.id.leftEmblemPlate)
        val rightEmblemPlate = findViewById<View>(R.id.rightEmblemPlate)

        // Reset positions, scales, and opacities
        leftGate.translationX = 0f
        rightGate.translationX = 0f
        leftGate.scaleX = 0.85f
        leftGate.scaleY = 0.85f
        leftGate.alpha = 0f
        leftGate.rotationY = 0f
        
        rightGate.scaleX = 0.85f
        rightGate.scaleY = 0.85f
        rightGate.alpha = 0f
        rightGate.rotationY = 0f

        leftEmblemText.alpha = 0.1f
        rightEmblemText.alpha = 0.1f
        leftEmblemPlate.alpha = 0.1f
        rightEmblemPlate.alpha = 0.1f

        leftEmblemText.scaleX = 0.8f
        leftEmblemText.scaleY = 0.8f
        rightEmblemText.scaleX = 0.8f
        rightEmblemText.scaleY = 0.8f

        scannerBeam.translationY = 0f
        scannerBeam.visibility = View.INVISIBLE
        accessGrantedText.visibility = View.GONE
        gatewayStatus.text = "Establishing secure connection..."
        gatewayStatus.setTextColor(android.graphics.Color.parseColor("#8F9CAE"))

        gatewayOverlay.visibility = View.VISIBLE
        gatewayOverlay.alpha = 0f

        // Start drifting ambient background particles
        startFloatingParticles()

        // Step 1: Fade-in overlay, zoom-in the gate doors, and shrink the login form behind
        gatewayOverlay.animate().alpha(1f).setDuration(400).start()
        scrollView.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(400).start()

        leftGate.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()
        rightGate.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).withEndAction {
            
            // Step 2: Start scanning animation from above the screen to the bottom
            scannerBeam.visibility = View.VISIBLE
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels.toFloat()

            val beamAnim = android.animation.ObjectAnimator.ofFloat(
                scannerBeam,
                "translationY",
                -dpToPx(),
                screenHeight
            )
            beamAnim.duration = 2000
            beamAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            // Update scanning stages and emblem light-up
            beamAnim.addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                
                // Detailed progress message sub-states
                when {
                    fraction < 0.25f -> {
                        gatewayStatus.text = "Establishing secure gateway..."
                    }
                    fraction >= 0.25f && fraction < 0.5f -> {
                        gatewayStatus.text = "Scanning credentials..."
                    }
                    fraction >= 0.5f && fraction < 0.75f -> {
                        gatewayStatus.text = "Decrypting signature..."
                        gatewayStatus.setTextColor(android.graphics.Color.parseColor("#00F2FE"))
                    }
                    fraction >= 0.75f -> {
                        gatewayStatus.text = "Identity Verified"
                        gatewayStatus.setTextColor(android.graphics.Color.parseColor("#2AF598"))
                    }
                }

                // Smoothly illuminate the emblem plate/text as scanner sweeps through the middle 30% to 70%
                if (fraction in 0.3f..0.7f) {
                    val progress = (fraction - 0.3f) / 0.4f
                    val currentAlpha = 0.1f + progress * 0.9f
                    val currentScale = 0.8f + progress * 0.2f
                    
                    leftEmblemText.alpha = currentAlpha
                    rightEmblemText.alpha = currentAlpha
                    leftEmblemPlate.alpha = currentAlpha
                    rightEmblemPlate.alpha = currentAlpha
                    
                    leftEmblemText.scaleX = currentScale
                    leftEmblemText.scaleY = currentScale
                    rightEmblemText.scaleX = currentScale
                    rightEmblemText.scaleY = currentScale
                } else if (fraction > 0.7f) {
                    leftEmblemText.alpha = 1.0f
                    rightEmblemText.alpha = 1.0f
                    leftEmblemPlate.alpha = 1.0f
                    rightEmblemPlate.alpha = 1.0f
                    leftEmblemText.scaleX = 1.0f
                    leftEmblemText.scaleY = 1.0f
                    rightEmblemText.scaleX = 1.0f
                    rightEmblemText.scaleY = 1.0f
                }
            }

            beamAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    scannerBeam.visibility = View.INVISIBLE

                    // Step 3: Access Granted climax
                    accessGrantedText.visibility = View.VISIBLE
                    accessGrantedText.alpha = 0f
                    accessGrantedText.scaleX = 0.3f
                    accessGrantedText.scaleY = 0.3f

                    // Quick light pulse on D | P emblem
                    leftEmblemText.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).withEndAction {
                        leftEmblemText.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }.start()
                    rightEmblemText.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).withEndAction {
                        rightEmblemText.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }.start()

                    // Reveal ACCESS GRANTED text with an overshoot spring bounce
                    accessGrantedText.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(450)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2.2f))
                        .withEndAction {
                            
                            // Setup a repeating professional pulse scale glow loop
                            val pulseAnim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                                accessGrantedText,
                                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.06f, 1.0f),
                                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.06f, 1.0f)
                            )
                            pulseAnim.duration = 750
                            pulseAnim.repeatCount = android.animation.ValueAnimator.INFINITE
                            pulseAnim.repeatMode = android.animation.ValueAnimator.REVERSE
                            pulseAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                            pulseAnim.start()

                            // After showing Access Granted for 1.2 seconds, swing the gates open and proceed
                            gatewayStatus.postDelayed({
                                pulseAnim.cancel()

                                // Step 4: 3D Swing Open Gate Transition (Hinge Rotation & Translation)
                                val screenWidth = displayMetrics.widthPixels.toFloat()
                                val leftTarget = -screenWidth / 2f
                                val rightTarget = screenWidth / 2f

                                // Set hinges (pivots) on left and right outer screen edges
                                leftGate.pivotX = 0f
                                leftGate.pivotY = leftGate.height.toFloat() / 2f
                                rightGate.pivotX = rightGate.width.toFloat()
                                rightGate.pivotY = rightGate.height.toFloat() / 2f

                                // Animate left gate (glide and swing backward)
                                leftGate.animate()
                                    .translationX(leftTarget)
                                    .rotationY(-75f)
                                    .alpha(0f)
                                    .setDuration(1000)
                                    .setInterpolator(android.view.animation.AnticipateOvershootInterpolator(1.0f))
                                    .start()

                                // Animate right gate (glide and swing backward)
                                rightGate.animate()
                                    .translationX(rightTarget)
                                    .rotationY(75f)
                                    .alpha(0f)
                                    .setDuration(1000)
                                    .setInterpolator(android.view.animation.AnticipateOvershootInterpolator(1.0f))
                                    .start()

                                accessGrantedText.animate()
                                    .alpha(0f)
                                    .scaleX(0.85f)
                                    .scaleY(0.85f)
                                    .setDuration(500)
                                    .start()

                                gatewayStatus.animate()
                                    .alpha(0f)
                                    .setDuration(500)
                                    .start()

                                // Cross-fade background overlay completely
                                gatewayOverlay.animate()
                                    .alpha(0f)
                                    .setDuration(1000)
                                    .withEndAction {
                                        actualNavigateToDashboard(role)
                                    }
                                    .start()
                            }, 1200)
                        }
                        .start()
                }
            })
            beamAnim.start()
        }.start()
    }

    private fun startFloatingParticles() {
        val p1 = findViewById<View>(R.id.particle1)
        val p2 = findViewById<View>(R.id.particle2)
        val p3 = findViewById<View>(R.id.particle3)
        val p4 = findViewById<View>(R.id.particle4)
        val p5 = findViewById<View>(R.id.particle5)

        animateParticle(p1, -30f, 30f, -40f, 40f, 3200)
        animateParticle(p2, -50f, 50f, -50f, 50f, 4500)
        animateParticle(p3, -25f, 25f, -30f, 30f, 3800)
        animateParticle(p4, -40f, 40f, -40f, 40f, 4100)
        animateParticle(p5, -20f, 20f, -50f, 50f, 3500)
    }

    private fun animateParticle(view: View, minX: Float, maxX: Float, minY: Float, maxY: Float, durationMs: Long) {
        val xAnim = android.animation.ObjectAnimator.ofFloat(view, "translationX", minX, maxX)
        xAnim.duration = durationMs
        xAnim.repeatCount = android.animation.ValueAnimator.INFINITE
        xAnim.repeatMode = android.animation.ValueAnimator.REVERSE
        xAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        xAnim.start()

        val yAnim = android.animation.ObjectAnimator.ofFloat(view, "translationY", minY, maxY)
        yAnim.duration = durationMs + 400
        yAnim.repeatCount = android.animation.ValueAnimator.INFINITE
        yAnim.repeatMode = android.animation.ValueAnimator.REVERSE
        yAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        yAnim.start()
    }

    private fun dpToPx(): Float {
        return 40f * resources.displayMetrics.density
    }

    private fun actualNavigateToDashboard(role: String?) {
        val intent = when (role?.lowercase()) {
            "admin", "principal", "hod", "faculty" -> Intent(this, ManagementMember::class.java)
            "student" -> Intent(this, Student::class.java)
            "security guard" -> Intent(this, SecurityGuard::class.java)
            "reception" -> Intent(this, Reception::class.java)
            else -> null
        }
        intent?.let {
            if (role != "student") SocketManager.connect()
            startActivity(it)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun getPermission() {
        val permissionArray = ArrayList<String>()
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(android.Manifest.permission.CALL_PHONE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionArray.isNotEmpty()) {
            requestPermissions(permissionArray.toTypedArray(), 1)
        } else {
            triggerSecureGatewayAnimation(LoginUserDataHolder.loginUserData?.get("role"))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            Toast.makeText(this@MainActivity, "Login Successful", Toast.LENGTH_SHORT).show()
            triggerSecureGatewayAnimation(LoginUserDataHolder.loginUserData?.get("role"))
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("DigitalPass", "DigitalPass", android.app.NotificationManager.IMPORTANCE_HIGH)
            channel.description = "DigitalPass Notification Channel"
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}