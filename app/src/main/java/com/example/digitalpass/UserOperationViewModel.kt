package com.example.digitalpass

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserOperationViewModel(private val repository: UserOperationRepository) : ViewModel() {

    private val _campuses = MutableLiveData<Result<List<String>>>()
    val campuses: LiveData<Result<List<String>>> = _campuses

    private val _departments = MutableLiveData<Result<List<String>>>()
    val departments: LiveData<Result<List<String>>> = _departments
    
    private val _userSyncState = MutableLiveData<Result<List<com.example.digitalpass.database.UserEntity>>>()
    val userSyncState: LiveData<Result<List<com.example.digitalpass.database.UserEntity>>> = _userSyncState
    
    private var isSyncingUsers = false

    fun fetchCampuses(token: String) {
        viewModelScope.launch {
            try {
                val data = repository.getCampuses(token)
                _campuses.value = Result.success(data)
            } catch (e: Exception) {
                _campuses.value = Result.failure(e)
            }
        }
    }

    fun fetchDepartments(token: String, type: String) {
        viewModelScope.launch {
            try {
                val data = repository.getDepartments(token, type)
                _departments.value = Result.success(data)
            } catch (e: Exception) {
                _departments.value = Result.failure(e)
            }
        }
    }

    fun triggerUserSync(token: String) {
        if (isSyncingUsers) return
        isSyncingUsers = true
        
        viewModelScope.launch {
            try {
                val updatedUsers = repository.syncUsers(token)
                _userSyncState.postValue(Result.success(updatedUsers))
            } catch (e: Exception) {
                _userSyncState.postValue(Result.failure(e))
            } finally {
                isSyncingUsers = false
            }
        }
    }
}
