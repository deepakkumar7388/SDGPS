package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.digitalpass.ui.UserManagementViewUserScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagementViewUser : BaseActivity() {

    private val userState = mutableStateOf<Map<String, String>>(emptyMap())
    private val isEditModeState = mutableStateOf(false)
    private val isLoadingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        @Suppress("UNCHECKED_CAST")
        val initialUser = (intent.getSerializableExtra("user") as? HashMap<String, String>) ?: hashMapOf()
        userState.value = initialUser

        setContent {
            val user by userState
            val isEditMode by isEditModeState
            val isLoading by isLoadingState

            UserManagementViewUserScreen(
                user = user,
                isLoading = isLoading,
                isEditMode = isEditMode,
                onBack = { finish() },
                onToggleEditMode = { isEditModeState.value = !isEditModeState.value },
                onSaveEdit = { updatedFields -> saveUserEdits(updatedFields) },
                onRemoveUser = { removeUser() }
            )
        }
    }

    private fun removeUser() {
        val email = userState.value["email"] ?: return
        isLoadingState.value = true

        val hashMap = HashMap<String, Any>().apply {
            put("removeEmails", listOf(email))
            put("token", LoginUserDataHolder.token)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.removeUser(hashMap)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                    isLoadingState.value = false
                    if (response.isSuccessful) {
                        Toast.makeText(this@UserManagementViewUser, "User removed successfully", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent().apply {
                            putExtra("userManagementOperation", "remove")
                            putExtra("previousEmail", email)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        Toast.makeText(this@UserManagementViewUser, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    isLoadingState.value = false
                    Toast.makeText(this@UserManagementViewUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveUserEdits(updatedFields: Map<String, String>) {
        val currentUser = HashMap(userState.value)
        val previousEmail = currentUser["email"] ?: ""

        val newUser = hashMapOf(
            "previousEmail" to previousEmail,
            "name" to (updatedFields["name"] ?: currentUser["name"] ?: ""),
            "email" to (updatedFields["email"] ?: currentUser["email"] ?: ""),
            "phone" to (updatedFields["phone"] ?: currentUser["phone"] ?: ""),
            "department" to (updatedFields["department"] ?: currentUser["department"] ?: ""),
            "role" to (updatedFields["role"] ?: currentUser["role"] ?: ""),
            "token" to LoginUserDataHolder.token
        )

        if ((newUser["role"] ?: "").equals("student", ignoreCase = true)) {
            newUser["uid"] = updatedFields["uid"] ?: currentUser["uid"] ?: ""
            newUser["fathername"] = updatedFields["fathername"] ?: currentUser["fathername"] ?: ""
            newUser["fatherphone"] = updatedFields["fatherphone"] ?: currentUser["fatherphone"] ?: ""
            newUser["batch"] = updatedFields["batch"] ?: currentUser["batch"] ?: ""
        }

        isLoadingState.value = true

        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.editUser(newUser as HashMap<String, String>)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isLoadingState.value = false
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@UserManagementViewUser, "User edited successfully", Toast.LENGTH_SHORT).show()
                            currentUser.putAll(newUser)
                            val resultIntent = Intent().apply {
                                putExtra("userManagementOperation", "edit")
                                putExtra("previousEmail", previousEmail)
                                putExtra("userUpdatedData", currentUser)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@UserManagementViewUser, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isLoadingState.value = false
                    runOnUiThread {
                        Toast.makeText(this@UserManagementViewUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}