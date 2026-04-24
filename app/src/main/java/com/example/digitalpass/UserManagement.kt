package com.example.digitalpass

import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagement : AppCompatActivity() {

    lateinit var searchView: SearchView
     lateinit var roleToggleGroup: MaterialButtonToggleGroup
     lateinit var membersRecyclerView: RecyclerView
     lateinit var adapter: UserManagementAdapter
      var memberList=ArrayList<HashMap<String,String>>()

    private var progressBar: CustomProgressBar?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_management)
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

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        progressBar=findViewById(R.id.customProgressBar)

        searchView = findViewById(R.id.userManagementSearch)
        membersRecyclerView = findViewById(R.id.recyclerViewUserManagement)
        membersRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = UserManagementAdapter(ArrayList<HashMap<String,String>>())
        membersRecyclerView.adapter = adapter

        setupSearchView()
        setupToggleGroup()
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


    override fun onResume() {
        super.onResume()
        //set filter by default all user
        roleToggleGroup.check(R.id.allUserButton)

        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            var callToGetMember = RetrofitClient.instance.getMembersForUserManagement(LoginUserDataHolder.token)
            callToGetMember.enqueue(object : Callback<ArrayList<HashMap<String,String>>> {
                override fun onResponse(
                    call: Call<ArrayList<HashMap<String,String>>?>,
                    response: Response<ArrayList<HashMap<String,String>>?>
                ) {
                    if (response.isSuccessful) {
                        memberList=response.body()!!
                        adapter.updateList(memberList)
                    } else {
                        var errorMessage= LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@UserManagement, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    progressBar?.stopAnimation()
                }

                override fun onFailure(
                    call: Call<ArrayList<HashMap<String,String>>?>,
                    t: Throwable
                ) {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@UserManagement, "Error", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }


}