package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Reception : BaseActivity() {
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var profileImage: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var visitorAdapter: RecentPassAdapter

    private var searchBar: SearchView?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reception)

        var toolbar=findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
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

        var swipeRefresh=findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefresh.setOnRefreshListener {
            LoginUserDataHolder.getVisitorList()
            swipeRefresh.isRefreshing=false
        }


        //create gallery launcher
        galleryLauncher=registerForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->
            if(uri==null)return@registerForActivityResult
            //upload image to server
            CommonOperation.uploadImage(this,uri)
        }

        //profile data setup
        profileImage = findViewById(R.id.ProfileImage)

        //now setup user profile in CommonOperation
        CommonOperation.setupUserProfile(this)

        var drawer=findViewById<DrawerLayout>(R.id.drawerLayout)
        profileImage.setOnClickListener {
            drawer.openDrawer(GravityCompat.END)
        }
        var editProfilePictureLogo=findViewById<ImageView>(R.id.editProfilePictureLogo)
        editProfilePictureLogo.setOnClickListener {
            galleryLauncher.launch("image/*")
        }


        //setup apply gate pass button
        var applyGatePassButton=findViewById<MaterialButton>(R.id.applyGatePassButton)
        applyGatePassButton.setOnClickListener {
            startActivity(Intent(this, AppliedGatePassBySelfUser::class.java))
        }


        var historyButton=findViewById<MaterialButton>(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, UserHistory::class.java))
        }

        searchBar=findViewById<SearchView>(R.id.receptionRecentSearch)
        //setup search bar
        setupSearchBar()
        //setup recyclerView and adapter for current visitor
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(this)
        visitorAdapter=RecentPassAdapter("visitor",ArrayList())
        recyclerView.adapter=visitorAdapter

        //setup these adapter in LoginUserDataHolder
        LoginUserDataHolder.visitorListAdapter=visitorAdapter
        LoginUserDataHolder.getVisitorList()

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

    override fun onResume(){
        super.onResume()
    }

}