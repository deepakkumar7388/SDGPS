package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.exceptions.CreateCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Call
import retrofit2.Callback

class ForgetPassword : AppCompatActivity() {
    private lateinit var updateText: TextView
    private lateinit var emailTextInputLayout: TextInputLayout
    private lateinit var email: TextInputEditText
    private lateinit var confirmPassword: TextInputEditText
    private lateinit var doneButton: Button

    private var emailText=""
    private var verificationCode=""
    lateinit var countDownOrResend:TextView
    private var countJob: Job? = null

    private lateinit var progressBar:CustomProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forget_password)
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

        progressBar=findViewById(R.id.customProgressBar)

        updateText = findViewById(R.id.textViewForUpdates)
        emailTextInputLayout = findViewById(R.id.emailInputLayout)
        email = findViewById(R.id.emailToForgetPassword)
        confirmPassword = findViewById(R.id.confirmPassword)
        doneButton = findViewById(R.id.doneButton)
        countDownOrResend=findViewById(R.id.countDownOrResendVerificationCode)



        doneButton.setOnClickListener {
            if(email.text.toString().trim()==""){
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else {

                emailText = email.text.toString()
                sendVerificationCode()
            }
        }
    }

    private fun sendVerificationCode(){

        progressBar.startProgressBar()

        //send verification code to the email
        var callToSendVerificationCode= RetrofitClient.instance.sendVerificationCode(emailText)
        callToSendVerificationCode.enqueue(object: Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                if(response.isSuccessful){
                    //set visibility of countDownOrResend to visible, also remove setOnClickListener
                    countDownOrResend.visibility= View.VISIBLE
                    countDownOrResend.setOnClickListener(null)
                    if(countJob!=null)countJob?.cancel()

                    //start the count down of 2 minutes by using lifecycleScope
                    countJob=lifecycleScope.launch {
                        for(i in 120 downTo 0){
                            countDownOrResend.text="Resend in 0${i/60}:${if(i%60<10) "0${i%60}" else i%60}"
                            delay(1000)
                        }
                        countDownOrResend.text="Resend code"
                        countDownOrResend.setOnClickListener {
                            sendVerificationCode()
                        }
                    }


                    email.text?.clear()
                    emailTextInputLayout.hint="Enter verification code"
                    //give the update to user that verification code has been sent on your email
                    updateText.text="Verification code has been sent to $emailText"
                    doneButton.text="Verify"

                    //set the click listener to verify code
                    doneButton.setOnClickListener {
                        if(email.text.toString()==""){
                            Toast.makeText(this@ForgetPassword,"Please enter the verification code",Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        verifyVerificationCode()
                    }
                }
                else Toast.makeText(this@ForgetPassword, LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()

                progressBar.stopAnimation()
            }
            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                progressBar.stopAnimation()
                Toast.makeText(this@ForgetPassword,"Something is went wrong",Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verifyVerificationCode(){
        progressBar.startProgressBar()
        verificationCode=email.text.toString()
        //verify the verification code
        var callToVerifyVerificationCode= RetrofitClient.instance.verifyVerificationCode(hashMapOf(
            "email" to emailText,
            "verificationCode" to verificationCode
        ))

        callToVerifyVerificationCode.enqueue(object: Callback<ResponseBody>{
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                if(response.isSuccessful){
                    //set the visibility of countDownOrResend to gone
                    countDownOrResend.visibility=View.GONE
                    countJob?.cancel()
                    updateText.text="Create new password for your account $emailText"
                    emailTextInputLayout.hint="Enter new password"
                    email.text?.clear()

                    //set the input type of email as textPassword and enable password toggle
                    email.inputType=129
                    emailTextInputLayout.endIconMode=TextInputLayout.END_ICON_PASSWORD_TOGGLE

                    findViewById<TextInputLayout>(R.id.ConfirmPasswordInputLayout).visibility= View.VISIBLE

                    doneButton.text="Update"
                    //set the click listener to update password
                    doneButton.setOnClickListener {
                        if(email.text.toString()==""||confirmPassword.text.toString()==""){
                            Toast.makeText(this@ForgetPassword,"Please enter the new password",Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if(email.text.toString()!=confirmPassword.text.toString()){
                            Toast.makeText(this@ForgetPassword,"Password does not match",Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        updatePassword(confirmPassword.text.toString())
                    }
                }
                else Toast.makeText(this@ForgetPassword, LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()

                progressBar.stopAnimation()
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                progressBar.stopAnimation()
                Toast.makeText(this@ForgetPassword,"Something is went wrong",Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePassword(newPassword:String){
        progressBar.startProgressBar()
        //call to update password
        var callToUpdatePassword= RetrofitClient.instance.updatePassword(hashMapOf(
            "email" to emailText,
            "verificationCode" to verificationCode,
            "newPassword" to newPassword
        ))

        callToUpdatePassword.enqueue(object:Callback<String>{
            override fun onResponse(
                call: Call<String?>,
                response: Response<String?>
            ) {
                if(response.isSuccessful){
                    Toast.makeText(this@ForgetPassword,"Password updated successfully",Toast.LENGTH_SHORT).show()
                    //here we receive the token from the server and we have to store it in shared preferences
                    var token=response.body()
                    if(token!=null){
                        getSharedPreferences("DigitalPassPrefs", MODE_PRIVATE).edit().putString("token",token).apply()

                        //store fcm token
                        LoginUserDataHolder.storeFCMToken()

                        //also we have to set or update this password with email in google credential

                        val credentialManager=androidx.credentials.CredentialManager.create(this@ForgetPassword)
                        // Create the password request
                        val passwordRequest = CreatePasswordRequest(emailText, newPassword)

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                credentialManager.createCredential(this@ForgetPassword,passwordRequest)
                                Toast.makeText(this@ForgetPassword, "Credential saved", Toast.LENGTH_SHORT).show()
                            } catch (e: CreateCredentialException) {
                            }

                            // Now navigate to MainActivity
                            val intent = Intent(this@ForgetPassword, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                }
                else Toast.makeText(this@ForgetPassword, LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()
                progressBar.stopAnimation()
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                progressBar.stopAnimation()
                Toast.makeText(this@ForgetPassword,"Something went wrong",Toast.LENGTH_SHORT).show()
            }
        })
    }
}