package com.example.digitalpass

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.digitalpass.ui.StudentApplyTabContent

class AppliedGatePassBySelfUser : BaseGatePassActivity() {
    override val recyclerViewId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userOperationViewModel.fetchCampuses(LoginUserDataHolder.token)

        setContent {
            val campusesResult by userOperationViewModel.campuses.observeAsState()
            val availableCampuses = campusesResult?.getOrNull() ?: emptyList()

            StudentApplyTabContent(
                initialPassType = "Regular Pass",
                availableCampuses = availableCampuses,
                onBack = { finish() },
                onRequestLocation = { callback ->
                    requestUserLocation { location ->
                        callback(location)
                    }
                },
                onSubmit = { passType, reason, destinationCampus ->
                    applyPassWithReason(passType, reason, destinationCampus)
                }
            )
        }
    }
}

