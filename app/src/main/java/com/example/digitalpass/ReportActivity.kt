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
import okhttp3.ResponseBody
import androidx.appcompat.app.AlertDialog
import com.example.digitalpass.utils.setupEmptyState

class ReportActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportAdapter: ReportAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var customProgressBar: CustomProgressBar
    private lateinit var addReportButton: FloatingActionButton
    private lateinit var deleteReportButton: FloatingActionButton
    private val reportList = ArrayList<HashMap<String, String>>()

    var addReportActivityResult=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode== RESULT_OK){
            var resultData=result.data
            var report=resultData?.getSerializableExtra("report") as HashMap<String,String>
            reportList.add(0,report)
            reportAdapter.notifyItemInserted(0)
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
            if (reportAdapter.isSelectionMode) {
                reportAdapter.clearSelection()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        customProgressBar = findViewById(R.id.customProgressBar)
        addReportButton = findViewById(R.id.addReportButton)
        deleteReportButton = findViewById(R.id.deleteReportButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        reportAdapter = ReportAdapter(reportList) { selectedCount ->
            updateSelectionUI(selectedCount)
        }
        recyclerView.adapter = reportAdapter
        
        val emptyView = findViewById<View>(R.id.emptyStateLayout)
        if (emptyView != null) {
            recyclerView.setupEmptyState(emptyView, "No Reports Found", R.drawable.reportemptyview)
        }

        swipeRefreshLayout.setOnRefreshListener {
            getReports()
            swipeRefreshLayout.isRefreshing = false
        }

        addReportButton.setOnClickListener {
            addReportActivityResult.launch(Intent(this, AddReportActivity::class.java))
        }

        deleteReportButton.setOnClickListener {
            val selectedReports = reportAdapter.getSelectedReports()
            if (selectedReports.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Delete Reports")
                    .setMessage("Are you sure you want to delete ${selectedReports.size} report(s)?")
                    .setPositiveButton("Yes") { _, _ ->
                        removeMultipleReports(selectedReports)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        getReports()
    }

    private fun updateSelectionUI(selectedCount: Int) {
        if (selectedCount > 0) {
            supportActionBar?.title = "$selectedCount Selected"
            addReportButton.visibility = View.GONE
            deleteReportButton.visibility = View.VISIBLE
        } else {
            supportActionBar?.title = "Reports"
            addReportButton.visibility = View.VISIBLE
            deleteReportButton.visibility = View.GONE
        }
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
                            reportAdapter.clearSelection()
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
                        Toast.makeText(this@ReportActivity, "Failed to load reports", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun removeMultipleReports(reports: List<HashMap<String, String>>) {
        customProgressBar.startProgressBar()
        val reportIds = reports.mapNotNull { it["reportId"] }
        
        val map = HashMap<String, Any>()
        map["token"] = LoginUserDataHolder.token
        map["reportIds"] = reportIds

        CoroutineScope(Dispatchers.IO).launch {
            RetrofitClient.instance.removeReport(map).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    customProgressBar.stopAnimation()
                    if (response.isSuccessful) {
                        Toast.makeText(this@ReportActivity, "Reports removed successfully", Toast.LENGTH_SHORT).show()
                        reportList.removeAll(reports)
                        reportAdapter.clearSelection()
                    } else {
                        Toast.makeText(this@ReportActivity, "Failed to remove reports", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    customProgressBar.stopAnimation()
                    Toast.makeText(this@ReportActivity, "Failed to remove reports: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onBackPressed() {
        if (reportAdapter.isSelectionMode) {
            reportAdapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}
