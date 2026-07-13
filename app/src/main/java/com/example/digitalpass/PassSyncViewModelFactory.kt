package com.example.digitalpass

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PassSyncViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassSyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = com.example.digitalpass.database.AppDatabase.getDatabase(context)
            return PassSyncViewModel(
                GatePassRepository(context),
                InterInstitutionalGatePassRepository(database.interInstitutionalGatePassDao(), context),
                VisitorRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
