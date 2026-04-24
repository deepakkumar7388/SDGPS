package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

            // 1. Get the Keyboard (IME) insets
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // 2. Calculate the bottom padding.
            // It should be the height of the keyboard OR the system navigation bar, whichever is larger.
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

        // Check for existing token
        val sharedPreferences = getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)


        if (token != null) {
            progressBar.startProgressBar()
            // token exists so call an api to get user data
            val loginWithToken = RetrofitClient.instance.loginUser(LoginData("", token))
            loginWithToken.enqueue(object : Callback<HashMap<String, String>> {
                override fun onResponse(
                    call: Call<HashMap<String, String>?>,
                    response: Response<HashMap<String, String>?>
                ) {

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        LoginUserDataHolder.loginUserData = responseBody
                        LoginUserDataHolder.token = token
                        navigateToDashboard(responseBody!!["role"])

                    } else {
                        Toast.makeText(this@MainActivity, "Enter your email and password", Toast.LENGTH_SHORT).show()
                    }

                    progressBar.stopAnimation()
                }

                override fun onFailure(
                    call: Call<HashMap<String, String>?>,
                    t: Throwable
                ) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@MainActivity, "Error connecting to server", Toast.LENGTH_SHORT).show()
                }
            })
        }

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
                                LoginUserDataHolder.storeFCMToken()
                                getPermission()
                                createNotificationChannel()
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

    private fun navigateToDashboard(role: String?) {
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
            Toast.makeText(this@MainActivity, "Login Successful", Toast.LENGTH_SHORT).show()
            navigateToDashboard(LoginUserDataHolder.loginUserData?.get("role"))
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
            navigateToDashboard(LoginUserDataHolder.loginUserData?.get("role"))
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
