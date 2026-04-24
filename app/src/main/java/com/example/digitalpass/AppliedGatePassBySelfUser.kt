package com.example.digitalpass

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppliedGatePassBySelfUser : AppCompatActivity() {
    private lateinit var adapter: RecentPassAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    //make lambda function to add common data in each gate pass like img,name,department,role,phone,campus,applyEmail of this user
    private var getCommonData={gatePass:HashMap<String,String>->
        gatePass["img"]=LoginUserDataHolder.loginUserData?.get("img")?:""
        gatePass["name"]=LoginUserDataHolder.loginUserData?.get("name")?:""
        gatePass["applyEmail"]=LoginUserDataHolder.loginUserData?.get("email")?:""
        gatePass["department"]=LoginUserDataHolder.loginUserData?.get("department")?:""
        gatePass["campus"]=LoginUserDataHolder.loginUserData?.get("campus")?:""
        gatePass["role"]=LoginUserDataHolder.loginUserData?.get("role")?:""
        gatePass["phone"]=LoginUserDataHolder.loginUserData?.get("phone")?:""
        gatePass
    }

    private var progressBar: CustomProgressBar?=null

    private var gatePassList= arrayListOf<HashMap<String,String>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_applied_gate_pass_by_self_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            getGatePass()
        }

        progressBar=findViewById(R.id.customProgressBar)

        var applyButton=findViewById<Button>(R.id.applyGatePassButton)
        var recyclerView=findViewById<RecyclerView>(R.id.gatePassRecyclerView)
        recyclerView.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter= RecentPassAdapter("selfGatePass",ArrayList())
        recyclerView.adapter=adapter

        //get all gate pass
        getGatePass()

        applyButton.setOnClickListener {
            showDialogueToGetReason()
        }

    }

    private fun showDialogueToGetReason() {
        val dialogView = layoutInflater.inflate(R.layout.show_dialog_to_give_aproval_visitor, null)
        var dialogApplyButton=dialogView.findViewById<Button>(R.id.remarkDoneButton)
        var reason=dialogView.findViewById<EditText>(R.id.remark)

        //setup text of button hint for applying gate pass
        dialogApplyButton.text="Apply"

        dialogView.findViewById<TextInputLayout>(R.id.nameInputLayout).hint = "Reason for gate pass"

        var dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialog.show()

        dialogApplyButton.setOnClickListener {
            if(reason.text.toString().trim()==""){
                Toast.makeText(this,"Please enter reason",Toast.LENGTH_SHORT).show()
            }else{
                applyForGatePass(reason.text.toString().trim())
                dialog.dismiss()
            }
        }

    }

    private fun applyForGatePass(reason: String) {

        progressBar?.startProgressBar()
        var callToApplyGatePass= RetrofitClient.instance.applyForGatePass(
            hashMapOf(
                "reason" to reason,
                "token" to LoginUserDataHolder.token
            )
        )
        callToApplyGatePass.enqueue(object: Callback<HashMap<String,String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                progressBar?.stopAnimation()
                if(response.isSuccessful){
                    Toast.makeText(this@AppliedGatePassBySelfUser,"Gate pass applied successfully",Toast.LENGTH_SHORT).show()
                    //also add some common information here by using lambda function

                    gatePassList.add(0,getCommonData(response.body()!!))
                    adapter.updateList(gatePassList)

                }else{
                    Toast.makeText(this@AppliedGatePassBySelfUser,LoginUserDataHolder.getErrorMessage(response),Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(
                call: Call<HashMap<String, String>?>,
                t: Throwable
            ) {

                progressBar?.stopAnimation()
                Toast.makeText(this@AppliedGatePassBySelfUser,"Something went wrong",Toast.LENGTH_SHORT).show()
            }
        })


    }


    private fun getGatePass(){
        if (!swipeRefreshLayout.isRefreshing) {
            progressBar?.startProgressBar()
        }
        var callToGetRecentSelfUserGatePass= RetrofitClient.instance.getSelfUserGatePass(LoginUserDataHolder.token)
        callToGetRecentSelfUserGatePass.enqueue(object: Callback<ArrayList<HashMap<String,String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, String>>?>,
                response: Response<ArrayList<HashMap<String, String>>?>
            ) {
                progressBar?.stopAnimation()
                swipeRefreshLayout.isRefreshing = false
                if(response.isSuccessful) {
                    gatePassList.clear()
                    //we have to add some common information in each gate pass from loginUserData by using lambda function
                    for (gatePass in response.body()!!) {
                        gatePassList.add(getCommonData(gatePass))
                    }

                    adapter.updateList(gatePassList)
                }
            }

            override fun onFailure(
                call: Call<ArrayList<HashMap<String, String>>?>,
                t: Throwable
            ) {
                progressBar?.stopAnimation()
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@AppliedGatePassBySelfUser,"Something went wrong",Toast.LENGTH_SHORT).show()
            }

        })

    }
}
