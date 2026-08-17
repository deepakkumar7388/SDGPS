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
import com.example.digitalpass.utils.setupEmptyState
import com.example.digitalpass.utils.evaluateEmptyState

class UserHistory : BaseActivity() {
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
    private var departmentSpinner: Spinner?=null
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
        val userEmail = LoginUserDataHolder.loginUserData?.get("email") ?: ""
        val isStudent = LoginUserDataHolder.loginUserData?.get("role") == "student"

        recyclerView.adapter = if (isStudent) gatePassAdapter else visitorAdapter
        
        val emptyView = findViewById<View>(R.id.emptyStateLayout)
        if (emptyView != null) {
            recyclerView.setupEmptyState(emptyView, "No History Found", R.drawable.historyemptyview)
        }
        

        // Setup observers for local cache
        val interInstitutionalSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.interInstitutionalSwitch)
        
        passSyncViewModel.historicalInterInstitutional.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked == true) {
                dateGatePassList = ArrayList(list.map { it.passData })
                if (isStudent) {
                    dateGatePassList = ArrayList(dateGatePassList.filter { it["applyEmail"] == userEmail })
                }
                gatePassAdapter.updateList(dateGatePassList)
                recentGatePassList = dateGatePassList
                progressBar?.stopAnimation()
            }
        }
        
        passSyncViewModel.historicalGatePasses.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked != true) {
                dateGatePassList = ArrayList(list.map { it.passData })
                if (isStudent) {
                    dateGatePassList = ArrayList(dateGatePassList.filter { it["applyEmail"] == userEmail })
                }
                gatePassAdapter.updateList(dateGatePassList)
                recentGatePassList = dateGatePassList
                progressBar?.stopAnimation()
            }
        }

        passSyncViewModel.historicalVisitors.observe(this) { list ->
            dateVisitorList = ArrayList(list.map { it.visitorData })
            visitorAdapter.updateList(dateVisitorList)
            recentVisitorList = dateVisitorList
            progressBar?.stopAnimation()
        }

        passSyncViewModel.rangeInterInstitutional.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked == true) {
                dateGatePassList = ArrayList(list.map { it.passData })
                if (isStudent) {
                    dateGatePassList = ArrayList(dateGatePassList.filter { it["applyEmail"] == userEmail })
                }
                gatePassAdapter.updateList(dateGatePassList)
                progressBar?.stopAnimation()
            }
        }
        
        passSyncViewModel.rangeGatePasses.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked != true) {
                dateGatePassList = ArrayList(list.map { it.passData })
                if (isStudent) {
                    dateGatePassList = ArrayList(dateGatePassList.filter { it["applyEmail"] == userEmail })
                }
                gatePassAdapter.updateList(dateGatePassList)
                progressBar?.stopAnimation()
            }
        }

        passSyncViewModel.rangeVisitors.observe(this) { list ->
            dateVisitorList = ArrayList(list.map { it.visitorData })
            visitorAdapter.updateList(dateVisitorList)
            progressBar?.stopAnimation()
        }

        interInstitutionalSwitch?.setOnCheckedChangeListener { _, _ ->
            getGatePassList(fromTimeStamp, toTimeStamp)
        }

        //get visitorList and gatePassList
        if (!isStudent) {
            getVisitorList(fromTimeStamp, toTimeStamp)
        }
        getGatePassList(fromTimeStamp, toTimeStamp)

        //setup searchBar
        setupSearchBar()


        toggleGroup = findViewById(R.id.toggleGroup)
        if (isStudent) {
            toggleGroup.visibility = View.GONE
            // Ensure Gate Pass button styling is active if it ever becomes visible somehow
            updateButtonStyles(false) 
        } else {
            // Set initial style
            updateButtonStyles(true)
        }

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                filterWithQuery(search?.query.toString())
                var isVisitor = checkedId == R.id.visitorListButton

                // Smoothly fade out the RecyclerView
                recyclerView.animate().alpha(0f).setDuration(150).withEndAction {
                    // Swap the adapter
                    recyclerView.adapter = if (isVisitor) visitorAdapter else gatePassAdapter
                    
                    val emptyView = findViewById<View>(R.id.emptyStateLayout)
                    if (emptyView != null) {
                        recyclerView.setupEmptyState(emptyView, "No History Found", R.drawable.historyemptyview)
                        recyclerView.evaluateEmptyState(emptyView) // Evaluate immediately for the active tab
                    }

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
            if (!isStudent) {
                getVisitorList(fromTimeStamp, toTimeStamp)
            }
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

        departmentSpinner=findViewById(R.id.spinnerDepartment)
        statusSpinner=findViewById(R.id.spinnerStatus)
        statusSpinner?.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusList)

        setupStatusSpinner()

        getAllDepartment()
    }

    private fun getAllDepartment(){
        progressBar?.startProgressBar()
        
        userOperationViewModel.departments.removeObservers(this)
        userOperationViewModel.departments.observe(this) { result ->
            result.onSuccess { departmentList ->
                val departments = ArrayList(departmentList)
                departments.add(0,"All Department")
                departmentSpinner?.adapter = ArrayAdapter(this@UserHistory, android.R.layout.simple_spinner_dropdown_item, departments)
            }.onFailure {
                Toast.makeText(this@UserHistory, "Something went wrong: ${it.message}", Toast.LENGTH_SHORT).show()
                departmentSpinner?.adapter = ArrayAdapter(this@UserHistory, android.R.layout.simple_spinner_dropdown_item, arrayListOf("All Department"))
            }
            progressBar?.stopAnimation()
            userOperationViewModel.departments.removeObservers(this)
        }
        
        userOperationViewModel.fetchDepartments(LoginUserDataHolder.token, "history")
    }

    private fun updateButtonStyles(isVisitor: Boolean) {
        val visitorButton = findViewById<MaterialButton>(R.id.visitorListButton)
        val gatePassButton = findViewById<MaterialButton>(R.id.gatePassListButton)

        val activeColor = android.graphics.Color.parseColor("#052E92")
        val inactiveColor = android.graphics.Color.WHITE
        val activeTextColor = android.graphics.Color.WHITE
        val inactiveTextColor = android.graphics.Color.parseColor("#052E92")

        val interInstitutionalSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.interInstitutionalSwitch)
        
        if (isVisitor) {
            visitorButton.setBackgroundColor(activeColor)
            visitorButton.setTextColor(activeTextColor)
            gatePassButton.setBackgroundColor(inactiveColor)
            gatePassButton.setTextColor(inactiveTextColor)
            interInstitutionalSwitch?.visibility = android.view.View.GONE
        } else {
            gatePassButton.setBackgroundColor(activeColor)
            gatePassButton.setTextColor(activeTextColor)
            visitorButton.setBackgroundColor(inactiveColor)
            visitorButton.setTextColor(inactiveTextColor)
            interInstitutionalSwitch?.visibility = android.view.View.VISIBLE
        }
    }

    private fun getVisitorList(fromDate: Long, toDate: Long) {
        progressBar?.startProgressBar()
        if (fromDate == 0L || toDate == 0L) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todayStart = "$today 00:00:00"
            passSyncViewModel.loadHistoricalVisitors(todayStart)
        } else {
            val startDate = getDate(fromDate) + " 00:00:00"
            val endDate = getDate(toDate) + " 23:59:59"
            passSyncViewModel.loadVisitorsByRange(startDate, endDate)
        }
    }

    private fun getGatePassList(fromDate: Long, toDate: Long) {
        progressBar?.startProgressBar()
        val interInstitutionalSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.interInstitutionalSwitch)
        val isInter = interInstitutionalSwitch?.isChecked == true
        if (fromDate == 0L || toDate == 0L) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todayStart = "$today 00:00:00"
            if (isInter) {
                passSyncViewModel.loadHistoricalInterInstitutionalGatePasses(todayStart)
            } else {
                passSyncViewModel.loadHistoricalGatePasses(todayStart)
            }
        } else {
            val startDate = getDate(fromDate) + " 00:00:00"
            val endDate = getDate(toDate) + " 23:59:59"
            if (isInter) {
                passSyncViewModel.loadInterInstitutionalByRange(startDate, endDate)
            } else {
                passSyncViewModel.loadGatePassesByRange(startDate, endDate)
            }
        }
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
            var temVisitorList = ArrayList(dateVisitorList.filter {
                (it["name"] ?: "").contains(query, ignoreCase = true) 
            })
            if (statusSpinner?.selectedItemPosition != 0) {
                val selectedStatus = statusSpinner?.selectedItem?.toString() ?: ""
                temVisitorList= ArrayList(temVisitorList.filter {
                    (it["status"] ?: "").contains(selectedStatus, ignoreCase = true)
                })
            }
            if(departmentSpinner?.selectedItemPosition!=0){
                temVisitorList= ArrayList(temVisitorList.filter {
                    (it["meetDepartment"] ?: "").contains(departmentSpinner?.selectedItem.toString(), ignoreCase = true)
                })
            }
                visitorAdapter.updateList(temVisitorList)

        }

        else{
            var temGatePassList = ArrayList(dateGatePassList.filter {
                (it["name"] ?: "").contains(query, ignoreCase = true) 
            })
            if (statusSpinner?.selectedItemPosition != 0) {
                val selectedStatus = statusSpinner?.selectedItem?.toString() ?: ""
                temGatePassList = ArrayList(temGatePassList.filter {
                    (it["status"] ?: "").contains(selectedStatus, ignoreCase = true)
                })
            }
            if(departmentSpinner?.selectedItemPosition!=0){
                temGatePassList= ArrayList(temGatePassList.filter {
                    (it["department"] ?: "").contains(departmentSpinner?.selectedItem.toString(), ignoreCase = true)
                })
            }
                gatePassAdapter.updateList(temGatePassList)
        }
    }

    private fun downloadExcelFile(fileName: String) {
        progressBar?.startProgressBar()
        Toast.makeText(this,"Downloading...",Toast.LENGTH_SHORT).show()
        val dataToDownload = if (toggleGroup.checkedButtonId == R.id.visitorListButton) visitorAdapter.recentPassList else gatePassAdapter.recentPassList

        if (dataToDownload.isEmpty()) {
            progressBar?.stopAnimation()
            Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show()
            return
        }

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
                
            }
        }

        departmentSpinner?.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                filterWithQuery(search?.query.toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

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
