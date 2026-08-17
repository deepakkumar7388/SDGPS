package com.example.digitalpass

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.ui.GatePassDetailScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

class GatePassDetail : BaseActivity() {

    lateinit var gatePass: HashMap<String, String>
    private var gatePassState = mutableStateOf<Map<String, String>>(emptyMap())
    private var isLoading = mutableStateOf(false)
    private var previousPasses = mutableStateOf<List<Map<String, String>>>(emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawGatePass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("gatePass", HashMap::class.java) as? HashMap<String, String>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("gatePass") as? HashMap<String, String>
        }

        if (rawGatePass == null) {
            Toast.makeText(this, "Failed to load gate pass data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        gatePass = rawGatePass
        gatePassState.value = gatePass

        val rawOperationType = intent.getStringExtra("operationType") ?: "member"
        val userEmail = LoginUserDataHolder.loginUserData?.get("email") ?: ""
        val passEmail = gatePass["email"] ?: gatePass["applyEmail"] ?: ""
        val operationType = if (rawOperationType == "self" || (userEmail.isNotBlank() && passEmail.isNotBlank() && passEmail.equals(userEmail, ignoreCase = true))) "self" else rawOperationType
        val listType = intent.getStringExtra("listType") ?: "recent"

        setContent {
            val currentGatePass by gatePassState
            val loading by isLoading
            val pastPasses by previousPasses

            val isActionAllowed = if (listType == "history") {
                false
            } else if (operationType == "self") {
                currentGatePass["status"]?.lowercase()?.trim() == "pending"
            } else {
                val status = currentGatePass["status"]?.lowercase()?.trim() ?: ""
                if (roleIsGuard()) {
                    (status == "approved" || isInterCampusGuardExit(currentGatePass)) && !checkDate()
                } else {
                    status == "pending" || status == "approving"
                }
            }

            GatePassDetailScreen(
                gatePass = currentGatePass,
                operationType = operationType,
                listType = listType,
                isLoading = loading,
                isActionAllowed = isActionAllowed,
                previousPasses = pastPasses,
                onBack = { finish() },
                onCallPhone = { phone -> callToPhone(phone) },
                onImageClick = { img ->
                    if (img.isNotBlank()) CommonOperation.showFullScreenImage(this@GatePassDetail, img)
                },
                onApprove = { tgRemark ->
                    approveGatePassWithRemark(tgRemark)
                },
                onReject = {
                    rejectGatePass()
                },
                onEditSave = { newReason, newTgRemark ->
                    if (operationType == "self") {
                        editSelfUserGatePass(newReason)
                    } else {
                        editGatePass(newReason, newTgRemark)
                    }
                },
                onRemoveSelfPass = {
                    removeGatePassBySelfUser()
                },
                onSecurityAction = {
                    executeSecurityAction()
                },
                onActivateInterPass = {
                    activateInterInstitutionalGatePass()
                },
                onLoadPreviousPasses = {
                    getPreviousGatePass()
                }
            )
        }
    }

    private fun roleIsGuard(): Boolean {
        return LoginUserDataHolder.loginUserData?.get("role")?.lowercase() == "security guard"
    }

    private fun isInterCampusGuardExit(pass: Map<String, String>): Boolean {
        if (!pass.containsKey("destinationCampus")) return false
        val myCampus = LoginUserDataHolder.loginUserData?.get("campus")
        val status = pass["status"] ?: ""
        return (pass["campus"] != myCampus && status == "Exited from source campus") || (pass["campus"] == myCampus && status != "approved")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDate(): Boolean {
        val applyDateVal = gatePassState.value["applyDate"]
        if (applyDateVal.isNullOrBlank()) return false
        return try {
            val gatePassDateString = applyDateVal.split(" ")[0].trim()
            val gatePassDate = LocalDate.parse(gatePassDateString)
            val currentDate = LocalDate.now()
            !gatePassDate.isEqual(currentDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun getPreviousGatePass() {
        isLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            val email = gatePass["applyEmail"] ?: ""
            val listOfAllGatePassByThisEmail = AppDatabase.getDatabase(this@GatePassDetail).gatePassDao().getAllGatePassesByEmail(email)
            val passList = listOfAllGatePassByThisEmail.map { it.passData }

            launch(Dispatchers.Main) {
                isLoading.value = false
                previousPasses.value = passList
            }
        }
    }

    private fun removeGatePassBySelfUser() {
        isLoading.value = true
        val hashToRemoveGatePass = hashMapOf(
            "token" to LoginUserDataHolder.token,
            "gatePassId" to (gatePass["gatePassId"] ?: "")
        )
        if (gatePass.containsKey("destinationCampus")) {
            hashToRemoveGatePass["destinationCampus"] = gatePass["destinationCampus"]!!
        }

        val callToRemove = RetrofitClient.instance.removeGatePassBySelfUser(hashToRemoveGatePass)
        callToRemove.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@GatePassDetail, "Gate Pass Removed Successfully", Toast.LENGTH_SHORT).show()
                    optimisticDelete()
                    finish()
                } else {
                    Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoading.value = false
                Toast.makeText(this@GatePassDetail, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editSelfUserGatePass(newReason: String) {
        if (newReason.isBlank()) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading.value = true
        val hashToEdit = hashMapOf(
            "token" to LoginUserDataHolder.token,
            "reason" to newReason,
            "gatePassId" to (gatePass["gatePassId"] ?: "")
        )
        if (gatePass.containsKey("destinationCampus")) {
            hashToEdit["destinationCampus"] = gatePass["destinationCampus"]!!
        }

        val callToEdit = RetrofitClient.instance.editGatePassBySelfUser(hashToEdit)
        callToEdit.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@GatePassDetail, "Gate Pass Edited Successfully", Toast.LENGTH_SHORT).show()
                    val updated = HashMap(gatePassState.value)
                    updated["reason"] = newReason
                    gatePassState.value = updated
                } else {
                    Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoading.value = false
                Toast.makeText(this@GatePassDetail, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun rejectGatePass() {
        isLoading.value = true
        val hashToRejectGatePass = hashMapOf(
            "token" to LoginUserDataHolder.token,
            "gatePassId" to (gatePass["gatePassId"] ?: "")
        )
        val callToReject: Call<ResponseBody> = if (gatePass.containsKey("destinationCampus")) {
            hashToRejectGatePass["destinationCampus"] = gatePass["destinationCampus"]!!
            RetrofitClient.instance.rejectInterInstitutionalGatePass(hashToRejectGatePass)
        } else {
            RetrofitClient.instance.rejectGatePass(hashToRejectGatePass)
        }

        callToReject.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@GatePassDetail, "Gate Pass Rejected Successfully", Toast.LENGTH_SHORT).show()
                    optimisticUpdateStatus("rejected")
                    val updated = HashMap(gatePassState.value)
                    updated["status"] = "rejected"
                    gatePassState.value = updated
                    finish()
                } else {
                    Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoading.value = false
                Toast.makeText(this@GatePassDetail, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun approveGatePassWithRemark(tgRemark: String) {
        val dataForApproval = hashMapOf(
            "token" to LoginUserDataHolder.token,
            "gatePassId" to (gatePass["gatePassId"] ?: "")
        )
        if (tgRemark.isNotBlank()) {
            dataForApproval["tgRemark"] = tgRemark
        }
        approveTheGatePass(dataForApproval)
    }

    private fun executeSecurityAction() {
        val dataForApproval = hashMapOf(
            "token" to LoginUserDataHolder.token,
            "gatePassId" to (gatePass["gatePassId"] ?: "")
        )
        approveTheGatePass(dataForApproval)
    }

    private fun approveTheGatePass(dataForApproval: HashMap<String, String>) {
        isLoading.value = true

        val callToGiveApproval: Call<ResponseBody> = if (gatePass.containsKey("destinationCampus")) {
            dataForApproval["destinationCampus"] = gatePass["destinationCampus"]!!
            if (roleIsGuard()) {
                RetrofitClient.instance.exitInterInstitutionalGatePass(dataForApproval)
            } else {
                RetrofitClient.instance.approveInterInstitutionalGatePassByMember(dataForApproval)
            }
        } else {
            RetrofitClient.instance.approveGatePass(dataForApproval)
        }

        callToGiveApproval.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@GatePassDetail, "Gate Pass Approved Successfully", Toast.LENGTH_SHORT).show()

                    val newStatus = if (roleIsGuard()) {
                        if (gatePass.containsKey("destinationCampus")) {
                            val myCampus = LoginUserDataHolder.loginUserData?.get("campus")
                            if (gatePass["campus"] == myCampus) {
                                if (gatePass["status"] != "approved") "Entered into source campus" else "Exited from source campus"
                            } else {
                                if (gatePass["status"] == "Exited from source campus") "Entered into destination campus" else "Exited from destination campus"
                            }
                        } else {
                            "exit"
                        }
                    } else {
                        "approved"
                    }

                    if (dataForApproval.containsKey("tgRemark")) {
                        gatePass["tgRemark"] = dataForApproval["tgRemark"]!!
                    }
                    optimisticUpdateStatus(newStatus)

                    val updated = HashMap(gatePassState.value)
                    updated["status"] = newStatus
                    if (dataForApproval.containsKey("tgRemark")) {
                        updated["tgRemark"] = dataForApproval["tgRemark"]!!
                    }
                    gatePassState.value = updated

                    finish()
                } else {
                    Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoading.value = false
                Toast.makeText(this@GatePassDetail, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editGatePass(newReason: String, newTgRemark: String) {
        val hashToEditGatePass = hashMapOf<String, String>()
        if (newReason.isNotBlank() && gatePass["reason"] != newReason) {
            hashToEditGatePass["reason"] = newReason
        }
        if (newTgRemark.isNotBlank() && (gatePass["tgRemark"] ?: "") != newTgRemark) {
            hashToEditGatePass["tgRemark"] = newTgRemark
        }

        if (hashToEditGatePass.isEmpty()) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            hashToEditGatePass["token"] = LoginUserDataHolder.token
            hashToEditGatePass["gatePassId"] = gatePass["gatePassId"] ?: ""
            if (gatePass.containsKey("destinationCampus")) {
                hashToEditGatePass["destinationCampus"] = gatePass["destinationCampus"]!!
            }

            val callToEditGatePass = RetrofitClient.instance.editGatePass(hashToEditGatePass)
            callToEditGatePass.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                    isLoading.value = false
                    if (response.isSuccessful) {
                        Toast.makeText(this@GatePassDetail, "Gate Pass Edited Successfully", Toast.LENGTH_SHORT).show()
                        val updated = HashMap(gatePassState.value)
                        if (hashToEditGatePass.containsKey("reason")) updated["reason"] = newReason
                        if (hashToEditGatePass.containsKey("tgRemark")) updated["tgRemark"] = newTgRemark
                        gatePassState.value = updated
                    } else {
                        Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    isLoading.value = false
                    Toast.makeText(this@GatePassDetail, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun activateInterInstitutionalGatePass() {
        isLoading.value = true
        val requestBody = HashMap<String, String>()
        requestBody["token"] = LoginUserDataHolder.token
        requestBody["gatePassId"] = gatePass["gatePassId"] ?: ""

        RetrofitClient.instance.activateInterInstitutionalGatePass(requestBody)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isLoading.value = false
                    if (response.isSuccessful) {
                        Toast.makeText(this@GatePassDetail, "Pass Activated Successfully", Toast.LENGTH_SHORT).show()
                        val updated = HashMap(gatePassState.value)
                        updated["passActivity"] = "active"
                        updated["status"] = "Entered into destination campus"
                        gatePassState.value = updated
                        optimisticUpdateStatus("Entered into destination campus")
                    } else {
                        Toast.makeText(this@GatePassDetail, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isLoading.value = false
                    Toast.makeText(this@GatePassDetail, "Failed to activate pass", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun optimisticUpdateStatus(newStatus: String) {
        val gatePassId = gatePass["gatePassId"]?.toIntOrNull() ?: return
        val tgRemark = gatePass["tgRemark"]
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@GatePassDetail)
            if (gatePass.containsKey("destinationCampus")) {
                val dao = db.interInstitutionalGatePassDao()
                val existing = dao.getGatePassById(gatePassId)
                if (existing != null) {
                    existing.passData["status"] = newStatus
                    if (tgRemark != null) existing.passData["tgRemark"] = tgRemark
                    dao.insertGatePass(existing)
                }
            } else {
                val dao = db.gatePassDao()
                val existing = dao.getGatePassById(gatePassId)
                if (existing != null) {
                    existing.passData["status"] = newStatus
                    if (tgRemark != null) existing.passData["tgRemark"] = tgRemark
                    dao.insertGatePass(existing)
                }
            }
        }
    }

    private fun optimisticDelete() {
        val gatePassId = gatePass["gatePassId"]?.toIntOrNull() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@GatePassDetail)
            if (gatePass.containsKey("destinationCampus")) {
                db.interInstitutionalGatePassDao().deleteByGatePassId(gatePassId)
            } else {
                db.gatePassDao().deleteByGatePassId(gatePassId)
            }
        }
    }
}