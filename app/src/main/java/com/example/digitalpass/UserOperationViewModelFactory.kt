package com.example.digitalpass

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.digitalpass.database.CampusDao
import com.example.digitalpass.database.DepartmentDao
import com.example.digitalpass.database.UserDao

class UserOperationViewModelFactory(
    private val context: Context,
    private val campusDao: CampusDao,
    private val departmentDao: DepartmentDao,
    private val userDao: UserDao,
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserOperationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserOperationViewModel(UserOperationRepository(context, campusDao, departmentDao, userDao, apiService)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
