package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportAdapter: ReportAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var customProgressBar: CustomProgressBar
    private val reportList = ArrayList<HashMap<String, String>>()

    var addReportActivityResult=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode== RESULT_OK){
            var resultData=result.data
            var report=resultData?.getSerializableExtra("report") as HashMap<String,String>
            reportList.add(report)
            reportAdapter.notifyItemInserted(reportList.size-1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reports"

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        customProgressBar = findViewById(R.id.customProgressBar)

        val addReportButton = findViewById<FloatingActionButton>(R.id.addReportButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        reportAdapter = ReportAdapter(reportList)
        recyclerView.adapter = reportAdapter

        swipeRefreshLayout.setOnRefreshListener {
            getReports()
            swipeRefreshLayout.isRefreshing = false
        }

        addReportButton.setOnClickListener {
            addReportActivityResult.launch(Intent(this, AddReportActivity::class.java))
        }

        getReports()
    }

    private fun getReports() {
        customProgressBar.startProgressBar()
        CoroutineScope(Dispatchers.IO).launch {
            RetrofitClient.instance.getReports(LoginUserDataHolder.token)
                .enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
                    override fun onResponse(
                        call: Call<ArrayList<HashMap<String, String>>>,
                        response: Response<ArrayList<HashMap<String, String>>>
                    ) {
                        customProgressBar.stopAnimation()
                        if (response.isSuccessful) {
                            reportList.clear()
                            response.body()?.let { reportList.addAll(it) }
                            reportAdapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(this@ReportActivity, "Failed to load reports", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<ArrayList<HashMap<String, String>>>,
                        t: Throwable
                    ) {
                        customProgressBar.stopAnimation()
                        // Since backend may not exist yet, we don't spam errors
                        // Toast.makeText(this@ReportActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

}
