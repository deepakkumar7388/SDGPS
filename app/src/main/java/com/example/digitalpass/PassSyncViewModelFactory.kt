package com.example.digitalpass

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PassSyncViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassSyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PassSyncViewModel(
                GatePassRepository(context),
                VisitorRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
