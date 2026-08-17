package com.example.digitalpass

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.digitalpass.ui.AddUserScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddUser : BaseActivity() {

    private val departmentListState = mutableStateOf<List<String>>(emptyList())
    private val roleListState = mutableStateOf<List<String>>(emptyList())
    private val batchListState = mutableStateOf<List<String>>(emptyList())
    private val isLoadingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fetchDepartments()

        setContent {
            val departments by departmentListState
            val roles by roleListState
            val batches by batchListState
            val isLoading by isLoadingState

            AddUserScreen(
                departments = departments,
                roles = roles,
                batches = batches,
                isLoading = isLoading,
                onDepartmentSelected = { dept ->
                    fetchRolesForDepartment(dept)
                    fetchBatchesForDepartment(dept)
                },
                onRoleSelected = { role ->
                    // State handled in screen
                },
                onBack = { finish() },
                onSubmit = { formData ->
                    submitAddUser(formData)
                }
            )
        }
    }

    private fun fetchDepartments() {
        val loggedInRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
        val loggedInDept = LoginUserDataHolder.loginUserData?.get("department")?.trim() ?: ""

        if (loggedInRole in listOf("hod", "faculty") && loggedInDept.isNotEmpty()) {
            departmentListState.value = listOf(loggedInDept)
            fetchRolesForDepartment(loggedInDept)
            fetchBatchesForDepartment(loggedInDept)
        } else {
            userOperationViewModel.departments.observe(this) { result ->
                result.onSuccess { list ->
                    departmentListState.value = list
                    if (list.isNotEmpty()) {
                        fetchRolesForDepartment(list.first())
                        fetchBatchesForDepartment(list.first())
                    }
                }
            }
            userOperationViewModel.fetchDepartments(LoginUserDataHolder.token, "userManagement")
        }
    }

    private fun fetchRolesForDepartment(dept: String) {
        isLoadingState.value = true
        val loggedInRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.getRoleBasedOnDepartment(hashMapOf(
                "department" to dept,
                "token" to LoginUserDataHolder.token
            ))
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(call: Call<ArrayList<String>?>, response: Response<ArrayList<String>?>) {
                    isLoadingState.value = false
                    if (response.isSuccessful) {
                        val rawRoles = response.body() ?: emptyList()
                        roleListState.value = when (loggedInRole) {
                            "faculty" -> rawRoles.filter { it.equals("student", ignoreCase = true) }
                            "hod" -> rawRoles.filter { it.lowercase().trim() in listOf("faculty", "student", "teacher", "tg") }
                            else -> rawRoles
                        }
                    }
                }
                override fun onFailure(call: Call<ArrayList<String>?>, t: Throwable) {
                    isLoadingState.value = false
                }
            })
        }
    }

    private fun fetchBatchesForDepartment(dept: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val hashForBatch = hashMapOf(
                "department" to dept,
                "role" to "student",
                "token" to LoginUserDataHolder.token
            )
            val call = RetrofitClient.instance.getBatchesBasedOnDepartment(hashForBatch)
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(call: Call<ArrayList<String>?>, response: Response<ArrayList<String>?>) {
                    if (response.isSuccessful) {
                        batchListState.value = response.body() ?: emptyList()
                    }
                }
                override fun onFailure(call: Call<ArrayList<String>?>, t: Throwable) {}
            })
        }
    }

    private fun submitAddUser(formData: Map<String, String>) {
        isLoadingState.value = true
        val payload = HashMap(formData).apply {
            put("token", LoginUserDataHolder.token)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val call = RetrofitClient.instance.addNewUser(payload)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isLoadingState.value = false
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@AddUser, "User Registered Successfully!", Toast.LENGTH_SHORT).show()
                            userOperationViewModel.triggerUserSync(LoginUserDataHolder.token)
                            finish()
                        }
                    } else {
                        val error = LoginUserDataHolder.getErrorMessage(response)
                        runOnUiThread {
                            Toast.makeText(this@AddUser, "Failed: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isLoadingState.value = false
                    runOnUiThread {
                        Toast.makeText(this@AddUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}