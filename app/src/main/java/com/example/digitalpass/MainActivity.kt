package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.credentials.CredentialManager
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.digitalpass.ui.LoginScreen
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val newConfig = android.content.res.Configuration(newBase.resources.configuration)
        newConfig.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(newConfig))
    }

    private val isLoadingState = mutableStateOf(false)
    private val emailState = mutableStateOf("")
    private val passwordState = mutableStateOf("")

    private var fetchedUsername: String? = null
    private var fetchedPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        retrieveSavedCredentials()

        setContent {
            val isLoading by isLoadingState
            val email by emailState
            val password by passwordState

            LoginScreen(
                initialEmail = email,
                initialPassword = password,
                isLoading = isLoading,
                onLoginClick = { emailInput, passInput ->
                    performLogin(emailInput, passInput)
                },
                onForgotPasswordClick = {
                    startActivity(Intent(this@MainActivity, ForgetPassword::class.java))
                }
            )
        }
    }

    private fun performLogin(emailSt: String, passwordSt: String) {
        if (emailSt.trim().isEmpty() || passwordSt.trim().isEmpty()) {
            Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
            return
        }

        isLoadingState.value = true
        val sharedPreferences = getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE)
        val loginData = LoginData(emailSt.trim(), passwordSt.trim())
        val call = RetrofitClient.instance.loginUser(loginData)

        call.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>>,
                response: Response<HashMap<String, String>>
            ) {
                isLoadingState.value = false
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
                            LoginUserDataHolder.saveState(this@MainActivity)
                            LoginUserDataHolder.storeFCMToken()
                            createNotificationChannel()

                            val currentHash = hashString(passwordSt)
                            val savedHashKey = "cred_hash_$emailSt"
                            val savedHash = sharedPreferences.getString(savedHashKey, null)

                            if ((emailSt == fetchedUsername && passwordSt == fetchedPassword) || currentHash == savedHash) {
                                getPermission()
                            } else {
                                val credentialManager = CredentialManager.create(this@MainActivity)
                                val passwordRequest = CreatePasswordRequest(emailSt, passwordSt)

                                lifecycleScope.launch {
                                    try {
                                        credentialManager.createCredential(this@MainActivity, passwordRequest)
                                        sharedPreferences.edit().putString(savedHashKey, currentHash).apply()
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to save credential", e)
                                    } finally {
                                        getPermission()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Login Failed: Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HashMap<String, String>>, t: Throwable) {
                isLoadingState.value = false
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualNavigateToDashboard(role: String?) {
        if (role.isNullOrBlank()) {
            LoginUserDataHolder.token = ""
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

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
            actualNavigateToDashboard(LoginUserDataHolder.loginUserData?.get("role"))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            actualNavigateToDashboard(LoginUserDataHolder.loginUserData?.get("role"))
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("DigitalPass", "DigitalPass", android.app.NotificationManager.IMPORTANCE_HIGH)
            channel.description = "DigitalPass Notification Channel"
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun retrieveSavedCredentials() {
        val credentialManager = CredentialManager.create(this)
        val getPasswordOption = GetPasswordOption()
        val getCredRequest = GetCredentialRequest(listOf(getPasswordOption))

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = getCredRequest
                )
                val credential = result.credential
                if (credential is PasswordCredential) {
                    val username = credential.id
                    val userPassword = credential.password
                    fetchedUsername = username
                    fetchedPassword = userPassword
                    emailState.value = username
                    passwordState.value = userPassword
                    performLogin(username, userPassword)
                }
            } catch (e: GetCredentialException) {
                Log.d("MainActivity", "No credentials or user cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to get credentials", e)
            }
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}