package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.example.digitalpass.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.digitalpass.utils.setupEmptyState
import com.example.digitalpass.utils.evaluateEmptyState

class SecurityGuard : BaseActivity() {
    private lateinit var profileImage: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var visitorAdapter: RecentPassAdapter
    private lateinit var gatePassAdapter: RecentPassAdapter
    private var securityPermission=false

    private var searchBar:SearchView?=null

    private var progressBar:CustomProgressBar?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_security_guard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

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

        var refreshLayout=findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        refreshLayout.setOnRefreshListener {
            passSyncViewModel.triggerGatePassSync(LoginUserDataHolder.token)
            passSyncViewModel.triggerVisitorSync(LoginUserDataHolder.token)
            passSyncViewModel.triggerInterInstitutionalSync(LoginUserDataHolder.token)
            refreshLayout.isRefreshing=false
        }



        progressBar=findViewById(R.id.customProgressBar)

        //profile data setup
        profileImage = findViewById(R.id.ProfileImage)

        //now setup user profile in CommonOperation
        CommonOperation.setupUserProfile(this)

        var drawer=findViewById<DrawerLayout>(R.id.drawerLayout)
        profileImage.setOnClickListener {
            drawer.openDrawer(GravityCompat.END)
        }
        
        setupNotificationBell()
        
        var editProfilePictureLogo=findViewById<ImageView>(R.id.editProfilePictureLogo)
        editProfilePictureLogo.setOnClickListener {
            launchProfileImageUpdate()
        }

        val enterVisitorButton = findViewById<FloatingActionButton>(R.id.enterVisitorButton)
        val historyButton = findViewById<MaterialButton>(R.id.historyButton)

        //setup applied gate pass button
        var applyGatePassButton = findViewById<MaterialButton>(R.id.applyGatePassButton)
        applyGatePassButton.setOnClickListener {
            startActivity(Intent(this, AppliedGatePassBySelfUser::class.java))
        }
        historyButton.setOnClickListener {
            startActivity(Intent(this, UserHistory::class.java))
        }

        val reportFloatingButton = findViewById<FloatingActionButton>(R.id.reportFloatingButton)
        reportFloatingButton.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        //check this security guard allotted or not
        checkPermissionOfSecurityGuard()

        enterVisitorButton.setOnClickListener {
            if(securityPermission) {
                val intent = Intent(this@SecurityGuard, EnterVisitor::class.java)
                intent.putExtra("operation", "enter")
                startActivity(intent)
            }
            else Toast.makeText(this, "Not assigned for campus", Toast.LENGTH_SHORT).show()
        }

        searchBar=findViewById<SearchView>(R.id.search)
        //setup search bar
        setupSearchBar()


        // Setup recyclerView and adapters
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        visitorAdapter = RecentPassAdapter("visitor", ArrayList())
        gatePassAdapter = RecentPassAdapter("gatePass", ArrayList())

        // Initial setup
        recyclerView.adapter = visitorAdapter
        LoginUserDataHolder.visitorListAdapter = visitorAdapter
        LoginUserDataHolder.gatePassListAdapter = gatePassAdapter
        
        val emptyView = findViewById<View>(R.id.emptyStateLayout)
        if (emptyView != null) {
            recyclerView.setupEmptyState(emptyView, "No Data Found", R.drawable.commonemptyviewforgatepassandvisitor)
        }

        // Fetch initial data
        setupPassSyncAndObserve()

        // Setup Toggle Group and Transitions
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        updateButtonStyles(true) // Default to Visitor

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val isVisitor = checkedId == R.id.visitorListButton

                //update list type in LoginUserDataHolder
                LoginUserDataHolder.listType = if (isVisitor) "visitor" else "gatePass"
                //also we have to filter the list
                LoginUserDataHolder.filterList(searchBar?.query?.toString() ?: "")

                // Smoothly fade out the RecyclerView
                recyclerView.animate().alpha(0f).setDuration(150).withEndAction {
                    // Swap the adapter
                    recyclerView.adapter = if (isVisitor) visitorAdapter else gatePassAdapter

                    if (emptyView != null) {
                        recyclerView.setupEmptyState(emptyView, "No Data Found", R.drawable.commonemptyviewforgatepassandvisitor)
                        recyclerView.evaluateEmptyState(emptyView)
                    }

                    // Update button visual feedback
                    updateButtonStyles(isVisitor)

                    // Fade it back in
                    recyclerView.animate().alpha(1f).setDuration(150).start()
                }.start()
            }
        }

        //trigger user sync
        userOperationViewModel.triggerUserSync(LoginUserDataHolder.token)
    }

    private fun setupSearchBar(){
        searchBar?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                LoginUserDataHolder.filterList(p0?:"")
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                LoginUserDataHolder.filterList((p0?:""))
                return true
            }
        })
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


    private fun checkPermissionOfSecurityGuard() {

        progressBar?.startProgressBar()
        // Use lifecycleScope so the callback is cancelled if the activity is destroyed
        // (prevents IllegalStateException / NPE on older devices when activity dies mid-request)
        lifecycleScope.launch(Dispatchers.IO) {
            RetrofitClient.instance.checkPermissionOfSecurityGuard(LoginUserDataHolder.token)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {

                        progressBar?.stopAnimation()
                        if (response.isSuccessful) {
                            securityPermission = true
                        } else {
                            Toast.makeText(this@SecurityGuard, "You are not allowed to enter visitor", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        progressBar?.stopAnimation()
                        Toast.makeText(this@SecurityGuard, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }


    private fun setupPassSyncAndObserve() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val todayStart = "$today 00:00:00"
        val todayEnd = "$today 23:59:59"

        val interInstitutionalSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.interInstitutionalSwitch)
        
        passSyncViewModel.activeInterInstitutional.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked == true) {
                val maps = ArrayList(list.map { it.passData })
                LoginUserDataHolder.gatePassList = maps
                gatePassAdapter.updateList(maps)
            }
        }
        
        passSyncViewModel.interInstitutionalSyncState.observe(this) { result ->
            result.onSuccess {
                passSyncViewModel.loadActiveInterInstitutionalGatePasses(todayStart, todayEnd)
            }
        }

        passSyncViewModel.activeGatePasses.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked != true) {
                val maps = ArrayList(list.map { it.passData })
                LoginUserDataHolder.gatePassList = maps
                gatePassAdapter.updateList(maps)
            }
        }
        
        passSyncViewModel.gatePassSyncState.observe(this) { result ->
            result.onSuccess {
                passSyncViewModel.loadActiveGatePasses(todayStart, todayEnd)
            }
        }

        passSyncViewModel.loadActiveInterInstitutionalGatePasses(todayStart, todayEnd)
        passSyncViewModel.triggerInterInstitutionalSync(LoginUserDataHolder.token)
        passSyncViewModel.loadActiveGatePasses(todayStart, todayEnd)
        passSyncViewModel.triggerGatePassSync(LoginUserDataHolder.token)

        interInstitutionalSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val list = passSyncViewModel.activeInterInstitutional.value ?: emptyList()
                val maps = ArrayList(list.map { it.passData })
                LoginUserDataHolder.gatePassList = maps
                gatePassAdapter.updateList(maps)
            } else {
                val list = passSyncViewModel.activeGatePasses.value ?: emptyList()
                val maps = ArrayList(list.map { it.passData })
                LoginUserDataHolder.gatePassList = maps
                gatePassAdapter.updateList(maps)
            }
        }

        passSyncViewModel.activeVisitors.observe(this) { list ->
            val maps = ArrayList(list.map { it.visitorData })
            LoginUserDataHolder.visitorList = maps
            visitorAdapter.updateList(maps)
        }

        passSyncViewModel.visitorSyncState.observe(this) { result ->
            result.onSuccess {
                passSyncViewModel.loadActiveVisitors(todayStart, todayEnd)
            }
        }

        passSyncViewModel.loadActiveVisitors(todayStart, todayEnd)
        passSyncViewModel.triggerVisitorSync(LoginUserDataHolder.token)
    }

    override fun onResume(){
        super.onResume()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val todayStart = "$today 00:00:00"
        val todayEnd = "$today 23:59:59"
        passSyncViewModel.loadActiveInterInstitutionalGatePasses(todayStart, todayEnd)
        passSyncViewModel.loadActiveGatePasses(todayStart, todayEnd)
        passSyncViewModel.loadActiveVisitors(todayStart, todayEnd)
    }
}
