package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.database.UserEntity
import com.example.digitalpass.ui.UserManagementScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagement : BaseActivity() {

    private lateinit var database: AppDatabase
    private val memberListState = mutableStateOf<List<Map<String, String>>>(emptyList())
    private val selectedRoleState = mutableStateOf("All")
    private val searchQueryState = mutableStateOf("")
    private val selectedEmailsState = mutableStateOf<Set<String>>(emptySet())
    private val isLoadingState = mutableStateOf(false)

    private val activityResultFromUserView = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val previousEmail = data?.getStringExtra("previousEmail")
            val currentList = memberListState.value.toMutableList()
            val position = currentList.indexOfFirst { it["email"] == previousEmail }

            if (position != -1) {
                if (data?.getStringExtra("userManagementOperation") == "remove") {
                    currentList.removeAt(position)
                    memberListState.value = currentList
                    CoroutineScope(Dispatchers.IO).launch {
                        previousEmail?.let { database.userDao().deleteUserByEmail(it) }
                    }
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val updatedUser = data?.getSerializableExtra("userUpdatedData") as? HashMap<String, String>
                    if (updatedUser != null) {
                        currentList[position] = updatedUser
                        if (intent.getStringExtra("userManagementType") == "batch") {
                            if (updatedUser["batch"] == intent?.getStringExtra("batchName")) currentList.removeAt(position)
                        }
                        memberListState.value = currentList
                        CoroutineScope(Dispatchers.IO).launch {
                            val newEmail = updatedUser["email"] ?: ""
                            if (newEmail.isNotEmpty()) {
                                if (previousEmail != null && previousEmail != newEmail) {
                                    database.userDao().deleteUserByEmail(previousEmail)
                                }
                                database.userDao().insertUser(UserEntity(newEmail, updatedUser))
                            }
                        }
                    }
                }
                userOperationViewModel.triggerUserSync(LoginUserDataHolder.token)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)
        loadMembersFromRoom()

        userOperationViewModel.userSyncState.observe(this) { result ->
            isLoadingState.value = false
            result.onSuccess { entities ->
                val rawUsers = entities.map { it.userData }
                memberListState.value = filterUsersForRole(rawUsers)
                Toast.makeText(this@UserManagement, "Users synced successfully", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@UserManagement, it.message ?: "Sync failed", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            val members by memberListState
            val selectedRole by selectedRoleState
            val searchQuery by searchQueryState
            val selectedEmails by selectedEmailsState
            val isLoading by isLoadingState

            UserManagementScreen(
                userList = members,
                selectedRoleFilter = selectedRole,
                searchQuery = searchQuery,
                selectedUserEmails = selectedEmails,
                isLoading = isLoading,
                onBack = {
                    if (selectedEmails.isNotEmpty()) {
                        selectedEmailsState.value = emptySet()
                    } else {
                        finish()
                    }
                },
                onSync = {
                    isLoadingState.value = true
                    userOperationViewModel.triggerUserSync(LoginUserDataHolder.token)
                },
                onRoleSelect = { role -> selectedRoleState.value = role },
                onSearchChange = { query -> searchQueryState.value = query },
                onUserClick = { user ->
                    val email = user["email"] ?: ""
                    if (selectedEmails.isNotEmpty()) {
                        toggleUserSelection(email)
                    } else {
                        val intent = Intent(this@UserManagement, UserManagementViewUser::class.java).apply {
                            putExtra("user", HashMap(user))
                        }
                        activityResultFromUserView.launch(intent)
                    }
                },
                onUserLongClick = { email ->
                    val myRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
                    if (myRole in listOf("admin", "principal")) {
                        toggleUserSelection(email)
                    }
                },
                onDeleteSelected = {
                    deleteSelectedUsers()
                },
                onAddUser = {
                    startActivity(Intent(this@UserManagement, AddUser::class.java))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadMembersFromRoom()
    }

    private fun toggleUserSelection(email: String) {
        val currentSet = selectedEmailsState.value.toMutableSet()
        if (currentSet.contains(email)) {
            currentSet.remove(email)
        } else {
            currentSet.add(email)
        }
        selectedEmailsState.value = currentSet
    }

    private fun filterUsersForRole(list: List<Map<String, String>>): List<Map<String, String>> {
        val loggedInRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
        val loggedInDept = LoginUserDataHolder.loginUserData?.get("department")?.trim() ?: ""

        return when (loggedInRole) {
            "hod" -> {
                list.filter { user ->
                    val dept = user["department"]?.trim() ?: ""
                    val role = user["role"]?.lowercase()?.trim() ?: ""
                    dept.equals(loggedInDept, ignoreCase = true) && role !in listOf("admin", "principal", "security", "security guard", "reception")
                }
            }
            "faculty" -> {
                list.filter { user ->
                    val dept = user["department"]?.trim() ?: ""
                    val role = user["role"]?.lowercase()?.trim() ?: ""
                    dept.equals(loggedInDept, ignoreCase = true) && role == "student"
                }
            }
            else -> list
        }
    }

    private fun loadMembersFromRoom() {
        isLoadingState.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allUsers = database.userDao().getAllUsers().map { it.userData }
                val type = intent.getStringExtra("userManagementType")

                val filteredUsers = filterUsersForRole(allUsers)

                val result = if (type == "batch") {
                    val batchName = intent.getStringExtra("batchName") ?: ""
                    filteredUsers.filter { it["batch"] != batchName && it["role"]?.lowercase() == "student" }
                } else {
                    filteredUsers
                }

                runOnUiThread {
                    isLoadingState.value = false
                    memberListState.value = result
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isLoadingState.value = false
                }
            }
        }
    }

    private fun deleteSelectedUsers() {
        val emailsToDelete = ArrayList(selectedEmailsState.value)
        if (emailsToDelete.isEmpty()) return

        isLoadingState.value = true
        val hashMap = HashMap<String, Any>().apply {
            put("removeEmails", emailsToDelete)
            put("token", LoginUserDataHolder.token)
        }
        val call = RetrofitClient.instance.removeUser(hashMap)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                isLoadingState.value = false
                if (response.isSuccessful) {
                    Toast.makeText(this@UserManagement, "Users deleted successfully", Toast.LENGTH_SHORT).show()
                    val currentList = memberListState.value.toMutableList()
                    currentList.removeAll { emailsToDelete.contains(it["email"]) }
                    memberListState.value = currentList
                    selectedEmailsState.value = emptySet()

                    CoroutineScope(Dispatchers.IO).launch {
                        emailsToDelete.forEach { email ->
                            database.userDao().deleteUserByEmail(email)
                        }
                    }
                    userOperationViewModel.triggerUserSync(LoginUserDataHolder.token)
                } else {
                    Toast.makeText(this@UserManagement, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                isLoadingState.value = false
                Toast.makeText(this@UserManagement, "Error deleting users", Toast.LENGTH_SHORT).show()
            }
        })
    }
}