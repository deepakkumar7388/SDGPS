package com.example.digitalpass

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.ui.VisitorDetailScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File

class EnterVisitor : BaseActivity() {

    private val isLoading = mutableStateOf(false)
    private val memberListState = mutableStateOf<List<Map<String, String>>>(emptyList())
    private val visitorDataState = mutableStateOf<Map<String, String>?>(null)
    private val capturedBitmapState = mutableStateOf<Bitmap?>(null)
    private var multipartImage: MultipartBody.Part? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) return@registerForActivityResult
        capturedBitmapState.value = bitmap
        loadBitmapAndTakeMultipart(bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val operation = intent.getStringExtra("operation") ?: "enter"

        if (operation != "enter") {
            try {
                @Suppress("UNCHECKED_CAST")
                val visitor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("visitor", HashMap::class.java) as? HashMap<String, String>
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("visitor") as? HashMap<String, String>
                }
                visitorDataState.value = visitor
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse visitor data", Toast.LENGTH_SHORT).show()
            }
        }

        loadMembersFromRoom()

        setContent {
            val members by memberListState
            val visitorData by visitorDataState
            val loading by isLoading
            val capturedBitmap by capturedBitmapState

            VisitorDetailScreen(
                operation = operation,
                visitorData = visitorData,
                memberList = members,
                isLoading = loading,
                capturedBitmap = capturedBitmap,
                onBack = { finish() },
                onCapturePhoto = { cameraLauncher.launch(null) },
                onCallPhone = { phoneNum -> makePhoneCall(phoneNum) },
                onSubmitNewVisitor = { name, phone, numVisitors, reason, selectedMember ->
                    submitNewVisitor(name, phone, numVisitors, reason, selectedMember)
                },
                onApproveMeet = { remark ->
                    val vData = visitorDataState.value
                    if (vData != null) {
                        val meetData = hashMapOf(
                            "visitorId" to (vData["visitorId"] ?: ""),
                            "token" to LoginUserDataHolder.token
                        )
                        if (remark.isNotBlank()) meetData["remark"] = remark
                        meetVisitor(meetData)
                    }
                },
                onSecurityExit = {
                    val vData = visitorDataState.value
                    if (vData != null) {
                        meetVisitor(
                            hashMapOf(
                                "visitorId" to (vData["visitorId"] ?: ""),
                                "token" to LoginUserDataHolder.token
                            )
                        )
                    }
                }
            )
        }
    }

    private fun loadMembersFromRoom() {
        isLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allLocalUsers = AppDatabase.getDatabase(this@EnterVisitor).userDao().getAllUsers()
                val isAdmin = LoginUserDataHolder.loginUserData?.get("role") == "admin"
                val currentUserCampus = LoginUserDataHolder.loginUserData?.get("campus") ?: ""
                val validRoles = listOf("principal", "hod", "faculty", "reception", "teacher", "tg")

                val filteredUsers = allLocalUsers.map { it.userData }.filter { user ->
                    val role = user["role"]?.lowercase() ?: ""
                    val campus = user["campus"] ?: ""
                    validRoles.contains(role) && (isAdmin || campus == currentUserCampus)
                }

                runOnUiThread {
                    isLoading.value = false
                    memberListState.value = filteredUsers
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isLoading.value = false
                }
            }
        }
    }

    private fun loadBitmapAndTakeMultipart(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            var quality = 100
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

            while (stream.toByteArray().size / 1024 > 200 && quality > 10) {
                stream.reset()
                quality = if (quality < 30) 5 else 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }

            val file = File(cacheDir, "img${System.currentTimeMillis()}.jpg")
            file.writeBytes(stream.toByteArray())
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            multipartImage = MultipartBody.Part.createFormData("img", "img.jpg", requestFile)
        }
    }

    private fun submitNewVisitor(
        name: String,
        phone: String,
        numVisitors: String,
        reason: String,
        selectedMember: Map<String, String>
    ) {
        if (multipartImage == null) {
            Toast.makeText(this, "Please capture visitor photo", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isBlank() || phone.isBlank() || numVisitors.isBlank() || reason.isBlank()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading.value = true
        val visitor = hashMapOf(
            "name" to name,
            "phone" to phone,
            "numberOfVisitor" to numVisitors,
            "reason" to reason,
            "meetDepartment" to (selectedMember["department"] ?: ""),
            "meetEmail" to (selectedMember["email"] ?: "")
        )

        val jsonObject = JSONObject(visitor as Map<*, *>).toString()
        val requestVisitor = jsonObject.toRequestBody("application/json".toMediaTypeOrNull())
        val requestToken = LoginUserDataHolder.token.toRequestBody("text/plain".toMediaTypeOrNull())

        val callToEnter = RetrofitClient.instance.enterVisitor(requestVisitor, requestToken, multipartImage!!)
        callToEnter.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@EnterVisitor, "Visitor entered successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EnterVisitor, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoading.value = false
                Toast.makeText(this@EnterVisitor, "Network connection error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun meetVisitor(meetData: HashMap<String, String>) {
        isLoading.value = true
        val callToMeet = RetrofitClient.instance.meetVisitor(meetData)
        callToMeet.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val isGuard = LoginUserDataHolder.loginUserData?.get("role")?.lowercase() == "security guard"
                    val msg = if (isGuard) "Visitor exit recorded successfully" else "Visitor meeting cleared successfully"
                    Toast.makeText(this@EnterVisitor, msg, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EnterVisitor, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoading.value = false
                Toast.makeText(this@EnterVisitor, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun makePhoneCall(phone: String) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(dialIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not launch dialer", Toast.LENGTH_SHORT).show()
        }
    }
}