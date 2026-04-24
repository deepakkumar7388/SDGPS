package com.example.digitalpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream


class ManagementMember : AppCompatActivity() {

    private lateinit var excelPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var profileImage: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var visitorAdapter: RecentPassAdapter
    private lateinit var gatepassAdapter: RecentPassAdapter
    private var searchBar: SearchView?=null
    private lateinit var progressBar: CustomProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_management_member)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

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

        swipeRefresh=findViewById(R.id.swipeRefreshLayout)
            swipeRefresh.setOnRefreshListener {
            LoginUserDataHolder.getVisitorList()
            LoginUserDataHolder.getGatePassList()
                swipeRefresh.isRefreshing=false
        }

        progressBar=findViewById(R.id.customProgressBar)

        //create gallery launcher
        galleryLauncher=registerForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->
            if(uri==null)return@registerForActivityResult
            //upload image to server
            CommonOperation.uploadImage(this,uri)
        }

        // Initialize file picker launcher
        excelPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedFileUri: Uri? = result.data?.data
                if (selectedFileUri != null) {

                    progressBar.startProgressBar()
                    uploadExcel(selectedFileUri)
                }
            }
        }

        //profile data setup
         profileImage = findViewById(R.id.ProfileImage)

        //now setup user profile in CommonOperation
        CommonOperation.setupUserProfile(this)

        var drawer=findViewById<DrawerLayout>(R.id.drawerLayout)
        profileImage.setOnClickListener {
            drawer.openDrawer(GravityCompat.END)
        }
        var editProfilePictureLogo=findViewById<ImageView>(R.id.editProfilePictureLogo)
        editProfilePictureLogo.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        //applied gate pass of this user
        var appliedGatePassButton=findViewById<MaterialButton>(R.id.applyGatePassButton)
        appliedGatePassButton.setOnClickListener {
            startActivity(Intent(this, AppliedGatePassBySelfUser::class.java))
        }



        //add new user
        val addUserButton = findViewById<MaterialButton>(R.id.addUserButton)
        addUserButton.setOnClickListener {
            showAddUserOptionsDialog()
        }

        //user management
        val userManagementButton = findViewById<MaterialButton>(R.id.userManagementButton)
        userManagementButton.setOnClickListener {
            startActivity(Intent(this, UserManagement::class.java))
        }

        //check the history of visitor and gatePass
        val historyButton = findViewById<MaterialButton>(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, UserHistory::class.java))
        }

        //Batch management
        val batchesButton = findViewById<MaterialButton>(R.id.batchesButton)
        if (LoginUserDataHolder.loginUserData?.get("role") != "admin" && 
            LoginUserDataHolder.loginUserData?.get("role") != "principal" && 
            LoginUserDataHolder.loginUserData?.get("role") != "hod") {
            batchesButton.visibility = View.GONE
        }
        batchesButton.setOnClickListener {
            //if user is not an admin then we will switch to batch activity
            if(LoginUserDataHolder.loginUserData?.get("role")!="admin"){
                var intent=Intent(this,Batch::class.java)
                intent.putExtra("campusName",LoginUserDataHolder.loginUserData?.get("campus"))
                startActivity(intent)
            }
            else showAllotmentDialog("batch")
        }

        //setup allotment button
        var allotmentButton = findViewById<MaterialButton>(R.id.allotmentButton)
        if(LoginUserDataHolder.loginUserData?.get("role")=="admin"||
            LoginUserDataHolder.loginUserData?.get("role")=="principal"||
            (LoginUserDataHolder.loginUserData?.get("role")=="hod"&& LoginUserDataHolder.loginUserData?.get("department")=="ADMINISTRATION")
            ){
            allotmentButton.visibility=View.VISIBLE
        }

        allotmentButton.setOnClickListener {
            //if user is not admin we directly switch to allotment activity
            if(LoginUserDataHolder.loginUserData?.get("role")!="admin"){
                var intent=Intent(this,LevelForBatch::class.java)
                intent.putExtra("levelType","allotment")
                intent.putExtra("campusName",LoginUserDataHolder.loginUserData?.get("campus"))
                startActivity(intent)
            }
            else showAllotmentDialog("allotment")
        }

         searchBar=findViewById<SearchView>(R.id.userManagementSearch)
        //setup search bar
        setupSearchBar()

        //setup recyclerView and adapter for current visitor and gatepass
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(this)
        visitorAdapter=RecentPassAdapter("visitor",ArrayList())
        gatepassAdapter=RecentPassAdapter("gatePass",ArrayList())

        //setup visitorAdapter default
        recyclerView.adapter=visitorAdapter

        //setup these adapter in LoginUserDataHolder
        LoginUserDataHolder.visitorListAdapter=visitorAdapter
        LoginUserDataHolder.gatePassListAdapter=gatepassAdapter

        LoginUserDataHolder.getVisitorList()
        LoginUserDataHolder.getGatePassList()

        val toggleGroup = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGroup)
        
        // Set initial style
        updateButtonStyles(true)

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                var isVisitor = checkedId == R.id.visitorListButton

                //also set list type in LoginUserDataHolder
                LoginUserDataHolder.listType = if (isVisitor) "visitor" else "gatePass"
                //also we have to filter the list
                LoginUserDataHolder.filterList(searchBar?.query?.toString() ?: "")

                // Smoothly fade out the RecyclerView
                recyclerView.animate().alpha(0f).setDuration(150).withEndAction {
                    // Swap the adapter
                    recyclerView.adapter = if (isVisitor) visitorAdapter else gatepassAdapter

                    // Update button visual feedback
                    updateButtonStyles(isVisitor)

                    // Fade it back in
                    recyclerView.animate().alpha(1f).setDuration(150).start()
                }.start()
            }
        }

    }

    private fun setupSearchBar(){
        searchBar?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                LoginUserDataHolder.filterList(p0?:"")
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                LoginUserDataHolder.filterList(p0?:"")
                return true
            }
        })
    }

    private fun updateButtonStyles(isVisitor: Boolean) {
        val visitorButton = findViewById<MaterialButton>(R.id.visitorListButton)
        val gatePassButton = findViewById<MaterialButton>(R.id.gatePassListButton)

        val activeColor = android.graphics.Color.parseColor("#052E92")
        val inactiveColor = android.graphics.Color.WHITE
        val activeTextColor = android.graphics.Color.WHITE
        val inactiveTextColor = android.graphics.Color.parseColor("#052E92")

        if (isVisitor) {
            visitorButton.setBackgroundColor(activeColor)
            visitorButton.setTextColor(activeTextColor)
            gatePassButton.setBackgroundColor(inactiveColor)
            gatePassButton.setTextColor(inactiveTextColor)
        } else {
            gatePassButton.setBackgroundColor(activeColor)
            gatePassButton.setTextColor(activeTextColor)
            visitorButton.setBackgroundColor(inactiveColor)
            visitorButton.setTextColor(inactiveTextColor)
        }
    }


    private fun uploadExcel(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val contentResolver = contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)

            if (inputStream != null && fileName != null) {
                val bytes = inputStream.use { it.readBytes() }

                // Call the extension functions directly on the objects
                val mediaType =
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull()
                val requestFile = bytes.toRequestBody(mediaType)

                val body = MultipartBody.Part.createFormData("file", fileName, requestFile)

                //Call on the token string directly
                val tokenRequestBody =
                    LoginUserDataHolder.token.toRequestBody("text/plain".toMediaTypeOrNull())

                RetrofitClient.instance.uploadExcelUsers(body, tokenRequestBody)
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@ManagementMember,
                                    "File uploaded successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: response.message()
                                Toast.makeText(
                                    this@ManagementMember,
                                    "Upload failed: $errorMsg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            progressBar.stopAnimation()
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            progressBar.stopAnimation()
                            Toast.makeText(
                                this@ManagementMember,
                                "Error: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }

        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    private fun showAddUserOptionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user_options, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.uploadExcelButton).setOnClickListener {
            openFilePicker()
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.addManuallyButton).setOnClickListener {
            startActivity(Intent(this, AddUser::class.java))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val mimeTypes = arrayOf(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        excelPickerLauncher.launch(Intent.createChooser(intent, "Select Excel File"))
    }



    //fetch the campus list and show the dialog
    private fun showAllotmentDialog(dialogType:String){

        progressBar.startProgressBar()

        //fetch the campus list and show the dialog with list of campus in recycler view of dialog
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.getCampusForAllotment(LoginUserDataHolder.token)
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(
                    call: Call<ArrayList<String>?>,
                    response: Response<ArrayList<String>?>
                ) {
                    progressBar.stopAnimation()
                    if (response.isSuccessful) {
                        var campusList = response.body()!!
                        var intent:Intent?=null
                        if(dialogType=="allotment") {
                            intent = Intent(this@ManagementMember, LevelForBatch::class.java)
                            intent.putExtra("levelType", "allotment")
                        }
                        else {
                            intent=Intent(this@ManagementMember, Batch::class.java)
                        }

                        if(campusList.size==1){
                            intent.putExtra("campusName",campusList[0])
                            startActivity(intent)
                        }
                        else{
                            runOnUiThread {
                            //now we have to show the dialog with list of campus
                            val dialog = MaterialAlertDialogBuilder(this@ManagementMember)
                                .setTitle("Select Campus")
                                .setItems(campusList.toTypedArray()) { dialog, which ->
                                    intent.putExtra("campusName",campusList[which])
                                    startActivity(intent)
                                }
                                .create()
                            dialog.show()
                                }
                        }
                    }
                    else{
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@ManagementMember, errorMessage, Toast.LENGTH_LONG)
                            .show()
                    }


                }

                override fun onFailure(
                    call: Call<ArrayList<String>?>,
                    t: Throwable
                ) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@ManagementMember, "Something went wrong", Toast.LENGTH_SHORT)
                }
            })
        }
    }



}