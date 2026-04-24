package com.example.digitalpass

import android.content.Intent
import android.graphics.Bitmap
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.isGone
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class EnterVisitor : AppCompatActivity() {
    lateinit var image: ImageView
    lateinit var editButton: ImageView
    lateinit var name: EditText
    lateinit var phone: EditText
    lateinit var email: EditText
    lateinit var numberOfVisitor: EditText
    lateinit var reason: EditText
    lateinit var departmentSpinner: Spinner
    lateinit var search: SearchView
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: LevelAdapter

    var memberList:ArrayList<HashMap<String,String>> = ArrayList()
    var selectedMember:HashMap<String,String>?=null

    var multipartImage:MultipartBody.Part?=null

    private lateinit var enterVisitorButton:Button
    var visitorData: HashMap<String,String>?=null
    lateinit var imageText: TextView
    lateinit var unselectMemberText: TextView
    lateinit var otherInfo: MaterialButton
    lateinit var otherInfoDescription: TextView

    private var progressBar:CustomProgressBar?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep this, but note it acts as a hint when Edge-to-Edge is on
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        enableEdgeToEdge()
        setContentView(R.layout.activity_enter_visitor)

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


        var cameraLauncher=registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap?->
            if (bitmap==null) return@registerForActivityResult
            Toast.makeText(this, "Image loading...", Toast.LENGTH_SHORT).show()
            image.setImageBitmap(bitmap)
            loadBitmapAndTakeMultipart(bitmap)
        }

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        progressBar=findViewById(R.id.customProgressBar)
        image=findViewById(R.id.visitorSecurityEnterImg)
        imageText=findViewById(R.id.tapToCaptureHint)
        editButton=findViewById(R.id.editButtonBySecurity)
        name=findViewById(R.id.visitorName)
        phone=findViewById(R.id.visitorPhone)
        email=findViewById(R.id.visitorEmail)
        numberOfVisitor=findViewById(R.id.visitorNumber)
        reason=findViewById(R.id.visitorReason)
        departmentSpinner=findViewById(R.id.departmentSpinner)
        unselectMemberText=findViewById(R.id.unselectMemberText)
        search=findViewById(R.id.memberSearchToSelectMember)

        otherInfo=findViewById(R.id.otherInformation)
        otherInfoDescription=findViewById(R.id.otherInformationDescription)

        recyclerView=findViewById(R.id.memberSearchBySecurityRecyclerView)
        recyclerView.layoutManager= LinearLayoutManager(this)
        adapter= LevelAdapter(memberList)
        adapter.adapterForVisitor=true
        recyclerView.adapter=adapter

         enterVisitorButton=findViewById(R.id.enterVisitorButton)


        image.setOnClickListener {
            cameraLauncher.launch(null)
        }

        if(intent.getStringExtra("operation")=="enter")toEnterVisitor()
        else{
            try {
                visitorData=if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {intent.getSerializableExtra("visitor", HashMap::class.java) as? HashMap<String, String>
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("visitor") as? HashMap<String, String>
                }
            }
            catch (e:Exception){
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
            if(visitorData!=null)toEditVisitor()
            else Toast.makeText(this, "null data", Toast.LENGTH_SHORT).show()
        }
        getMemberToMeetVisitor()

        unselectMemberText.setOnClickListener {
            selectedMember = null
            adapter.updateList(memberList)
            unselectMemberText.visibility=View.GONE
            filterWithQueryAndDepartment(search.query.toString(),departmentSpinner.selectedItem.toString())
        }

        otherInfo.setOnClickListener {
            if(otherInfoDescription.isVisible) otherInfoDescription.visibility=View.GONE
            else otherInfoDescription.visibility=View.VISIBLE
        }

        //setup enterVisitor for history
        if(intent.getStringExtra("listType")=="history"){
            editButton.visibility=View.GONE
            enterVisitorButton.visibility=View.GONE
        }

    }

    private fun getMemberToMeetVisitor(){
        progressBar?.startProgressBar()
            var call=RetrofitClient.instance.getAllMemberForVisitor(LoginUserDataHolder.token)
            call.enqueue(object : Callback<ArrayList<HashMap<String,String>>>{
                override fun onResponse(
                    call: Call<ArrayList<HashMap<String, String>>?>,
                    response: Response<ArrayList<HashMap<String, String>>?>
                ) {
                    progressBar?.stopAnimation()
                    if (response.isSuccessful){
                        memberList=response.body()!!
                        if(visitorData==null)adapter.updateList(memberList)
                        else{
                            //we have to find only one user from memberList where member email==visitorData meetEmail
                            for (member in memberList) {
                                if (member["email"]==visitorData!!["meetEmail"]){
                                    selectedMember=member
                                    break
                                }
                            }
                            //if selected member is null then we will set default first member
                            if(selectedMember==null){
                                if(memberList.isNotEmpty())selectedMember= memberList[0]
                            }
                            //update adapter with selectedMember
                         adapter.updateList(arrayListOf(selectedMember) as ArrayList<HashMap<String, String>>)

                        }
                        
                        //setup department spinner
                        setupDepartmentSpinner()
                    }
                    else Toast.makeText(this@EnterVisitor, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(
                    call: Call<ArrayList<HashMap<String, String>>?>,
                    t: Throwable
                ) {
                    Toast.makeText(this@EnterVisitor, "Something went wrong", Toast.LENGTH_SHORT).show()
                }

            })

    }

    private fun setupDepartmentSpinner(){
            var departmentList = ArrayList<String>()
            departmentList.add("All Department")
        //get all unique department from memberList
            for (member in memberList) {
                if (!departmentList.contains(member["department"])) {
                    departmentList.add(member["department"]!!)
                }
            }
            departmentSpinner.adapter = ArrayAdapter(
                this@EnterVisitor,
                android.R.layout.simple_spinner_item,
                departmentList)

            setupFilter()
    }

    private fun setupFilter() {
        departmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                filterWithQueryAndDepartment(search.query.toString(),departmentSpinner.selectedItem.toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                filterWithQueryAndDepartment(search.query.toString(),departmentSpinner.selectedItem.toString())
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                filterWithQueryAndDepartment(search.query.toString(),departmentSpinner.selectedItem.toString())
                return true
            }
        })

        }

    private fun filterWithQueryAndDepartment(query:String,department:String){

        if(selectedMember!=null)return

        var filteredList=when{
            department=="All Department"->memberList
            else -> memberList.filter { it["department"]==department } as ArrayList<HashMap<String, String>>
        }

        filteredList=filteredList.filter { it["name"]!!.contains(query,ignoreCase = true) } as ArrayList<HashMap<String, String>>
        adapter.updateList(filteredList)
    }

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 1 && resultCode == RESULT_OK) {
         selectedMember = data?.getSerializableExtra("member") as? HashMap<String, String>
        if (selectedMember != null) {
            adapter.updateList(arrayListOf(selectedMember) as ArrayList<HashMap<String, String>>)
            if(editButton.isGone)unselectMemberText.visibility = View.VISIBLE
        }
    }
}

    private fun loadBitmapAndTakeMultipart(bitmap:Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            //compress the image until its size become <200KB
            var quality = 100
            var stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

            while (stream.toByteArray().size / 1024 > 200 && quality > 10) {
                stream.reset()
                quality = if (quality < 30) 5 else 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }

            var file = File(cacheDir, "img${System.currentTimeMillis()}.jpg")
            file.writeBytes(stream.toByteArray())

            //convert this file into multipart
            var requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

            multipartImage = MultipartBody.Part.createFormData("img", "img.jpg", requestFile)

            runOnUiThread {
                Toast.makeText(this@EnterVisitor, "Image loaded", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun toEnterVisitor(){
        //gone the visibility of otherInfo
        otherInfo.visibility=View.GONE

        enterVisitorButton.setOnClickListener {
            //check all the fields are filled
            if(multipartImage==null||
                name.text.toString()==""||
                phone.text.toString()==""||
                email.text.toString()==""||
                numberOfVisitor.text.toString()==""||
                reason.text.toString()==""||
                selectedMember==null){
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar?.startProgressBar()
            var visitor=hashMapOf(
                "name" to name.text.toString(),
                "phone" to phone.text.toString(),
                "visitorEmail" to email.text.toString(),
                "numberOfVisitor" to numberOfVisitor.text.toString(),
                "reason" to reason.text.toString(),
                "meetDepartment" to selectedMember!!["department"]!!,
                "meetEmail" to selectedMember!!["email"]!!,
            )

            //convert this visitor in json object
            var jsonObject= JSONObject(visitor as Map<*, *>).toString()
            //convert this visitor and token in requestBody
            var requestVisitor=jsonObject.toRequestBody("application/json".toMediaTypeOrNull())
            var requestToken=LoginUserDataHolder.token.toRequestBody("text/plain".toMediaTypeOrNull())

            var callToEnterVisitor= RetrofitClient.instance.enterVisitor(requestVisitor,requestToken,multipartImage!!)
            callToEnterVisitor.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    progressBar?.stopAnimation()
                    if(response.isSuccessful){
                        Toast.makeText(this@EnterVisitor, "Visitor entered successfully", Toast.LENGTH_SHORT).show()
                        finish()
                }
                    else Toast.makeText(this@EnterVisitor,LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(
                    call: Call<ResponseBody?>,
                    t: Throwable
                ) {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@EnterVisitor, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    private fun toEditVisitor(){
        editButton.visibility=View.VISIBLE
        imageText.visibility=View.GONE
        //setup enterVisitorButton text
        setupEnterVisitorButtonText()

        //setup other info description
        setupOtherInfoDescription()
        
        //setup visitor data

        if(visitorData?.get("img") !="") Glide.with(this).load(LoginUserDataHolder.getURL(visitorData?.get("img"))).into(image)
        name.setText(visitorData?.get("name"))
        phone.setText(visitorData?.get("phone"))
        email.setText(visitorData?.get("visitorEmail"))
        numberOfVisitor.setText(visitorData?.get("numberOfVisitor"))
        reason.setText(visitorData?.get("reason"))

        //show metaData
        findViewById<MaterialCardView>(R.id.metadataCard).visibility=View.VISIBLE
        findViewById<TextView>(R.id.visitorIdText).text=visitorData?.get("visitorId")
        findViewById<TextView>(R.id.visitorStatus).text=visitorData?.get("status")
        findViewById<TextView>(R.id.campusText).text=visitorData?.get("campus")
        findViewById<TextView>(R.id.entryDateText).text=visitorData?.get("entryDate")

        setupEnableView(false)

        //if status is meet disable edit button
        if(visitorData?.get("status")=="meet"){
            editButton.visibility=View.INVISIBLE
            if(LoginUserDataHolder.loginUserData?.get("role")!="security guard")enterVisitorButton.visibility=View.GONE
        }

        editButton.setOnClickListener {
            editButton.visibility=View.GONE
            imageText.visibility=View.VISIBLE
            unselectMemberText.visibility=View.VISIBLE
            enterVisitorButton.text="Done"
            setupEnableView(true)
        }

        enterVisitorButton.setOnClickListener {
            if(enterVisitorButton.text=="Done")setupEnterVisitorButtonToEdit()
            else setupEnterVisitorButtonToApprove()
        }
    }


    private fun setupEnableView(viewType:Boolean){
        search.isEnabled=viewType
        departmentSpinner.isEnabled=viewType
        image.isEnabled=viewType
        name.isEnabled=viewType
        phone.isEnabled=viewType
        email.isEnabled=viewType
        numberOfVisitor.isEnabled=viewType
        reason.isEnabled=viewType
    }

    private fun setupEnterVisitorButtonText(){
        if(LoginUserDataHolder.loginUserData?.get("role")=="security guard")enterVisitorButton.text="Exit"
        else enterVisitorButton.text="Meet"
    }

    private fun setupOtherInfoDescription(){
        //leave the information which are already used in are view
        var descriptionString=""
        //add lastUpdatedBy and remark in description if it exist
        if(visitorData?.containsKey("lastUpdatedBy")!!)descriptionString+="Last Updated By: ${visitorData?.get("lastUpdatedBy")}\n"
        if(visitorData?.containsKey("remark")!!)descriptionString+="Remark: ${visitorData?.get("remark")}\n"
        otherInfoDescription.text=descriptionString
    }

    private fun setupEnterVisitorButtonToApprove(){
            if (LoginUserDataHolder.loginUserData?.get("email") == visitorData?.get("meetEmail")||
                (LoginUserDataHolder.loginUserData?.get("role")=="security guard"&& visitorData?.get("status")=="meet")) {
                meetVisitor(hashMapOf(
                    "visitorId" to visitorData!!["visitorId"]!!,
                    "token" to LoginUserDataHolder.token
                ))
            }
            else showDialogToGetReason()
    }
    private fun setupEnterVisitorButtonToEdit(){

            if(name.text.toString()==""||
                phone.text.toString()==""||
                email.text.toString()==""||
                numberOfVisitor.text.toString()==""||
                reason.text.toString()==""||
                selectedMember==null){
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
                return
            }

            //only we have to send changed data if there any changes
            var changedVisitorHashMap=hashMapOf<String,String>()

            if(name.text.toString()!=visitorData!!["name"])changedVisitorHashMap["name"]=name.text.toString()
            if(phone.text.toString()!=visitorData!!["phone"])changedVisitorHashMap["phone"]=phone.text.toString()
            if(email.text.toString()!=visitorData!!["visitorEmail"])changedVisitorHashMap["visitorEmail"]=email.text.toString()
            if(numberOfVisitor.text.toString()!=visitorData!!["numberOfVisitor"])changedVisitorHashMap["numberOfVisitor"]=numberOfVisitor.text.toString()
            if(reason.text.toString()!=visitorData!!["reason"])changedVisitorHashMap["reason"]=reason.text.toString()
            if(selectedMember!!["email"]!=visitorData!!["meetEmail"])changedVisitorHashMap["meetEmail"]=selectedMember!!["email"]!!
            if(selectedMember!!["department"]!=visitorData!!["meetDepartment"])changedVisitorHashMap["meetDepartment"]=selectedMember!!["department"]!!

            //if nothing is to be change
            if(changedVisitorHashMap.isEmpty() && multipartImage==null){
                Toast.makeText(this, "Visitor edited successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        else {

                progressBar?.startProgressBar()
                changedVisitorHashMap["visitorId"] = visitorData!!["visitorId"].toString()
                changedVisitorHashMap["token"] = LoginUserDataHolder.token

                var jsonObject = JSONObject(changedVisitorHashMap as Map<*, *>).toString()
                var requestVisitor =
                    jsonObject.toRequestBody("application/json".toMediaTypeOrNull())

                var callToEditVisitor =
                    RetrofitClient.instance.editVisitor(requestVisitor, multipartImage)
                callToEditVisitor.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        progressBar?.stopAnimation()
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@EnterVisitor,
                                "Visitor edited successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        else Toast.makeText(
                            this@EnterVisitor,
                            LoginUserDataHolder.getErrorMessage(response),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(
                        call: Call<ResponseBody?>,
                        t: Throwable
                    ) {
                        progressBar?.stopAnimation()
                        Toast.makeText(
                            this@EnterVisitor,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
            }

    }


    private fun showDialogToGetReason() {
        // 1. Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.show_dialog_to_give_aproval_visitor, null)

        // 2. Find views inside the inflated dialogView (NOT the dialog itself yet)
        // Assuming 'remark' is an EditText. Change to TextInputEditText if using Material components.
        val enteredRemark = dialogView.findViewById<EditText>(R.id.remark)
        val doneButton = dialogView.findViewById<Button>(R.id.remarkDoneButton)

        // 3. Build and show the dialog
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // 4. Set the click listener on the button inside the dialog
        doneButton?.setOnClickListener {
            val remarkText = enteredRemark?.text.toString()

            if (remarkText.trim().isEmpty()) {
                Toast.makeText(this, "Please enter your remark", Toast.LENGTH_SHORT).show()
            } else {
                // Prepare data and call the meetVisitor API
                val meetData = hashMapOf(
                    "visitorId" to visitorData!!["visitorId"]!!,
                    "token" to LoginUserDataHolder.token,
                    "remark" to remarkText
                )
                meetVisitor(meetData)
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun meetVisitor(meetData: HashMap<String, String>){

        progressBar?.startProgressBar()
        var callToMeetVisitor= RetrofitClient.instance.meetVisitor(meetData)
        callToMeetVisitor.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                progressBar?.stopAnimation()
                if(response.isSuccessful){
                    if(LoginUserDataHolder.loginUserData?.get("role")=="security guard")Toast.makeText(this@EnterVisitor, "Visitor exited successfully", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this@EnterVisitor, "Visitor met successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else Toast.makeText(this@EnterVisitor, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()

            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                Toast.makeText(this@EnterVisitor, "Something went wrong", Toast.LENGTH_SHORT).show()
            }

        })
    }

}