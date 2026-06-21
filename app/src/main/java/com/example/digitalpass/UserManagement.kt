package com.example.digitalpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.internal.service.Common
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.database.UserEntity

class UserManagement : BaseActivity() {

    private lateinit var database: AppDatabase
    lateinit var searchView: SearchView
     lateinit var roleToggleGroup: MaterialButtonToggleGroup
     lateinit var membersRecyclerView: RecyclerView
     lateinit var adapter: UserManagementAdapter
      var memberList=ArrayList<HashMap<String,String>>()

    private var progressBar: CustomProgressBar?=null
    private lateinit var deleteUserButton: FloatingActionButton

    lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private var activityResultFromUserView=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if(result.resultCode==RESULT_OK){
            var data=result.data
            var previousEmail = data?.getStringExtra("previousEmail")
            var position=memberList.indexOfFirst { it["email"]== previousEmail }
            if(position!=-1) {
                if (data?.getStringExtra("userManagementOperation") == "remove") {
                    memberList.removeAt(position)
                    CoroutineScope(Dispatchers.IO).launch {
                        previousEmail?.let { database.userDao().deleteUserByEmail(it) }
                    }
                } else {
                    var updatedUser =data?.getSerializableExtra("userUpdatedData") as HashMap<String, String>
                    memberList[position]=updatedUser
                    //remove this user if this user not from this batch
                    if(intent.getStringExtra("userManagementType")=="batch"){
                        if(updatedUser["batch"]==intent?.getStringExtra("batchName"))memberList.removeAt(position)
                    }
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
                filterMembers()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_management)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Get the Keyboard (IME) insets
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Calculate the bottom padding.
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

        database = AppDatabase.getDatabase(this)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            if (adapter.isSelectionMode) {
                adapter.clearSelection()
            } else {
                finish()
            }
        }
        toolbar.inflateMenu(R.menu.menu_user_management)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sync -> {
                    fetchUsersFromServer()
                    true
                }
                else -> false
            }
        }

        progressBar=findViewById(R.id.customProgressBar)
        deleteUserButton=findViewById(R.id.deleteUserButton)

        searchView = findViewById(R.id.userManagementSearch)
        membersRecyclerView = findViewById(R.id.recyclerViewUserManagement)
        membersRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = UserManagementAdapter(ArrayList<HashMap<String,String>>(), { selectedCount ->
            updateSelectionUI(selectedCount)
        }) { userItem->
            var intent= Intent(this, UserManagementViewUser::class.java).apply{
                putExtra("user",userItem)
            }
            activityResultFromUserView.launch(intent)
        }
        membersRecyclerView.adapter = adapter

        deleteUserButton.setOnClickListener {
            val selectedUsers = adapter.getSelectedUsers()
            if (selectedUsers.isNotEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Users")
                    .setMessage("Are you sure you want to delete ${selectedUsers.size} user(s)?")
                    .setPositiveButton("Yes") { _, _ ->
                        removeMultipleUsers(selectedUsers)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        setupSearchView()
        setupToggleGroup()
        fetchAllUserData()
    }



    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterMembers()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMembers()
                return true
            }
        })
    }

    private fun setupToggleGroup() {
         roleToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.roleToggleGroup)
        roleToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            filterMembers()
        }
    }

     fun filterMembers() {
         if(memberList.isEmpty())return

         var query=searchView.query.toString()
         var role= when (roleToggleGroup.checkedButtonId) {
             R.id.studentButton-> "student"
             R.id.managementMemberButton -> "managementMember"
             R.id.securityButton -> "security"
             R.id.receptionButton->"reception"
             else -> "allUsers"
         }

         var filteredList=ArrayList<HashMap<String,String>>()
         if(role=="allUsers") {
             for (member in memberList) {
                 if(member.get("name")!!.contains(query,ignoreCase = true)) filteredList.add(member)
             }
         }

         else if(role=="managementMember"){
             for (member in memberList) {
                 if (member.get("name")!!.contains(query, ignoreCase = true) && "principalhodfaculty".contains(
                         member["role"]!!, ignoreCase = true)) {
                     filteredList.add(member)
                 }
             }
         }
         else {
             for (member in memberList) {
                 if (member["name"]!!.contains(query, ignoreCase = true) && member["role"]!!.contains(role, ignoreCase = true)) {
                     filteredList.add(member)
                 }
             }

                 }

         adapter.updateList(filteredList)

    }


    private fun fetchAllUserData(){
        //set filter by default all user
        roleToggleGroup.check(R.id.allUserButton)

        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            //check in intent if user management type is userManagement or there is no key in intent then we fetch all users
            var userManagementType=intent?.getStringExtra("userManagementType")?:"userManagement"
            if(userManagementType=="userManagement") {
                val localUsers = database.userDao().getAllUsers()
                if (localUsers.isNotEmpty()) {
                    memberList = ArrayList(localUsers.map { it.userData })
                    runOnUiThread {
                        adapter.updateList(memberList)
                        progressBar?.stopAnimation()
                    }
                } else {
                    fetchUsersFromServer()
                }
            }
            else{
                var batchName=intent?.getStringExtra("batchName")
                toolbar.title="User Management\n$batchName"
                var localBatchMembers=database.userDao().getAllUsersOfBatch(batchName)
                if(localBatchMembers.isNotEmpty()){
                    memberList=ArrayList(localBatchMembers.map { it.userData })
                    runOnUiThread {
                        adapter.updateList(memberList)
                        progressBar?.stopAnimation()
                    }
                }
            }
        }
    }

    private fun fetchUsersFromServer() {
        runOnUiThread { progressBar?.startProgressBar() }
        var callToGetMember = RetrofitClient.instance.getMembersForUserManagement(LoginUserDataHolder.token)
        callToGetMember.enqueue(object : Callback<ArrayList<HashMap<String,String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String,String>>?>,
                response: Response<ArrayList<HashMap<String,String>>?>
            ) {
                if (response.isSuccessful) {
                    val fetchedUsers = response.body()!!
                    CoroutineScope(Dispatchers.IO).launch {
                        database.userDao().deleteAllUsers()
                        val entities = fetchedUsers.map { UserEntity(it["email"] ?: "", it) }.filter { it.email.isNotEmpty() }
                        database.userDao().insertAll(entities)
                        memberList = ArrayList(entities.map { it.userData })
                        runOnUiThread {
                            adapter.updateList(memberList)
                            filterMembers()
                            progressBar?.stopAnimation()
                        }
                    }
                } else {
                    var errorMessage= LoginUserDataHolder.getErrorMessage(response)
                    runOnUiThread {
                        Toast.makeText(this@UserManagement, errorMessage, Toast.LENGTH_SHORT).show()
                        progressBar?.stopAnimation()
                    }
                }
            }

            override fun onFailure(
                call: Call<ArrayList<HashMap<String,String>>?>,
                t: Throwable
            ) {
                runOnUiThread {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@UserManagement, "Error", Toast.LENGTH_SHORT).show()
                }
            }

        })
    }

    private fun updateSelectionUI(selectedCount: Int) {
        if (selectedCount > 0) {
            toolbar.title = "$selectedCount Selected"
            deleteUserButton.visibility = android.view.View.VISIBLE
        } else {
            var batchName=intent?.getStringExtra("batchName")
            if(batchName!=null){
                toolbar.title="User Management\n$batchName"
            } else {
                toolbar.title="User Management"
            }
            deleteUserButton.visibility = android.view.View.GONE
        }
    }

    private fun removeMultipleUsers(users: List<HashMap<String, String>>) {
        progressBar?.startProgressBar()
        val emails = users.mapNotNull { it["email"] }
        
        val map = HashMap<String, Any>()
        map["token"] = LoginUserDataHolder.token
        map["removeEmails"] = emails

        CoroutineScope(Dispatchers.IO).launch {
            RetrofitClient.instance.removeUser(map).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    runOnUiThread { progressBar?.stopAnimation() }
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@UserManagement, "Users removed successfully", Toast.LENGTH_SHORT).show()
                            memberList.removeAll(users)
                            adapter.clearSelection()
                            filterMembers()
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            emails.forEach { email ->
                                database.userDao().deleteUserByEmail(email)
                            }
                        }
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        runOnUiThread {
                            Toast.makeText(this@UserManagement, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    runOnUiThread { progressBar?.stopAnimation() }
                    runOnUiThread {
                        Toast.makeText(this@UserManagement, "Failed to remove users: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    override fun onBackPressed() {
        if (adapter.isSelectionMode) {
            adapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}