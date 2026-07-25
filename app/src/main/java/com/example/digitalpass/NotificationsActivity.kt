package com.example.digitalpass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalpass.database.AppDatabase
import com.example.digitalpass.database.NotificationEntity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.digitalpass.CommonOperation.logout
import com.example.digitalpass.utils.setupEmptyState

class NotificationsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val newConfig = android.content.res.Configuration(newBase.resources.configuration)
        newConfig.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(newConfig))
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        database = AppDatabase.getDatabase(this)

        adapter = NotificationAdapter(emptyList()) { notification ->
            markAsRead(notification)
        }
        recyclerView.adapter = adapter
        
        val emptyView = findViewById<android.view.View>(R.id.emptyStateLayout)
        if (emptyView != null) {
            recyclerView.setupEmptyState(emptyView, "No Notifications", R.drawable.notificationemptyview)
        }

        // Observe unread notifications
        database.notificationDao().getUnreadNotifications().observe(this) { notifications ->
            adapter.updateData(notifications)
        }
    }

    private fun markAsRead(notification: NotificationEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            database.notificationDao().markAsRead(notification.id)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mark_all_read -> {
                if(adapter.notifications.isEmpty())
                    Toast.makeText(this,"No notifications",Toast.LENGTH_SHORT).show()
                else{
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Confirm Read")
                        .setMessage("Are you sure you want to mark all notifications as read?")
                        .setPositiveButton("Yes, Read") { _, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.notificationDao().deleteAll()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
