package com.example.digitalpass

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserHistory : AppCompatActivity() {
    private var search: SearchView? = null
    private lateinit var toggleGroup: com.google.android.material.button.MaterialButtonToggleGroup

    private lateinit var visitorAdapter: RecentPassAdapter
    private lateinit var gatePassAdapter: RecentPassAdapter

    private var fromTimeStamp: Long = 0
    private var toTimeStamp: Long = 0

    private var recentVisitorList = ArrayList<HashMap<String, String>>()
    private var recentGatePassList = ArrayList<HashMap<String, String>>()

    private var dateVisitorList = ArrayList<HashMap<String, String>>()
    private var dateGatePassList = ArrayList<HashMap<String, String>>()

    //lambda function to get date from timestamp in standard date form yyyy-MM-dd
    private var getDate = { timeInMilli: Long ->
        if (timeInMilli == 0L)
            ""
        else {
            val date = java.util.Date(timeInMilli)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd")
            format.format(date)
        }
    }

    private var progressBar: CustomProgressBar? = null

    private var statusList=arrayListOf(
        "All Status","pending","meet","exit","approving","approved","rejected"
    )

    private var statusSpinner: Spinner?=null
    lateinit var dateFromButton:MaterialButton
    lateinit var dateToButton:MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_history)
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

        progressBar = findViewById(R.id.customProgressBar)

        search = findViewById(R.id.userManagementSearch)
        dateFromButton = findViewById(R.id.dateFromButton)
        dateToButton = findViewById(R.id.dateToButton)

        var recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.membersRecyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        visitorAdapter = RecentPassAdapter("visitor", ArrayList())
        gatePassAdapter = RecentPassAdapter("gatePass", ArrayList())
        visitorAdapter.listTypeByDate = "history"
        gatePassAdapter.listTypeByDate = "history"
        recyclerView.adapter = visitorAdapter

        //get visitorList and gatePassList
        getVisitorList(fromTimeStamp, toTimeStamp)
        getGatePassList(fromTimeStamp, toTimeStamp)

        //setup searchBar
        setupSearchBar()


        toggleGroup = findViewById(R.id.toggleGroup)
        // Set initial style
        updateButtonStyles(true)

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                filterWithQuery(search?.query.toString())
                var isVisitor = checkedId == R.id.visitorListButton

                // Smoothly fade out the RecyclerView
                recyclerView.animate().alpha(0f).setDuration(150).withEndAction {
                    // Swap the adapter
                    recyclerView.adapter = if (isVisitor) visitorAdapter else gatePassAdapter

                    // Update button visual feedback
                    updateButtonStyles(isVisitor)

                    // Fade it back in
                    recyclerView.animate().alpha(1f).setDuration(150).start()
                }.start()
            }
        }

        //setup apply and clear button
        var applyButton = findViewById<Button>(R.id.applyButton)
        var clearButton = findViewById<Button>(R.id.clearButton)

        applyButton.setOnClickListener {
            getVisitorList(fromTimeStamp, toTimeStamp)
            getGatePassList(fromTimeStamp, toTimeStamp)

            //clear the search bar
            search?.setQuery("", false)
            statusSpinner?.setSelection(0)
            applyButton.isEnabled = false
        }
        clearButton.setOnClickListener {
            fromTimeStamp = 0
            toTimeStamp = 0
            dateFromButton.text = "Date From"
            dateToButton.text = "Date To"
            applyButton.isEnabled = false
            clearButton.isEnabled = false
            dateVisitorList = recentVisitorList
            dateGatePassList = recentGatePassList
            visitorAdapter.updateList(dateVisitorList)
            gatePassAdapter.updateList(dateGatePassList)
            statusSpinner?.setSelection(0)
            search?.setQuery("", false)
        }

        var dateFromPicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()
        var dateToPicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()


        dateFromButton.setOnClickListener {
            //show date picker
            dateFromPicker.show(supportFragmentManager, "DATE_PICKER")
        }
        dateFromPicker.addOnPositiveButtonClickListener { selection ->
            //ensure from date is less then or equal to to date if to date is selected
            if (dateToButton.text != "Date To" && dateToPicker.selection != null) {
                if (selection!! <= dateToPicker.selection!!) {
                    dateFromButton.text = dateFromPicker.headerText
                    fromTimeStamp = selection
                    applyButton.isEnabled = true
                } else Toast.makeText(this, "From date should be less then or equal to to date", Toast.LENGTH_SHORT).show()
            } else {
                dateFromButton.text = dateFromPicker.headerText
                fromTimeStamp = selection
            }
            clearButton.isEnabled = true
        }


        dateToButton.setOnClickListener {
            //show date picker
            dateToPicker.show(supportFragmentManager, "DATE_PICKER")
        }
        dateToPicker.addOnPositiveButtonClickListener { selection ->
            //ensure to date is greater then or equal to from date if from date is selected
            if (dateFromButton.text != "Date From" && dateFromPicker.selection != null) {
                if (selection!! >= dateFromPicker.selection!!) {
                    dateToButton.text = dateToPicker.headerText
                    toTimeStamp = selection
                    applyButton.isEnabled = true
                } else Toast.makeText(this, "To date should be greater then or equal to from date", Toast.LENGTH_SHORT).show()
            } else {
                dateToButton.text = dateToPicker.headerText
                toTimeStamp = selection
            }
            clearButton.isEnabled = true
        }

        findViewById<MaterialButton>(R.id.downloadCSVButton).setOnClickListener {
            if(toggleGroup.checkedButtonId==R.id.visitorListButton){
                if(visitorAdapter.recentPassList.isEmpty()) Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show()
                else showDialogDownloadInformation()
            }
            else{
                if(gatePassAdapter.recentPassList.isEmpty()) Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show()
                else showDialogDownloadInformation()
            }
        }

        statusSpinner=findViewById(R.id.spinner)
        statusSpinner?.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusList)
        setupStatusSpinner()
    }

    private fun updateButtonStyles(isVisitor: Boolean) {
        val visitorButton = findViewById<MaterialButton>(R.id.visitorListButton)
        val gatePassButton = findViewById<MaterialButton>(R.id.gatePassListButton)

        val activeColor = android.graphics.Color.parseColor("#052E92")
        val inactiveColor = android.graphics.Color.WHITE
        val activeTextColor = android.graphics.Color.WHITE
        val inactiveTextColor = android.graphics.Color.parseColor("#052E92")

        if (isVisitor) {
            visitorButton.setBackgroundColor(activeColor)
            visitorButton.setTextColor(activeTextColor)
            gatePassButton.setBackgroundColor(inactiveColor)
            gatePassButton.setTextColor(inactiveTextColor)
        } else {
            gatePassButton.setBackgroundColor(activeColor)
            gatePassButton.setTextColor(activeTextColor)
            visitorButton.setBackgroundColor(inactiveColor)
            visitorButton.setTextColor(inactiveTextColor)
        }
    }

    private fun getVisitorList(fromDate: Long, toDate: Long) {
        progressBar?.startProgressBar()
        var callToGetVisitorList = RetrofitClient.instance.getVisitorListHistory(
            hashMapOf(
                "token" to LoginUserDataHolder.token,
                "fromDate" to getDate(fromDate),
                "toDate" to getDate(toDate)
            )
        )

        callToGetVisitorList.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, String>>?>,
                response: Response<ArrayList<HashMap<String, String>>?>
            ) {
                if (response.isSuccessful) {
                    dateVisitorList = response.body()!!
                    visitorAdapter.updateList(dateVisitorList)
                    if (fromDate == 0L || toDate == 0L) recentVisitorList = dateVisitorList
                } else Toast.makeText(this@UserHistory, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()

                progressBar?.stopAnimation()
            }

            override fun onFailure(
                call: Call<ArrayList<HashMap<String, String>>?>,
                t: Throwable
            ) {
                progressBar?.stopAnimation()
                Toast.makeText(this@UserHistory, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getGatePassList(fromDate: Long, toDate: Long) {
        progressBar?.startProgressBar()
        var callToGetGatePassList = RetrofitClient.instance.getGatePassListHistory(
            hashMapOf(
                "token" to LoginUserDataHolder.token,
                "fromDate" to getDate(fromDate),
                "toDate" to getDate(toDate)
            )
        )
        callToGetGatePassList.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, String>>?>,
                response: Response<ArrayList<HashMap<String, String>>?>
            ) {
                if (response.isSuccessful) {
                    dateGatePassList = response.body()!!
                    gatePassAdapter.updateList(dateGatePassList)
                    if (fromDate == 0L || toDate == 0L) recentGatePassList = dateGatePassList
                } else Toast.makeText(this@UserHistory, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                progressBar?.stopAnimation()
            }

            override fun onFailure(
                call: Call<ArrayList<HashMap<String, String>>?>,
                t: Throwable
            ) {
                progressBar?.stopAnimation()
                Toast.makeText(this@UserHistory, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearchBar() {
        search?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterWithQuery(search?.query.toString())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterWithQuery(search?.query.toString())
                return true
            }
        })
    }

    private fun filterWithQuery(query: String) {
        if (toggleGroup.checkedButtonId == R.id.visitorListButton){
            var temVisitorList=dateVisitorList.filter{it["name"]!!.contains(query, ignoreCase = true)} as ArrayList<HashMap<String,String>>
            if(statusSpinner?.selectedItemPosition==0)visitorAdapter.updateList(temVisitorList)
            else visitorAdapter.updateList(temVisitorList.filter{it["status"]!!.contains(statusSpinner?.selectedItem.toString(),ignoreCase = true)} as ArrayList<HashMap<String, String>>)
        }
        else{

            var temGatePassList=dateGatePassList.filter{it["name"]!!.contains(query, ignoreCase = true)} as ArrayList<HashMap<String,String>>
            if(statusSpinner?.selectedItemPosition==0)gatePassAdapter.updateList(temGatePassList)
            else gatePassAdapter.updateList(temGatePassList.filter{it["status"]!!.contains(statusSpinner?.selectedItem.toString(),ignoreCase = true)} as ArrayList<HashMap<String, String>>)
        }
    }

    private fun downloadExcelFile(fileName: String) {
        progressBar?.startProgressBar()
        Toast.makeText(this,"Downloading...",Toast.LENGTH_SHORT).show()
        val dataToDownload = if (toggleGroup.checkedButtonId == R.id.visitorListButton) visitorAdapter.recentPassList else gatePassAdapter.recentPassList

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Digital Pass History")

        val keys = dataToDownload[0].keys.toList()

        // Create Header Row
        val headerRow = sheet.createRow(0)
        for ((index, key) in keys.withIndex()) {
            headerRow.createCell(index).setCellValue(key)
        }

        // Create Data Rows
        for ((rowIndex, item) in dataToDownload.withIndex()) {
            val row = sheet.createRow(rowIndex + 1)
            for ((colIndex, key) in keys.withIndex()) {
                row.createCell(colIndex).setCellValue(item[key] ?: "")
            }
        }

        saveExcelFile(fileName, workbook)
    }

    private fun saveExcelFile(fileName: String, workbook: XSSFWorkbook) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.xlsx")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, contentValues)
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { stream ->
                    workbook.write(stream)
                }
                workbook.close()
                progressBar?.stopAnimation()
                Toast.makeText(this, "Excel file downloaded", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                progressBar?.stopAnimation()
                Toast.makeText(this, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
    }

    private fun setupStatusSpinner(){
        statusSpinner?.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                filterWithQuery(search?.query.toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }

    private fun showDialogDownloadInformation() {
        var dialogView=layoutInflater.inflate(R.layout.download_information,null)
        dialogView.findViewById<TextView>(R.id.listType).text=if(toggleGroup.checkedButtonId==R.id.visitorListButton) "List Type: Visitor" else "List Type: Gate Pass"
        dialogView.findViewById<TextView>(R.id.dateRange).text="Date Range: ${dateFromButton.text} - ${dateToButton.text}"
        dialogView.findViewById<TextView>(R.id.downloadedListStatusType).text="Status: ${statusSpinner?.selectedItem}"
        var fileName=dialogView.findViewById<TextInputEditText>(R.id.fileName)
        fileName.setText("DigitalPass_History_${if(toggleGroup.checkedButtonId==R.id.visitorListButton) "Visitor" else "GatePass"}_${dateFromButton.text} - ${dateToButton.text}")
        var dialog= MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialog.show()
        dialogView.findViewById<MaterialButton>(R.id.downloadButton).setOnClickListener {
            if(fileName.text.toString().trim().isEmpty()) Toast.makeText(this, "Please enter file name", Toast.LENGTH_SHORT).show()
            else downloadExcelFile(fileName.text.toString())
            dialog.dismiss()
        }
    }
}
