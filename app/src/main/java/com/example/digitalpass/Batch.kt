package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.database.BatchEntity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.digitalpass.utils.setupEmptyState
import com.example.digitalpass.utils.evaluateEmptyState
import android.view.View

class Batch : BaseActivity() {

    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private var allBatchList = ArrayList<BatchItemData>()
    private var studentBatchList = ArrayList<BatchItemData>()
    private var otherBatchList = ArrayList<BatchItemData>()
    private lateinit var batchAdapter: BatchAdapter
    private lateinit var batchFilterToggleGroup: MaterialButtonToggleGroup

    private var progressBar: CustomProgressBar?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_batch)
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

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.inflateMenu(R.menu.menu_user_management)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sync -> {
                    fetchBatchesFromServer(intent.getStringExtra("campusName")!!)
                    true
                }
                else -> false
            }
        }

        progressBar=findViewById(R.id.customProgressBar)


        LoginUserDataHolder.setCampusForBatch(this, intent.getStringExtra("campusName")!!)

        val createNewBatch = findViewById<FloatingActionButton>(R.id.createNewBatch)
        createNewBatch.setOnClickListener {
            val intent = Intent(this, AddNewBatch::class.java)
            intent.putExtra("operation", "create")
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerViewForAllBatches)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupFilter()
    }

    override fun onResume() {
        super.onResume()
        fetchAllBatchData(intent.getStringExtra("campusName")!!)
    }

    private fun fetchAllBatchData(campusName: String) {
        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val localBatches = database.batchDao().getBatchesByCampus(campusName)
            val localUsers = database.userDao().getAllUsers()
            val counts = HashMap<String, Int>()
            localUsers.forEach { user ->
                val bName = user.userData["batch"]
                if (bName != null) {
                    counts[bName] = (counts[bName] ?: 0) + 1
                }
            }

            if (localBatches.isNotEmpty()) {
                studentBatchList = ArrayList(localBatches.filter { it.type == "student" }.map { BatchItemData(it.batchName, counts[it.batchName] ?: 0) })
                otherBatchList = ArrayList(localBatches.filter { it.type == "member" }.map { BatchItemData(it.batchName, counts[it.batchName] ?: 0) })
                allBatchList = ArrayList(studentBatchList + otherBatchList)

                runOnUiThread {
                    if (!::batchAdapter.isInitialized) {
                        batchAdapter = BatchAdapter(allBatchList)
                        recyclerView.adapter = batchAdapter
                        val emptyView = findViewById<View>(R.id.emptyStateLayout)
                        if (emptyView != null) {
                            recyclerView.setupEmptyState(emptyView, "No Batches Found", R.drawable.batchemptyview)
                        }
                    } else {
                        // Refresh current filter state with new data
                        val searchBatch = findViewById<SearchView>(R.id.searchBatch)
                        filterWithQueryAndToggle(searchBatch.query?.toString())
                    }
                    progressBar?.stopAnimation()
                }
            } else {
                fetchBatchesFromServer(campusName)
            }
        }
    }

    private fun fetchBatchesFromServer(campusName: String) {
        runOnUiThread { progressBar?.startProgressBar() }
        val callForAllBatch = RetrofitClient.instance.getAllBatches(hashMapOf(
            "token" to LoginUserDataHolder.token,
            "campus" to campusName))

        callForAllBatch.enqueue(object : Callback<HashMap<String, ArrayList<String>>> {
            override fun onResponse(
                call: Call<HashMap<String, ArrayList<String>>?>,
                response: Response<HashMap<String, ArrayList<String>>?>
            ) {
                if (response.isSuccessful) {
                    val batches = response.body()
                    val fetchedStudentList = batches?.get("student") ?: ArrayList()
                    val fetchedOtherList = batches?.get("member") ?: ArrayList()
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        database.batchDao().deleteBatchesByCampus(campusName)
                        
                        val entities = ArrayList<BatchEntity>()
                        fetchedStudentList.forEach { batchName ->
                            entities.add(BatchEntity(batchName, "student", campusName))
                        }
                        fetchedOtherList.forEach { batchName ->
                            entities.add(BatchEntity(batchName, "member", campusName))
                        }
                        
                        database.batchDao().insertAll(entities)

                        val localUsers = database.userDao().getAllUsers()
                        val counts = HashMap<String, Int>()
                        localUsers.forEach { user ->
                            val bName = user.userData["batch"]
                            if (bName != null) {
                                counts[bName] = (counts[bName] ?: 0) + 1
                            }
                        }
                        
                        studentBatchList = ArrayList(fetchedStudentList.map { BatchItemData(it, counts[it] ?: 0) })
                        otherBatchList = ArrayList(fetchedOtherList.map { BatchItemData(it, counts[it] ?: 0) })
                        allBatchList = ArrayList(studentBatchList + otherBatchList)
                        
                        runOnUiThread {
                            if (!::batchAdapter.isInitialized) {
                                batchAdapter = BatchAdapter(allBatchList)
                                recyclerView.adapter = batchAdapter
                                val emptyView = findViewById<View>(R.id.emptyStateLayout)
                                if (emptyView != null) {
                                    recyclerView.setupEmptyState(emptyView, "No Batches Found", R.drawable.batchemptyview)
                                }
                            } else {
                                val searchBatch = findViewById<SearchView>(R.id.searchBatch)
                                filterWithQueryAndToggle(searchBatch.query?.toString())
                            }
                            progressBar?.stopAnimation()
                        }
                    }
                } else {
                    val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                    runOnUiThread {
                        Toast.makeText(this@Batch, errorMessage, Toast.LENGTH_LONG).show()
                        progressBar?.stopAnimation()
                    }
                }
            }

            override fun onFailure(
                call: Call<HashMap<String, ArrayList<String>>?>,
                t: Throwable
            ) {
                runOnUiThread {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@Batch, "Network error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupFilter() {
        val searchBatch = findViewById<SearchView>(R.id.searchBatch)
        batchFilterToggleGroup = findViewById(R.id.batchFilterToggleGroup)

        searchBatch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterWithQueryAndToggle(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterWithQueryAndToggle(newText)
                return true
            }
        })

        // Trigger filter when the toggle button selection changes
        batchFilterToggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                filterWithQueryAndToggle(searchBatch.query?.toString())
            }
        }
    }

    private fun filterWithQueryAndToggle(query: String?) {
        // Prevent filtering if data isn't loaded yet
        if (allBatchList.isEmpty() && studentBatchList.isEmpty() && otherBatchList.isEmpty()) return

        val baseList = when (batchFilterToggleGroup.checkedButtonId) {
            R.id.allBatchesButton -> allBatchList
            R.id.studentBatchesButton -> studentBatchList
            R.id.otherBatchesButton -> otherBatchList
            else -> allBatchList
        }

        val filteredList = if (query.isNullOrBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (::batchAdapter.isInitialized) {
            batchAdapter.updateList(ArrayList(filteredList))
            val emptyView = findViewById<View>(R.id.emptyStateLayout)
            if (emptyView != null) {
                recyclerView.evaluateEmptyState(emptyView)
            }
        }
    }
}
