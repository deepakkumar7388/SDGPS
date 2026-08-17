package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.digitalpass.ui.MainScaffold

class Student : BaseGatePassActivity() {
    override val recyclerViewId: Int = R.id.studentRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userOperationViewModel.fetchCampuses(LoginUserDataHolder.token)

        setContent {
            val gatePasses by passSyncViewModel.selfGatePasses.observeAsState(emptyList())
            val gatePassesList = gatePasses.map { it.passData }
            val campusesResult by userOperationViewModel.campuses.observeAsState()
            val availableCampuses = campusesResult?.getOrNull() ?: emptyList()

            MainScaffold(
                role = "student",
                onLogout = { CommonOperation.logout(this@Student, "thisUser") },
                onNavigate = { _ -> },
                onApplyPass = { passType, reason, destinationCampus ->
                    applyPassWithReason(passType, reason, destinationCampus)
                },
                availableCampuses = availableCampuses,
                onRequestLocation = { callback ->
                    requestUserLocation { location ->
                        callback(location)
                    }
                },
                contentView = null,
                gatePasses = gatePassesList
            )
        }

        setupNotificationBell()
    }
}
