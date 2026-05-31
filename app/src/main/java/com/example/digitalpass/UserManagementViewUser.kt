package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagementViewUser : BaseActivity() {

    private lateinit var img: ImageView
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
    private lateinit var doneButton: Button
    private lateinit var editButton: ImageView
    private lateinit var user: HashMap<String, String>
    var clickability=false

    private var progressBar: CustomProgressBar?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_management_view_user)
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

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        progressBar=findViewById(R.id.customProgressBar)

        img = findViewById(R.id.userManagementViewUserImg)
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
        doneButton = findViewById(R.id.add)
        editButton = findViewById(R.id.editEnableButton)


        setupUser()
        setupEnableOfAllView(clickability)
        editButton.setOnClickListener {
            clickability=!clickability
            setupEnableOfAllView(clickability)
            //set the visibility of edit button gone
            editButton.visibility=View.GONE
            doneButton.text="Done"
        }

        doneButton.setOnClickListener {
            if(doneButton.text=="Done")editUser()
            else removeUser()
        }

    }

    private fun setupUser() {
        user = intent.getSerializableExtra("user") as HashMap<String, String>

        //setup common things of user
        if (user["img"]?.trim() != "") Glide.with(this).load(LoginUserDataHolder.getURL(user["img"])).into(img)
        name.setText(user["name"])
        email.setText(user["email"])
        phone.setText(user["phone"])
        batchSpinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_item,arrayOf(user["batch"]))

        //setup visibility of fields
        if(LoginUserDataHolder.loginUserData?.get("role") =="admin")
            campusSpinner.visibility = Spinner.VISIBLE


        //if user is student
        if(user["role"]=="student") {
            uid.visibility = EditText.VISIBLE
            fatherName.visibility = EditText.VISIBLE
            fatherPhone.visibility = EditText.VISIBLE
            uid.setText(user["uid"])
            fatherName.setText(user["fathername"])
            fatherPhone.setText(user["fatherphone"])
        }

        fetchCampusAndDepartment()



        setupSpinnerListeners()


    }


    private fun fetchCampusAndDepartment() {

        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.getCampusAndDepartment(LoginUserDataHolder.token)
            call.enqueue(object : Callback<HashMap<String, ArrayList<String>>> {
                override fun onResponse(
                    call: Call<HashMap<String, ArrayList<String>>?>, response: Response<HashMap<String, ArrayList<String>>?>
                ) {
                    progressBar?.stopAnimation()
                    if (response.isSuccessful) {
                        val campusAndDepartment = response.body()
                        val campusList = campusAndDepartment?.get("campus") ?: arrayListOf()
                        val departmentList = campusAndDepartment?.get("department") ?: arrayListOf()

                        campusList.add(0, "Select Campus")
                        departmentList.add(0, "Select Department")

                        campusSpinner.adapter = ArrayAdapter(this@UserManagementViewUser, android.R.layout.simple_spinner_item, campusList)
                        departmentSpinner.adapter = ArrayAdapter(this@UserManagementViewUser, android.R.layout.simple_spinner_item, departmentList)

                        if(LoginUserDataHolder.loginUserData?.get("role") =="admin"){
                            campusSpinner.setSelection(campusList.indexOf(user["campus"]))
                        }
                        departmentSpinner.setSelection(departmentList.indexOf(user["department"]))

                        fetchRole()

                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@UserManagementViewUser, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(
                    call: Call<HashMap<String, ArrayList<String>>?>, t: Throwable
                ) {
                    Toast.makeText(this@UserManagementViewUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
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
                                this@UserManagementViewUser,
                                "Please select campus and department first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                    }
                    else if (departmentSpinner.selectedItemPosition == 0) {
                        roleSpinner.setSelection(0)
                        Toast.makeText(
                            this@UserManagementViewUser,
                            "Please select department first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return

                    }


                    val selectedRole = roleSpinner.selectedItem.toString().trim()
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

    private fun fetchRole() {
        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.getRoleBasedOnDepartment(hashMapOf(
                "department" to departmentSpinner.selectedItem.toString(),
                "token" to LoginUserDataHolder.token
            ))
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(
                    call: Call<ArrayList<String>?>, response: Response<ArrayList<String>?>
                ) {
                    progressBar?.stopAnimation()
                    if (response.isSuccessful) {
                        val roleList = response.body() ?: arrayListOf()
                        roleList.add(0, "Select Role")
                        roleSpinner.adapter = ArrayAdapter(this@UserManagementViewUser, android.R.layout.simple_spinner_item, roleList)

                        //find position of users role and then set it
                        var position=roleList.indexOf(user["role"])
                        if(position==-1)position=0
                        roleSpinner.setSelection(position)
                        //if user is student then we will fetch the batches
                        if(roleSpinner.selectedItem.toString()=="student")fetchBatchesBasedOnDepartment()
                        else setStudentFieldsVisibility(View.GONE)
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@UserManagementViewUser, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ArrayList<String>?>, t: Throwable) {
                    Toast.makeText(this@UserManagementViewUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    private fun fetchBatchesBasedOnDepartment() {
        progressBar?.startProgressBar()
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
                    progressBar?.stopAnimation()
                    if (response.isSuccessful) {
                        val batchList = response.body() ?: arrayListOf()
                        batchList.add(0, "Select Batch")
                        batchSpinner.adapter = ArrayAdapter(this@UserManagementViewUser, android.R.layout.simple_spinner_item, batchList)
                        var position=batchList.indexOf(user["batch"])
                        if(position==-1)position=0
                        batchSpinner.setSelection(position)
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@UserManagementViewUser, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ArrayList<String>?>, t: Throwable) {
                    Toast.makeText(this@UserManagementViewUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setStudentFieldsVisibility(visibility: Int) {
        findViewById<MaterialCardView>(R.id.guardianInfoCard).visibility = visibility
        batchSpinner.visibility = visibility
    }

    private fun setupEnableOfAllView(clickability: Boolean) {
        name.isEnabled = clickability
        email.isEnabled = clickability
        phone.isEnabled = clickability
        campusSpinner.isEnabled = clickability
        departmentSpinner.isEnabled = clickability
        roleSpinner.isEnabled = clickability
        batchSpinner.isEnabled = clickability
        uid.isEnabled = clickability
        fatherName.isEnabled = clickability
        fatherPhone.isEnabled = clickability
    }

    private fun removeUser() {

        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.removeUser(hashMapOf(
                "removeEmail" to user["email"]!!,
                "token" to LoginUserDataHolder.token
            ))
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    progressBar?.stopAnimation()
                    if (response.isSuccessful) {
                        Toast.makeText(this@UserManagementViewUser, "User removed successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    else{
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@UserManagementViewUser, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<ResponseBody?>,
                    t: Throwable
                ) {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@UserManagementViewUser, "Something went wrong: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })


        }
    }

    private fun editUser() {
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

        progressBar?.startProgressBar()

        var newUser=hashMapOf(
            "previousEmail" to user["email"],
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
            val call = RetrofitClient.instance.editUser(newUser as HashMap<String, String>)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    progressBar?.stopAnimation()
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@UserManagementViewUser, "User edited successfully", Toast.LENGTH_SHORT).show()
                            finish() // Close activity on success
                        }
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        runOnUiThread {
                            Toast.makeText(this@UserManagementViewUser, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    progressBar?.stopAnimation()
                    runOnUiThread {
                        Toast.makeText(this@UserManagementViewUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    override fun onResume(){
        super.onResume()
    }
}