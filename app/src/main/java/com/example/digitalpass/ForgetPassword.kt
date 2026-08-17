package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.credentials.CreatePasswordRequest
import androidx.lifecycle.lifecycleScope
import com.example.digitalpass.ui.ForgetPasswordScreen
import com.example.digitalpass.ui.ResetPasswordStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgetPassword : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val newConfig = android.content.res.Configuration(newBase.resources.configuration)
        newConfig.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(newConfig))
    }

    private val currentStepState = mutableStateOf(ResetPasswordStep.ENTER_EMAIL)
    private val emailState = mutableStateOf("")
    private val countdownTextState = mutableStateOf("")
    private val canResendState = mutableStateOf(false)
    private val isLoadingState = mutableStateOf(false)

    private var verificationCode = ""
    private var countJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val step by currentStepState
            val emailVal by emailState
            val countdownText by countdownTextState
            val canResend by canResendState
            val loading by isLoadingState

            ForgetPasswordScreen(
                currentStep = step,
                emailValue = emailVal,
                countdownText = countdownText,
                canResend = canResend,
                isLoading = loading,
                onBack = { finish() },
                onSendCode = { emailInput ->
                    if (emailInput.trim().isEmpty()) {
                        Toast.makeText(this@ForgetPassword, "Please enter your email", Toast.LENGTH_SHORT).show()
                    } else {
                        emailState.value = emailInput.trim()
                        sendVerificationCode(emailInput.trim())
                    }
                },
                onVerifyCode = { code ->
                    if (code.trim().isEmpty()) {
                        Toast.makeText(this@ForgetPassword, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                    } else {
                        verificationCode = code.trim()
                        verifyVerificationCode(code.trim())
                    }
                },
                onResetPassword = { newPass, confirmPass ->
                    if (newPass.isEmpty() || confirmPass.isEmpty()) {
                        Toast.makeText(this@ForgetPassword, "Please fill both password fields", Toast.LENGTH_SHORT).show()
                    } else if (newPass != confirmPass) {
                        Toast.makeText(this@ForgetPassword, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    } else {
                        updatePassword(newPass)
                    }
                },
                onResendClick = {
                    sendVerificationCode(emailState.value)
                }
            )
        }
    }

    private fun sendVerificationCode(emailText: String) {
        isLoadingState.value = true
        val call = RetrofitClient.instance.sendVerificationCode(emailText)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoadingState.value = false
                if (response.isSuccessful) {
                    currentStepState.value = ResetPasswordStep.VERIFY_OTP
                    startCountdownTimer()
                } else {
                    Toast.makeText(this@ForgetPassword, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoadingState.value = false
                Toast.makeText(this@ForgetPassword, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startCountdownTimer() {
        countJob?.cancel()
        canResendState.value = false
        countJob = lifecycleScope.launch {
            for (i in 120 downTo 0) {
                val mins = i / 60
                val secs = i % 60
                countdownTextState.value = "Resend code in 0$mins:${if (secs < 10) "0$secs" else "$secs"}"
                delay(1000)
            }
            countdownTextState.value = "Resend code"
            canResendState.value = true
        }
    }

    private fun verifyVerificationCode(code: String) {
        isLoadingState.value = true
        val call = RetrofitClient.instance.verifyVerificationCode(
            hashMapOf(
                "email" to emailState.value,
                "verificationCode" to code
            )
        )

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoadingState.value = false
                if (response.isSuccessful) {
                    countJob?.cancel()
                    currentStepState.value = ResetPasswordStep.RESET_PASSWORD
                } else {
                    Toast.makeText(this@ForgetPassword, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoadingState.value = false
                Toast.makeText(this@ForgetPassword, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePassword(newPassword: String) {
        isLoadingState.value = true
        val call = RetrofitClient.instance.updatePassword(
            hashMapOf(
                "email" to emailState.value,
                "verificationCode" to verificationCode,
                "newPassword" to newPassword
            )
        )

        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                isLoadingState.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@ForgetPassword, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    val token = response.body()
                    if (token != null) {
                        getSharedPreferences("DigitalPassPrefs", MODE_PRIVATE).edit().putString("token", token).apply()
                        LoginUserDataHolder.storeFCMToken()

                        val credentialManager = androidx.credentials.CredentialManager.create(this@ForgetPassword)
                        val passwordRequest = CreatePasswordRequest(emailState.value, newPassword)

                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                credentialManager.createCredential(this@ForgetPassword, passwordRequest)
                            } catch (e: Exception) {
                                Log.w("ForgetPassword", "Credential save skipped: ${e.message}")
                            }

                            val intent = Intent(this@ForgetPassword, splashScreen::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(this@ForgetPassword, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                isLoadingState.value = false
                Toast.makeText(this@ForgetPassword, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }
}