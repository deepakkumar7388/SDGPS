package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.digitalpass.ui.MainScaffold

class Reception : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val visitors by passSyncViewModel.activeVisitors.observeAsState(emptyList())
            val visitorsList = visitors.map { it.visitorData }

            MainScaffold(
                role = "reception",
                onLogout = { CommonOperation.logout(this@Reception, "thisUser") },
                onNavigate = { route ->
                    when (route) {
                        "apply" -> {
                            val intent = Intent(this@Reception, AppliedGatePassBySelfUser::class.java)
                            intent.putExtra("autoOpenApply", true)
                            startActivity(intent)
                        }
                        "entry" -> startActivity(Intent(this@Reception, EnterVisitor::class.java))
                        "history" -> startActivity(Intent(this@Reception, UserHistory::class.java))
                        "report" -> startActivity(Intent(this@Reception, ReportActivity::class.java))
                    }
                },
                contentView = null,
                visitors = visitorsList
            )
        }

        setupNotificationBell()
    }
}
