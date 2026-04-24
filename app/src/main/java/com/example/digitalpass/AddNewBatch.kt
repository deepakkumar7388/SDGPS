package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddNewBatch : AppCompatActivity() {

    lateinit var level1RecyclerView: RecyclerView
    lateinit var level2RecyclerView: RecyclerView
    var level1Array=ArrayList<String>()
    var level2Array=ArrayList<String>()

    lateinit var batchYearSpinner: Spinner
    lateinit var batchDepartmentSpinner: Spinner
    lateinit var batchSectionSpinner: Spinner
    lateinit var batchCampusSpinner: Spinner
    lateinit var level1CreateBatch: Button
    lateinit var level2CreateBatch: Button
    lateinit var add: Button
    lateinit var editButton: ImageView

    private lateinit var progressBar:CustomProgressBar



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_new_batch)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var toolbar=findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        progressBar=findViewById(R.id.customProgressBar)

        //set spinner for batch

        batchYearSpinner = findViewById<Spinner>(R.id.batchYearSpinner)
        batchDepartmentSpinner = findViewById<Spinner>(R.id.batchDepartmentSpinner)
        batchSectionSpinner = findViewById<Spinner>(R.id.batchSectionSpinner)
        batchCampusSpinner = findViewById<Spinner>(R.id.batchCampusSpinner)

        //set level for batch
        level1CreateBatch = findViewById<Button>(R.id.level1CreateBatch)
        level2CreateBatch = findViewById<Button>(R.id.level2CreateBatch)

        //set recyclerView for level
        level1RecyclerView = findViewById(R.id.recyclerViewLevel1)
        level2RecyclerView = findViewById(R.id.recyclerViewLevel2)
        level1RecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        level2RecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        add = findViewById<Button>(R.id.add)

        if (intent.getStringExtra("operation") == "create") {
            if(LoginUserDataHolder.loginUserData?.get("role")!="admin"){
                batchCampusSpinner.visibility=Spinner.GONE
            }
            fetchBatchData()
            setAddButtonToCreateBatch()
        } else {
            toolbar.title= "Edit Batch"
            add.text = "Remove"
            //set edit button
             editButton = findViewById<ImageView>(R.id.editButton)
            editButton.visibility = Button.VISIBLE
            var textViewForBatchName = findViewById<TextView>(R.id.textViewForBatchName)
            textViewForBatchName.visibility = TextView.VISIBLE
            textViewForBatchName.text = intent.getStringExtra("batchName")

            //set visibility gone of spinner and label
            findViewById<TextView>(R.id.yearSpinnerLabel).visibility = TextView.GONE
            findViewById<TextView>(R.id.campusSpinnerLabel).visibility = TextView.GONE
            findViewById<TextView>(R.id.departmentSpinnerLabel).visibility = TextView.GONE
            findViewById<TextView>(R.id.sectionSpinnerLabel).visibility = TextView.GONE
            batchYearSpinner.visibility = Spinner.GONE
            batchDepartmentSpinner.visibility = Spinner.GONE
            batchSectionSpinner.visibility = Spinner.GONE
            batchCampusSpinner.visibility = Spinner.GONE

            //set level click disable
            level1CreateBatch.isEnabled=false
            level2CreateBatch.isEnabled=false

            editButton.setOnClickListener {
                level1CreateBatch.isEnabled = true
                level2CreateBatch.isEnabled = true
                Toast.makeText(this, "Edit enable", Toast.LENGTH_SHORT).show()
                editButton.visibility = ImageView.GONE
                add.text = "Done"
            }

            //fetch level data
            fetchLevelData()

            //set add button to remove or edit done batch
            setAddButtonToEditBatch()

            //set leve1 and level2 button to edit level
            setEditLevelButton()

        }
    }

    //data of level1 and level2 will come here
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                var level1Data =
                    data?.getSerializableExtra("levelData") as ArrayList<*> as ArrayList<HashMap<String, String>>
                level1RecyclerView.adapter = LevelAdapter(level1Data)

                //get the email of level1 member in array
                level1Array = getEmail(level1Data)
            }
        }
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                var level2Data =
                    data?.getSerializableExtra("levelData") as ArrayList<*> as ArrayList<HashMap<String, String>>
                level2RecyclerView.adapter = LevelAdapter(level2Data)

                //get the email of level2 member in array
                level2Array = getEmail(level2Data)
            }
        }
    }

    //get email from leveled member
    fun getEmail(levelData: ArrayList<HashMap<String, String>>): ArrayList<String> {
        var email = ArrayList<String>()
        for (i in levelData) {
            email.add(i["email"]!!)
        }
        return email
    }

    //fetch batch data
    fun fetchBatchData() {
        progressBar.startProgressBar()
        //api call for batch data
        var callForBatchData = RetrofitClient.instance.getDataForBatch(LoginUserDataHolder.token)
        callForBatchData.enqueue(object : Callback<HashMap<String, ArrayList<String>>> {
            override fun onResponse(
                call: Call<HashMap<String, ArrayList<String>>?>,
                response: Response<HashMap<String, ArrayList<String>>?>
            ) {
                progressBar.stopAnimation()
                if (response.isSuccessful) {
                    val batchData = response.body()
                    //fill the batch data in spinner
                    //insert select year option in batchData!!["year"]
                    batchData!!["year"]!!.add(0, "Select Year")
                    batchYearSpinner.adapter = ArrayAdapter(
                        this@AddNewBatch,
                        android.R.layout.simple_spinner_item,
                        batchData["year"]!!
                    )
                    batchData["department"]!!.add(0, "Select Department")
                    batchDepartmentSpinner.adapter = ArrayAdapter(
                        this@AddNewBatch,
                        android.R.layout.simple_spinner_item,
                        batchData["department"]!!,
                    )
                    batchDepartmentSpinner.setSelection(
                        batchData["department"]!!.indexOf(
                            LoginUserDataHolder.loginUserData?.get("department")
                        )
                    )
                    batchData["section"]!!.add(0, "Select Section")
                    batchSectionSpinner.adapter = ArrayAdapter(
                        this@AddNewBatch,
                        android.R.layout.simple_spinner_item,
                        batchData["section"]!!
                    )

                    if(LoginUserDataHolder.loginUserData?.get("role")=="admin") {
                        batchData["campus"]!!.add(0, "Select Campus")
                        batchCampusSpinner.adapter = ArrayAdapter(
                            this@AddNewBatch,
                            android.R.layout.simple_spinner_item,
                            batchData["campus"]!!
                        )
                    }
                }
            }

            override fun onFailure(
                call: Call<HashMap<String, ArrayList<String>>?>,
                t: Throwable
            ) {
                progressBar.stopAnimation()
                Toast.makeText(this@AddNewBatch, "Something went wrong", Toast.LENGTH_SHORT).show()
            }

        })
    }

    //set add button to create new batch
    fun setAddButtonToCreateBatch() {

        //set level for batch
        level1CreateBatch.setOnClickListener {
            var intent = Intent(this, LevelForBatch::class.java)
            intent.putExtra("levelType", "unlevel")
            startActivityForResult(intent, 1)
        }
        level2CreateBatch.setOnClickListener {
            var intent = Intent(this, LevelForBatch::class.java)
            intent.putExtra("levelType", "unlevel")
            startActivityForResult(intent, 2)
        }

        //add new batch
        add.setOnClickListener {
            if (level1Array.isEmpty() || level2Array.isEmpty()||batchYearSpinner.selectedItem.toString()=="Select Year"||batchDepartmentSpinner.selectedItem.toString()=="Select Department"||batchSectionSpinner.selectedItem.toString()=="Select Section") {
                Toast.makeText(this, "Please select all the required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(LoginUserDataHolder.loginUserData?.get("role")=="admin" && batchCampusSpinner.selectedItem.toString()=="Select Campus"){
                Toast.makeText(this, "Please select campus", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            var batchName = batchYearSpinner.selectedItem.toString() + "-" + batchDepartmentSpinner.selectedItem.toString() + "-" + batchSectionSpinner.selectedItem.toString()

            if(LoginUserDataHolder.loginUserData?.get("role")=="admin"){
                batchName=batchCampusSpinner.selectedItem.toString()+"-"+batchName
            }

            progressBar.startProgressBar()

            var newBatchData = BatchData(
                LoginUserDataHolder.token,
                batchName,
                level1Array,
                level2Array
            )

            CoroutineScope(Dispatchers.IO).launch {
                var callToAddNewBatch = RetrofitClient.instance.addNewBatch(newBatchData)

                callToAddNewBatch.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        progressBar.stopAnimation()
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@AddNewBatch,
                                "Batch added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            var errorMessage = LoginUserDataHolder.getErrorMessage(response)

                            // Display the extracted or fallback error message in a long toast
                            Toast.makeText(this@AddNewBatch, errorMessage, Toast.LENGTH_LONG)
                                .show()
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseBody?>,
                        t: Throwable
                    ) {
                        Toast.makeText(this@AddNewBatch, "Something went wrong", Toast.LENGTH_SHORT)
                            .show()
                    }

                })
            }
        }
    }


    //fetch level data
    fun fetchLevelData() {

        progressBar.startProgressBar()

        CoroutineScope(Dispatchers.IO).launch {
            var callToGetLeveledMember = RetrofitClient.instance.getLeveledMember(
                hashMapOf(
                    "token" to LoginUserDataHolder.token,
                    "batchName" to intent.getStringExtra("batchName")!!
                )
            )

            callToGetLeveledMember.enqueue(object : Callback<HashMap<String, ArrayList<HashMap<String, String>>>> {
                override fun onResponse(
                    call: Call<HashMap<String, ArrayList<HashMap<String, String>>>?>,
                    response: Response<HashMap<String, ArrayList<HashMap<String, String>>>?>
                ) {
                    progressBar.stopAnimation()
                    if (response.isSuccessful) {
                        var levelData = response.body()!!
                        var level1Data = levelData["level1"]!!
                        var level2Data = levelData["level2"]!!
                        level1RecyclerView.adapter = LevelAdapter(level1Data)
                        level2RecyclerView.adapter = LevelAdapter(level2Data)

                        //get the email of level1 member in array
                        level1Array = getEmail(level1Data)
                        level2Array = getEmail(level2Data)

                    }
                    else{
                        var errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        // Display the extracted or fallback error message in a long toast
                        Toast.makeText(this@AddNewBatch, errorMessage, Toast.LENGTH_LONG)
                            .show()
                    }
                }

                override fun onFailure(
                    call: Call<HashMap<String, ArrayList<HashMap<String, String>>>?>,
                    t: Throwable
                ) {
                    progressBar.stopAnimation()
                   Toast.makeText(this@AddNewBatch, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            })

        }


    }

    //set add button to edit batch
    fun setAddButtonToEditBatch() {
        add.setOnClickListener {
            //to remove batch
            if(add.text=="Remove"){
                progressBar.startProgressBar()
                CoroutineScope(Dispatchers.IO).launch {
                    var callToRemoveBatch=RetrofitClient.instance.removeBatch(
                        hashMapOf(
                            "token" to LoginUserDataHolder.token,
                            "batchName" to intent.getStringExtra("batchName")!!,
                            "campus" to LoginUserDataHolder.campusForBatchOperation
                        )
                    )

                    callToRemoveBatch.enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(
                            call: Call<ResponseBody?>,
                            response: Response<ResponseBody?>
                        ) {
                            progressBar.stopAnimation()
                            if(response.isSuccessful){
                                Toast.makeText(this@AddNewBatch, "Batch removed successfully", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            else{
                                var errorMessage = LoginUserDataHolder.getErrorMessage(response)

                                // Display the extracted or fallback error message in a long toast
                                Toast.makeText(this@AddNewBatch, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<ResponseBody?>,
                            t: Throwable
                        ) {
                            progressBar.stopAnimation()
                            Toast.makeText(this@AddNewBatch, "Something went wrong", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            //batch editing done
            else{
                if(level1Array.isEmpty() || level2Array.isEmpty()){
                    Toast.makeText(this, "Please select level", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                progressBar.startProgressBar()
                CoroutineScope(Dispatchers.IO).launch {
                    //edit batch
                    var batchData=BatchData(
                        LoginUserDataHolder.token,
                        intent.getStringExtra("batchName")!!,
                        level1Array,
                        level2Array
                    )
                    var callToEditBatch=RetrofitClient.instance.editBatch(batchData)

                   callToEditBatch.enqueue(object : Callback<ResponseBody>{
                       override fun onResponse(
                           call: Call<ResponseBody?>,
                           response: Response<ResponseBody?>
                       ) {
                           progressBar.stopAnimation()
                           if(response.isSuccessful){
                               Toast.makeText(this@AddNewBatch, "Batch edited successfully", Toast.LENGTH_SHORT).show()
                               add.text="Remove"
                               editButton.visibility=ImageView.VISIBLE
                               level1CreateBatch.isEnabled=false
                               level2CreateBatch.isEnabled=false
                           }
                           else{
                               var errorMessage = LoginUserDataHolder.getErrorMessage(response)
                               // Display the extracted or fallback error message in a long toast
                               Toast.makeText(this@AddNewBatch, errorMessage, Toast.LENGTH_LONG).show()
                           }
                       }

                       override fun onFailure(
                           call: Call<ResponseBody?>,
                           t: Throwable
                       ) {
                           progressBar.stopAnimation()
                           Toast.makeText(this@AddNewBatch, "Something went wrong", Toast.LENGTH_SHORT).show()
                       }

                   })
                }

            }
        }
    }

    //setup edit level button
    fun setEditLevelButton(){
        level1CreateBatch.setOnClickListener {
            var intent=Intent(this,LevelForBatch::class.java)
            intent.putExtra("levelType","level")
            intent.putExtra("levelData",level1Array)
            startActivityForResult(intent,1)
        }
        level2CreateBatch.setOnClickListener {
            var intent=Intent(this,LevelForBatch::class.java)
            intent.putExtra("levelType","level")
            intent.putExtra("levelData",level2Array)
            startActivityForResult(intent,2)
        }

    }

}