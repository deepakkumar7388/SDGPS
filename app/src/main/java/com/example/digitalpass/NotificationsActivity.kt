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

class NotificationsActivity : AppCompatActivity() {

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
}
