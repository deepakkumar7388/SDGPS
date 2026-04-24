package com.example.digitalpass

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

class GatePassDetail : AppCompatActivity() {
    lateinit var reason: EditText
    lateinit var otherInfoDescription: TextView
    lateinit var gatePass:HashMap<String,String>
    lateinit var tgRemark:EditText
    lateinit var approve: MaterialButton

    private var progressBar: CustomProgressBar?=null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gate_pass_detail)
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

        var img=findViewById<ImageView>(R.id.imageView)
        var name=findViewById<TextView>(R.id.name)
        reason=findViewById(R.id.reason)
        var status=findViewById<TextView>(R.id.status)
        var gatePassId=findViewById<TextView>(R.id.gatePassId)
        var department=findViewById<TextView>(R.id.department)
        var phone=findViewById<TextView>(R.id.phone)
        var otherInfo=findViewById<TextView>(R.id.otherInformation)
        otherInfoDescription=findViewById(R.id.otherInformationDescription)
        var callIcon=findViewById<MaterialButton>(R.id.callIcon)
        var editButton=findViewById<ImageView>(R.id.editButton)
        var reject=findViewById<MaterialButton>(R.id.rejectButton)
        approve=findViewById(R.id.approveButton)
        var tgRemarkLayout=findViewById<TextInputLayout>(R.id.tgRemarkInputLayout)
        tgRemark=findViewById(R.id.tgRemark)

        gatePass= (intent.getSerializableExtra("gatePass") as? HashMap<String,String>)!!

        //the user can not do any operation when status is not (pending or approving) ,also can not do any thing when apply date is not equal to current date
        if(gatePass["status"]!="pending"&&gatePass["status"]!="approving"||checkDate()){
            reject.visibility= View.GONE
            approve.visibility=View.GONE
            editButton.visibility=View.GONE
        }

        if(intent.getStringExtra("operationType")=="self"){
            reject.visibility= View.GONE
            callIcon.visibility=View.GONE

            if(gatePass["status"]=="pending"){
                approve.text="Remove"

                //setup approve button for self user
                setupApproveButtonForSelfUser()
            }
            else{
                approve.visibility=View.GONE
                editButton.visibility=View.GONE
            }

        }
        else{
            if(LoginUserDataHolder.loginUserData?.get("role")=="security guard"){
                approve.visibility=View.VISIBLE
                approve.text="Exit"
                approve.setOnClickListener {
                    approveTheGatePass(hashMapOf(
                        "token" to LoginUserDataHolder.token,
                        "gatePassId" to gatePass["gatePassId"]!!
                    ))
                }
            }
            else {
                reject.setOnClickListener {
                    rejectGatePass()
                }
                approve.setOnClickListener {
                    if (approve.text == "Approve") approveGatePass()
                    else editGatePass()
                }
            }
        }

        editButton.setOnClickListener {
            approve.text="Done"
            editButton.visibility=View.GONE
            reason.isEnabled=true
            reject.visibility=View.GONE

            if(intent.getStringExtra("operationType")=="member")tgRemark.isEnabled=true
        }

        //setup user data
        if(gatePass["img"]?.trim()!="")
            Glide.with(this).load(LoginUserDataHolder.getURL(gatePass["img"])).into(img)
        name.text=gatePass["name"]
        status.text=gatePass["status"]
        gatePassId.text=gatePass["gatePassId"]
        department.text=gatePass["department"]+"  "+gatePass["role"]
        phone.text=gatePass["phone"]
        reason.setText(gatePass["reason"])

        //get other info description
        getOtherInfoDescription()

        otherInfo.setOnClickListener {
            if(otherInfoDescription.visibility==View.GONE)otherInfoDescription.visibility=View.VISIBLE
            else otherInfoDescription.visibility=View.GONE
        }

        //check there is any tgRemark key in gatePass
        if(gatePass.containsKey("tgRemark")){
            tgRemarkLayout.visibility=View.VISIBLE
            tgRemark.setText(gatePass["tgRemark"])
        }



        //setup gate pass for history
        if(intent.getStringExtra("listType")=="history"){
            reject.visibility= View.GONE
            approve.visibility=View.GONE
            editButton.visibility=View.GONE
        }

    }

    private fun getOtherInfoDescription(){
        var descriptionString="Campus : ${gatePass["campus"]}"

        if(gatePass["role"]=="student"&&gatePass["applyEmail"]!=LoginUserDataHolder.loginUserData?.get("email")){
            descriptionString=descriptionString+"\n UID : ${gatePass["uid"]}"+"\n Batch : ${gatePass["batch"]}"+
                    "\n Father Name : ${gatePass["fathername"]}"+"\n Father Phone : ${gatePass["fatherphone"]}"
        }
        descriptionString=descriptionString+"\n Apply Date : ${gatePass["applyDate"]}"
        if(gatePass["remark"]?.trim()!="")descriptionString=descriptionString+"\n Remarks : ${gatePass["remark"]}"

        otherInfoDescription.text=descriptionString
    }

    private fun setupApproveButtonForSelfUser(){
        approve.setOnClickListener {
            if(approve.text=="Remove"){
                progressBar?.startProgressBar()
                var callToRemove= RetrofitClient.instance.removeGatePassBySelfUser(
                    hashMapOf(
                        "token" to LoginUserDataHolder.token,
                        "gatePassId" to gatePass["gatePassId"]!!
                    )
                )
                callToRemove.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {

                        progressBar?.stopAnimation()
                        if(response.isSuccessful){
                            Toast.makeText(this@GatePassDetail,"Gate Pass Removed Successfully",Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        else Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(
                        call: Call<ResponseBody?>,
                        t: Throwable
                    ) {
                        progressBar?.stopAnimation()
                        Toast.makeText(this@GatePassDetail, "Something went wrong",Toast.LENGTH_SHORT).show()
                    }
                })
            }
            else editSelfUserGatePass()
        }
    }

    private fun editSelfUserGatePass(){
        //check is there any changes in reason
        if(gatePass["reason"]!=reason.text.toString()){
            if(reason.text.isBlank()) {
                Toast.makeText(this, "Please enter reason", Toast.LENGTH_SHORT).show()
                return
            }
            progressBar?.startProgressBar()
            var callToEdit= RetrofitClient.instance.editGatePassBySelfUser(hashMapOf(
                "token" to LoginUserDataHolder.token,
                "reason" to reason.text.toString(),
                "gatePassId" to gatePass["gatePassId"]!!
            ))
            callToEdit.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {

                    progressBar?.stopAnimation()
                    if(response.isSuccessful){
                        Toast.makeText(this@GatePassDetail,"Gate Pass Edited Successfully",Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    else Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(
                    call: Call<ResponseBody?>,
                    t: Throwable
                ) {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@GatePassDetail, "Something went wrong",Toast.LENGTH_SHORT).show()
                }
            })

        }
        else{
            Toast.makeText(this,"No changes made",Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDate(): Boolean{
        var gatePassDateString=gatePass["applyDate"]!!.split(" ")[0].trim()
        var gatePassDate= LocalDate.parse(gatePassDateString)
        var currentDate=LocalDate.now()
        if(gatePassDate.isEqual(currentDate))return false
        return true
    }


    private fun rejectGatePass(){
        progressBar?.startProgressBar()
        var callToReject= RetrofitClient.instance.rejectGatePass(hashMapOf(
            "token" to LoginUserDataHolder.token,
            "gatePassId" to gatePass["gatePassId"]!!
        ))
        callToReject.enqueue(object: Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                progressBar?.stopAnimation()
                if(response.isSuccessful){
                    Toast.makeText(this@GatePassDetail,"Gate Pass Rejected Successfully",Toast.LENGTH_SHORT).show()
                    finish()
                }
                else Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                progressBar?.stopAnimation()
                Toast.makeText(this@GatePassDetail, "Something went wrong",Toast.LENGTH_SHORT)
            }
        })
    }

    private fun approveGatePass(){
        //we have check is there any tgRemark available or not ,if there is no tgRemark so first we have to take tgRemark then we will give approval
        if(gatePass.containsKey("tgRemark")){
            approveTheGatePass(hashMapOf(
                "token" to LoginUserDataHolder.token,
                "gatePassId" to gatePass["gatePassId"]!!,
            ))
        }
        else{
            showDialogToGetTGRemark()
        }
    }

    private fun showDialogToGetTGRemark(){
        var dialogView=layoutInflater.inflate(R.layout.show_dialog_to_give_aproval_visitor,null)
        var dialog= MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialog.show()

        var remarkOfDialog=dialogView.findViewById<EditText>(R.id.remark)

        dialogView.findViewById<Button>(R.id.remarkDoneButton).setOnClickListener {
            if(remarkOfDialog.text.toString().trim()==""){
                Toast.makeText(this,"Please enter remark",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            approveTheGatePass(hashMapOf(
                "token" to LoginUserDataHolder.token,
                "gatePassId" to gatePass["gatePassId"]!!,
                "tgRemark" to remarkOfDialog.text.toString().trim()
            ))
            dialog.dismiss()
        }

    }

    private fun approveTheGatePass(dataForApproval: HashMap<String, String>){
        progressBar?.startProgressBar()
        var callToGiveApproval= RetrofitClient.instance.approveGatePass(dataForApproval)
        callToGiveApproval.enqueue(object: Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                progressBar?.stopAnimation()
                if(response.isSuccessful) {
                    if(LoginUserDataHolder.loginUserData?.get("role")=="security guard")
                        Toast.makeText(this@GatePassDetail,"Exited Successfully",Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this@GatePassDetail,"Gate Pass Approved Successfully",Toast.LENGTH_SHORT).show()

                    finish()
                }
                else Toast.makeText(
                    this@GatePassDetail,
                    LoginUserDataHolder.getErrorMessage(response),
                    Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                progressBar?.stopAnimation()
                Toast.makeText(this@GatePassDetail, "Something went wrong",Toast.LENGTH_SHORT)
            }

        })
    }

    private fun editGatePass(){

        if(reason.text.toString().trim()==""||(gatePass.containsKey("tgRemark")&&tgRemark.text.toString().trim()=="")){
            Toast.makeText(this,"Please enter the required field",Toast.LENGTH_SHORT).show()
            return
        }

        var hashToEditGatePass = hashMapOf<String, String>()
        //first check there is any changes made in reason for gate pass and tgRemark
        if (gatePass["reason"] != reason.text.toString().trim()) hashToEditGatePass["reason"] = reason.text.toString().trim()
        if (gatePass["tgRemark"] != tgRemark.text.toString().trim()) hashToEditGatePass["tgRemark"] = tgRemark.text.toString().trim()

        if (hashToEditGatePass.isEmpty()) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show()
            finish()
        }
        else {

            progressBar?.startProgressBar()

            CoroutineScope(Dispatchers.IO).launch {

                hashToEditGatePass["token"] = LoginUserDataHolder.token
                hashToEditGatePass["gatePassId"] = gatePass["gatePassId"] as String

                var callToEditGatePass = RetrofitClient.instance.editGatePass(hashToEditGatePass)
                callToEditGatePass.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        progressBar?.stopAnimation()
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@GatePassDetail,
                                "Gate Pass Edited Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else Toast.makeText(
                            this@GatePassDetail,
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
                            this@GatePassDetail,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })

            }
        }
    }

}