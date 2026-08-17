package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.digitalpass.ui.MainScaffold

class SecurityGuard : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val gatePasses by passSyncViewModel.activeGatePasses.observeAsState(emptyList())
            val gatePassesList = gatePasses.map { it.passData }
            val visitors by passSyncViewModel.activeVisitors.observeAsState(emptyList())
            val visitorsList = visitors.map { it.visitorData }

            MainScaffold(
                role = "security guard",
                onLogout = { CommonOperation.logout(this@SecurityGuard, "thisUser") },
                onNavigate = { route ->
                    when (route) {
                        "apply" -> {
                            val intent = Intent(this@SecurityGuard, AppliedGatePassBySelfUser::class.java)
                            intent.putExtra("autoOpenApply", true)
                            startActivity(intent)
                        }
                        "verify" -> startActivity(Intent(this@SecurityGuard, MainActivity::class.java))
                        "visitors" -> startActivity(Intent(this@SecurityGuard, EnterVisitor::class.java))
                        "approvals" -> startActivity(Intent(this@SecurityGuard, UserHistory::class.java))
                        "report" -> startActivity(Intent(this@SecurityGuard, ReportActivity::class.java))
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
