package com.example.digitalpass

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddReportActivity : BaseActivity() {

    private lateinit var reportImageView: ImageView
    private lateinit var tapToCaptureHint: LinearLayout
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var submitReportButton: MaterialButton
    private lateinit var customProgressBar: CustomProgressBar
    private lateinit var departmentSpinner: Spinner

    private var multipartImage: MultipartBody.Part? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_report)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        reportImageView = findViewById(R.id.reportImageView)
        tapToCaptureHint = findViewById(R.id.tapToCaptureHint)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        submitReportButton = findViewById(R.id.submitReportButton)
        customProgressBar = findViewById(R.id.customProgressBar)
        departmentSpinner = findViewById(R.id.departmentSpinner)

        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap == null) return@registerForActivityResult
            Toast.makeText(this, "Image loading...", Toast.LENGTH_SHORT).show()
            reportImageView.setImageBitmap(bitmap)
            tapToCaptureHint.visibility = View.GONE
            loadBitmapAndTakeMultipart(bitmap)
        }

        reportImageView.setOnClickListener {
            cameraLauncher.launch(null)
        }

        submitReportButton.setOnClickListener {
            submitReport()
        }

        getAllDepartment()
    }

    private fun getAllDepartment(){
        customProgressBar.startProgressBar()
        
        userOperationViewModel.departments.removeObservers(this)
        userOperationViewModel.departments.observe(this) { result ->
            result.onSuccess { departmentList ->
                val departments = ArrayList(departmentList)
                departments.add(0,"ALL DEPARTMENT")
                departmentSpinner.adapter = ArrayAdapter(this@AddReportActivity, android.R.layout.simple_spinner_item, departments)
            }.onFailure {
                Toast.makeText(this@AddReportActivity, "Something went wrong: ${it.message}", Toast.LENGTH_SHORT).show()
            }
            customProgressBar.stopAnimation()
            userOperationViewModel.departments.removeObservers(this)
        }
        
        userOperationViewModel.fetchDepartments(LoginUserDataHolder.token, "report")
    }

    private fun loadBitmapAndTakeMultipart(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            var quality = 100
            var stream = ByteArrayOutputStream()
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

            runOnUiThread {
                Toast.makeText(this@AddReportActivity, "Image loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitReport() {
        val description = descriptionEditText.text.toString().trim()
        
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }

        if (multipartImage == null) {
            Toast.makeText(this, "Please capture a picture", Toast.LENGTH_SHORT).show()
            return
        }

        customProgressBar.startProgressBar()
        submitReportButton.isEnabled = false
        var requestDescription=description.toRequestBody("text/plain".toMediaTypeOrNull())
        var requestDepartment=departmentSpinner.selectedItem.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val requestToken = LoginUserDataHolder.token.toRequestBody("text/plain".toMediaTypeOrNull())

        val callToAddReport = RetrofitClient.instance.addReport(requestDepartment,requestDescription, requestToken, multipartImage!!)
        callToAddReport.enqueue(object : Callback<HashMap<String,String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                customProgressBar.stopAnimation()
                submitReportButton.isEnabled=true
                if (response.isSuccessful) {
                    Toast.makeText(this@AddReportActivity,"Report Submitted",Toast.LENGTH_LONG).show()
                    var resultIntent= Intent().apply{
                        putExtra("report",response.body())
                    }
                    setResult(RESULT_OK,resultIntent)
                    finish()
                    }
                else Toast.makeText(this@AddReportActivity, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(
                call: Call<HashMap<String, String>?>,
                t: Throwable
            ) {
                customProgressBar.stopAnimation()
                submitReportButton.isEnabled=true
                Toast.makeText(this@AddReportActivity, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
