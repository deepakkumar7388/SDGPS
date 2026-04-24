package com.example.digitalpass

import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LevelForBatch : AppCompatActivity() {

    lateinit var search: SearchView
    lateinit var selectedMemberRecyclerView:RecyclerView
    lateinit var unselectedMemberRecyclerView: RecyclerView
    var selectedList=ArrayList<HashMap<String,String>>()
    var unselectedList=ArrayList<HashMap<String,String>>()
    lateinit var selectedMemberAdapter:LevelAdapter
    lateinit var unselectedMemberAdapter:LevelAdapter

    lateinit var doneButton: ExtendedFloatingActionButton

    private lateinit var progressBar:CustomProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_level_for_batch)
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

        search=findViewById(R.id.searchMemberForGatePass)

        selectedMemberRecyclerView=findViewById(R.id.selectedMemberRecyclerView)
        unselectedMemberRecyclerView=findViewById(R.id.unselectedMemberRecyclerView)

        selectedMemberRecyclerView.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(this)
        unselectedMemberRecyclerView.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(this)

        //create adapter for selected and unselected member
        selectedMemberAdapter= LevelAdapter(selectedList)
        unselectedMemberAdapter=LevelAdapter(unselectedList)

        //set alternative adapter
        selectedMemberAdapter.alternativeAdapter=unselectedMemberAdapter
        unselectedMemberAdapter.alternativeAdapter=selectedMemberAdapter

        //set adapter to recyclerView
        selectedMemberRecyclerView.adapter=selectedMemberAdapter
        unselectedMemberRecyclerView.adapter=unselectedMemberAdapter

        //setup search bar
        setupSearchBar()

         doneButton=findViewById(R.id.selectedMembersDoneButton)

        if(intent.getStringExtra("levelType")=="allotment"){
            findViewById<MaterialToolbar>(R.id.toolbar).title="Allot Security Guard"
            //get only allotted security guard
            getAllottedSecurityGuard()
        }
        else {

            //this is related to batch management
            //get all users
            getAllUsers()
        }

    }

    fun getAllUsers(){
        //members are selected and return the list of selected member to AddBatch activity
        doneButton.setOnClickListener {
            val returnIntent = intent
            returnIntent.putExtra("levelData", selectedMemberAdapter.levelData)
            setResult(RESULT_OK, returnIntent)
            finish()
        }

        progressBar.startProgressBar()

        CoroutineScope(Dispatchers.IO).launch {
            var callForAllUsers = RetrofitClient.instance.getAllMemberForLevel(hashMapOf(
                "token" to LoginUserDataHolder.token,
                "campus" to LoginUserDataHolder.campusForBatchOperation
            ))
            callForAllUsers.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
                override fun onResponse(
                    call: Call<ArrayList<HashMap<String, String>>?>,
                    response: Response<ArrayList<HashMap<String, String>>?>
                ) {

                    if (response.isSuccessful) {
                        unselectedList = response.body()!!
                        if (intent.getStringExtra("levelType") == "unlevel"){
                            unselectedMemberAdapter.updateList(
                                unselectedList
                            )
                        }
                        else {
                            var levelData = intent.getStringArrayListExtra("levelData")!!

                            //filter selected members from unselected list
                            val iterator = unselectedList.iterator()
                            while (iterator.hasNext()) {
                                val item = iterator.next()
                                if (levelData.contains(item["email"])) {
                                    selectedList.add(item)
                                    iterator.remove() // Safely remove the item using the iterator
                                }
                            }
                            selectedMemberAdapter.updateList(selectedList)
                            unselectedMemberAdapter.updateList(unselectedList)
                        }

                    } else {
                        var errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@LevelForBatch, errorMessage, Toast.LENGTH_SHORT).show()
                    }

                    progressBar.stopAnimation()
                }

                override fun onFailure(
                    call: Call<ArrayList<HashMap<String, String>>?>,
                    t: Throwable
                ) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@LevelForBatch, t.message, Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    private fun getAllottedSecurityGuard(){
        doneButton.setOnClickListener {
            if(selectedMemberAdapter.levelData.size==0){
                Toast.makeText(this, "Please select security guard", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAllottedSecurityGuard()
        }

        progressBar.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            var callForAllottedSecurityGuard=RetrofitClient.instance.getAllottedSecurityGuard(
                hashMapOf(
                    "campus" to intent.getStringExtra("campusName")!!,
                    "token" to LoginUserDataHolder.token
                )
            )
            callForAllottedSecurityGuard.enqueue(object:Callback<HashMap<String,ArrayList<HashMap<String,String>>>>{
                override fun onResponse(
                    call: Call<HashMap<String, ArrayList<HashMap<String, String>>>?>,
                    response: Response<HashMap<String, ArrayList<HashMap<String, String>>>?>
                ) {
                    progressBar.stopAnimation()
                    if(response.isSuccessful){
                        unselectedList=response.body()!!["unallotted"]!!
                        selectedList=response.body()!!["allotted"]!!
                        unselectedMemberAdapter.updateList(unselectedList)
                        selectedMemberAdapter.updateList(selectedList)
                    }
                    else{
                        var errorMessage=LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@LevelForBatch, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<HashMap<String, ArrayList<HashMap<String, String>>>?>,
                    t: Throwable
                ) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@LevelForBatch, "Something went wrong", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }
    fun setupSearchBar(){
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterMembers(query)
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                filterMembers(query)
                return true
            }

        })
    }


    private fun saveAllottedSecurityGuard(){
        progressBar.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            var callToSaveAllottedSecurityGuard = RetrofitClient.instance.saveAllottedSecurityGuard(
                hashMapOf(
                    "campus" to intent.getStringExtra("campusName")!!,
                    "allottedSecurityGuard" to getEmail(),
                    "token" to LoginUserDataHolder.token
                )
            )

            callToSaveAllottedSecurityGuard.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    progressBar.stopAnimation()
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@LevelForBatch,
                            "Security guard allotted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        var errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@LevelForBatch, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<ResponseBody?>,
                    t: Throwable
                ) {
                    progressBar.stopAnimation()
                    Toast.makeText(this@LevelForBatch, "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
            })
            }

    }

    private fun getEmail():ArrayList<String>{
        var emailList=ArrayList<String>()
        for(item in selectedMemberAdapter.levelData){
            emailList.add(item["email"]!!)
        }
        return emailList
    }

    fun filterMembers(query: String?){
        var filteredMembers=if(query.isNullOrEmpty()){
            unselectedList
        }
        else{
            unselectedList.filter { it["name"]!!.contains(query,ignoreCase = true) }
        }

        //filter selected members from filtered list
        filteredMembers=filteredMembers.filter { !selectedMemberAdapter.levelData.contains(it) }

        unselectedMemberAdapter.updateList(filteredMembers as ArrayList<HashMap<String, String>>)
    }
}