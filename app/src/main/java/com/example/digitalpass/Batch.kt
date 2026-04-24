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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Batch : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var allBatchList = ArrayList<String>()
    private var studentBatchList = ArrayList<String>()
    private var otherBatchList = ArrayList<String>()
    private lateinit var batchAdapter: BatchAdapter
    private lateinit var batchFilterToggleGroup: MaterialButtonToggleGroup

    private var progressBar: CustomProgressBar?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_batch)
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

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        progressBar=findViewById(R.id.customProgressBar)


        LoginUserDataHolder.campusForBatchOperation=intent.getStringExtra("campusName")!!

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
        progressBar?.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            val callForAllBatch = RetrofitClient.instance.getAllBatches(hashMapOf(
                "token" to LoginUserDataHolder.token,
                "campus" to intent.getStringExtra("campusName")!!))

            callForAllBatch.enqueue(object : Callback<HashMap<String, ArrayList<String>>> {
                override fun onResponse(
                    call: Call<HashMap<String, ArrayList<String>>?>,
                    response: Response<HashMap<String, ArrayList<String>>?>
                ) {
                    if (response.isSuccessful) {
                        val batches = response.body()
                        studentBatchList = batches?.get("student") ?: ArrayList()
                        otherBatchList = batches?.get("member") ?: ArrayList()
                        allBatchList = ArrayList(studentBatchList + otherBatchList)

                        if (!::batchAdapter.isInitialized) {
                            batchAdapter = BatchAdapter(allBatchList)
                            recyclerView.adapter = batchAdapter
                        } else {
                            // Refresh current filter state with new data
                            val searchBatch = findViewById<SearchView>(R.id.searchBatch)
                            filterWithQueryAndToggle(searchBatch.query?.toString())
                        }
                    } else {
                        val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                        Toast.makeText(this@Batch, errorMessage, Toast.LENGTH_LONG).show()
                    }

                    progressBar?.stopAnimation()
                }

                override fun onFailure(
                    call: Call<HashMap<String, ArrayList<String>>?>,
                    t: Throwable
                ) {
                    progressBar?.stopAnimation()
                    Toast.makeText(this@Batch, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }
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
            baseList.filter { it.contains(query, ignoreCase = true) }
        }

        if (::batchAdapter.isInitialized) {
            batchAdapter.updateList(ArrayList(filteredList))
        }
    }
}
