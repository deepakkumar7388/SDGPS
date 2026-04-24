package com.example.digitalpass

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddUser : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var phone: EditText
    private lateinit var campusSpinner: Spinner
    private lateinit var departmentSpinner: Spinner
    private lateinit var roleSpinner: Spinner
    private lateinit var batchSpinner: Spinner
    private lateinit var uid: EditText
    private lateinit var fatherName: EditText
    private lateinit var fatherPhone: EditText
    private lateinit var addButton: Button
    private lateinit var backButton: ImageView

    private lateinit var progressBar: CustomProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_user)

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

        name = findViewById(R.id.newUserName)
        email = findViewById(R.id.newUserEmail)
        phone = findViewById(R.id.phone)
        campusSpinner = findViewById(R.id.campusSpinner)
        departmentSpinner = findViewById(R.id.departmentSpinner)
        roleSpinner = findViewById(R.id.roleSpinner)
        batchSpinner = findViewById(R.id.batchSpinner)
        uid = findViewById(R.id.UID)
        fatherName = findViewById(R.id.fatherName)
        fatherPhone = findViewById(R.id.fatherPhone)
        addButton = findViewById(R.id.add)
        backButton = findViewById(R.id.backButton)

        if(LoginUserDataHolder.loginUserData?.get("role")=="admin")campusSpinner.visibility=View.VISIBLE

        fetchCampusAndDepartment()
        setupSpinnerListeners()

        backButton.setOnClickListener {
            finish()
        }

        addButton.setOnClickListener {
            addUser()
        }
    }

    private fun setupSpinnerListeners() {
        departmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    fetchRole()
                } else {
                    roleSpinner.adapter = null
                    batchSpinner.adapter = null
                    setStudentFieldsVisibility(View.GONE)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    if(LoginUserDataHolder.loginUserData?.get("role")=="admin") {
                        if (campusSpinner.selectedItemPosition == 0 || departmentSpinner.selectedItemPosition == 0) {
                            roleSpinner.setSelection(0)
                            Toast.makeText(
                                this@AddUser,
                                "Please select campus and department first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                    }
                    else if (departmentSpinner.selectedItemPosition == 0) {
                            roleSpinner.setSelection(0)
                            Toast.makeText(
                                this@AddUser,
                                "Please select department first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                    }


                    val selectedRole = roleSpinner.selectedItem.toString()
                    if (selectedRole.equals("student", ignoreCase = true)) {
                        fetchBatchesBasedOnDepartment()
                        setStudentFieldsVisibility(View.VISIBLE)
                    } else {
                        setStudentFieldsVisibility(View.GONE)
                    }

                } else {
                    setStudentFieldsVisibility(View.GONE)
                    batchSpinner.adapter = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setStudentFieldsVisibility(visibility: Int) {
        findViewById<TextView>(R.id.batchLabel).visibility = visibility
        findViewById<TextInputLayout>(R.id.uidInputLayout).visibility = visibility
        findViewById<TextInputLayout>(R.id.FNInputLayout).visibility = visibility
        findViewById<TextInputLayout>(R.id.FPhoneInputLayout).visibility = visibility
        batchSpinner.visibility = visibility
    }

    private fun fetchCampusAndDepartment() {
        progressBar.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.getCampusAndDepartment(LoginUserDataHolder.token)
            call.enqueue(object : Callback<HashMap<String, ArrayList<String>>> {
                override fun onResponse(
                    call: Call<HashMap<String, ArrayList<String>>?>, response: Response<HashMap<String, ArrayList<String>>?>
                ) {
                    if (response.isSuccessful) {
                        val campusAndDepartment = response.body()
                        val campusList = campusAndDepartment?.get("campus") ?: arrayListOf()
                        val departmentList = campusAndDepartment?.get("department") ?: arrayListOf()

                        campusList.add(0, "Select Campus")
                        departmentList.add(0, "Select Department")

                        campusSpinner.adapter = ArrayAdapter(this@AddUser, android.R.layout.simple_spinner_item, campusList)
                        departmentSpinner.adapter = ArrayAdapter(this@AddUser, android.R.layout.simple_spinner_item, departmentList)
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@AddUser, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    progressBar.stopAnimation()
                }

                override fun onFailure(
                    call: Call<HashMap<String, ArrayList<String>>?>, t: Throwable
                ) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@AddUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchRole() {
        progressBar.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.getRoleBasedOnDepartment(hashMapOf(
                "department" to departmentSpinner.selectedItem.toString(),
                "token" to LoginUserDataHolder.token
            ))
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(
                    call: Call<ArrayList<String>?>, response: Response<ArrayList<String>?>
                ) {
                    if (response.isSuccessful) {
                        val roleList = response.body() ?: arrayListOf()
                        roleList.add(0, "Select Role")
                        roleSpinner.adapter = ArrayAdapter(this@AddUser, android.R.layout.simple_spinner_item, roleList)
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@AddUser, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    progressBar.stopAnimation()
                }

                override fun onFailure(call: Call<ArrayList<String>?>, t: Throwable) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@AddUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchBatchesBasedOnDepartment() {
        progressBar.startProgressBar()

        CoroutineScope(Dispatchers.IO).launch {
            var hashForBatch=hashMapOf(
                "department" to departmentSpinner.selectedItem.toString(),
                "role" to roleSpinner.selectedItem.toString(),
                "token" to LoginUserDataHolder.token
            )
            if(LoginUserDataHolder.loginUserData?.get("role")=="admin")hashForBatch.put("campus",campusSpinner.selectedItem.toString())

            val call = RetrofitClient.instance.getBatchesBasedOnDepartment(hashForBatch)

            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(
                    call: Call<ArrayList<String>?>, response: Response<ArrayList<String>?>
                ) {
                    if (response.isSuccessful) {
                        val batchList = response.body() ?: arrayListOf()
                        batchList.add(0, "Select Batch")
                        batchSpinner.adapter = ArrayAdapter(this@AddUser, android.R.layout.simple_spinner_item, batchList)
                    } else {
                        batchSpinner.adapter= ArrayAdapter(this@AddUser, android.R.layout.simple_spinner_item, arrayListOf("Batch not found"))
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@AddUser, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    progressBar.stopAnimation()
                }

                override fun onFailure(call: Call<ArrayList<String>?>, t: Throwable) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@AddUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun addUser() {
        // Validation
        if(LoginUserDataHolder.loginUserData?.get("role")=="admin"&&campusSpinner.selectedItemPosition==0){
            Toast.makeText(this, "Please select campus", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.text.isBlank() || email.text.isBlank() || phone.text.isBlank()|| departmentSpinner.selectedItemPosition == 0 || roleSpinner.selectedItemPosition == 0) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if(roleSpinner.selectedItem.toString().equals("student",ignoreCase = true)) {
            if(uid.text.isBlank() || fatherName.text.isBlank() || fatherPhone.text.isBlank()|| batchSpinner.selectedItemPosition==0){
                Toast.makeText(this, "Please fill all fields for the student role", Toast.LENGTH_SHORT).show()
                return
            }
        }

        //start progress bar
        progressBar.startProgressBar()

        var newUser=hashMapOf(
            "name" to name.text.toString(),
            "email" to email.text.toString(),
            "phone" to phone.text.toString(),
            "department" to departmentSpinner.selectedItem.toString(),
            "role" to roleSpinner.selectedItem.toString(),
            "token" to LoginUserDataHolder.token
        )
        if(roleSpinner.selectedItem.toString().equals("student",ignoreCase = true)) {
            newUser.put("uid",uid.text.toString())
            newUser.put("fathername",fatherName.text.toString())
            newUser.put("fatherphone",fatherPhone.text.toString())
            newUser.put("batch",batchSpinner.selectedItem.toString())
        }
        if(LoginUserDataHolder.loginUserData?.get("role")=="admin")newUser.put("campus",campusSpinner.selectedItem.toString())

        // API Call
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.addNewUser(newUser)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@AddUser, "User Added Successfully", Toast.LENGTH_SHORT).show()
                            finish() // Close activity on success
                        }
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        runOnUiThread {
                            Toast.makeText(this@AddUser, "Failed to add user: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                    progressBar.stopAnimation()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    progressBar.stopAnimation()
                    runOnUiThread {
                        Toast.makeText(this@AddUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}