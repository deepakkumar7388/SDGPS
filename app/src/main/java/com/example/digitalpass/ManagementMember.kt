package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.digitalpass.ui.MainScaffold

class ManagementMember : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val gatePasses by passSyncViewModel.activeGatePasses.observeAsState(emptyList())
            val gatePassesList = gatePasses.map { it.passData }
            val visitors by passSyncViewModel.activeVisitors.observeAsState(emptyList())
            val visitorsList = visitors.map { it.visitorData }

            MainScaffold(
                role = LoginUserDataHolder.loginUserData?.get("role") ?: "Management Member",
                onLogout = { CommonOperation.logout(this@ManagementMember, "thisUser") },
                onNavigate = { route ->
                    when (route) {
                        "apply" -> {
                            val intent = Intent(this@ManagementMember, AppliedGatePassBySelfUser::class.java)
                            intent.putExtra("autoOpenApply", true)
                            startActivity(intent)
                        }
                        "approvals" -> startActivity(Intent(this@ManagementMember, UserHistory::class.java))
                        "users" -> startActivity(Intent(this@ManagementMember, UserManagement::class.java))
                        "batches" -> startActivity(Intent(this@ManagementMember, LevelForBatch::class.java))
                        "report" -> startActivity(Intent(this@ManagementMember, ReportActivity::class.java))
                    }
                },
                contentView = null,
                gatePasses = gatePassesList,
                visitors = visitorsList
            )
        }

        setupNotificationBell()
    }
}
