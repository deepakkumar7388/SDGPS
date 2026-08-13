package com.example.digitalpass

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalpass.database.GatePassEntity
import com.example.digitalpass.database.VisitorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PassSyncViewModel(
    private val gatePassRepository: GatePassRepository,
    private val interInstitutionalGatePassRepository: InterInstitutionalGatePassRepository,
    private val visitorRepository: VisitorRepository
) : ViewModel() {

    private val _gatePassSyncState = MutableLiveData<Result<List<GatePassEntity>>>()
    val gatePassSyncState: LiveData<Result<List<GatePassEntity>>> = _gatePassSyncState

    private val _visitorSyncState = MutableLiveData<Result<List<VisitorEntity>>>()
    val visitorSyncState: LiveData<Result<List<VisitorEntity>>> = _visitorSyncState

    private val _activeGatePasses = MutableLiveData<List<GatePassEntity>>()
    val activeGatePasses: LiveData<List<GatePassEntity>> = _activeGatePasses

    private val _historicalGatePasses = MutableLiveData<List<GatePassEntity>>()
    val historicalGatePasses: LiveData<List<GatePassEntity>> = _historicalGatePasses

    private val _interInstitutionalSyncState = MutableLiveData<Result<List<GatePassEntity>>>()
    val interInstitutionalSyncState: LiveData<Result<List<GatePassEntity>>> = _interInstitutionalSyncState

    private val _activeInterInstitutional = MutableLiveData<List<GatePassEntity>>()
    val activeInterInstitutional: LiveData<List<GatePassEntity>> = _activeInterInstitutional

    private val _historicalInterInstitutional = MutableLiveData<List<GatePassEntity>>()
    val historicalInterInstitutional: LiveData<List<GatePassEntity>> = _historicalInterInstitutional

    private val _activeVisitors = MutableLiveData<List<VisitorEntity>>()
    val activeVisitors: LiveData<List<VisitorEntity>> = _activeVisitors

    private val _historicalVisitors = MutableLiveData<List<VisitorEntity>>()
    val historicalVisitors: LiveData<List<VisitorEntity>> = _historicalVisitors

    private var isSyncingGatePasses = false
    private var isSyncingVisitors = false
    private var isSyncingInterInstitutional = false

    fun triggerGatePassSync(token: String) {
        if (isSyncingGatePasses) return
        isSyncingGatePasses = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = gatePassRepository.syncGatePasses(token)
                _gatePassSyncState.postValue(Result.success(data))
            } catch (e: Exception) {
                _gatePassSyncState.postValue(Result.failure(e))
            } finally {
                isSyncingGatePasses = false
            }
        }
    }

    fun triggerInterInstitutionalSync(token: String) {
        if (isSyncingInterInstitutional) return
        isSyncingInterInstitutional = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = interInstitutionalGatePassRepository.syncInterInstitutionalGatePasses(token)
                _interInstitutionalSyncState.postValue(Result.success(data))
            } catch (e: Exception) {
                _interInstitutionalSyncState.postValue(Result.failure(e))
            } finally {
                isSyncingInterInstitutional = false
            }
        }
    }

    fun triggerVisitorSync(token: String) {
        if (isSyncingVisitors) return
        isSyncingVisitors = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = visitorRepository.syncVisitors(token)
                _visitorSyncState.postValue(Result.success(data))
            } catch (e: Exception) {
                _visitorSyncState.postValue(Result.failure(e))
            } finally {
                isSyncingVisitors = false
            }
        }
    }

    fun loadActiveGatePasses(todayStart: String, todayEnd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = gatePassRepository.getActiveGatePasses(todayStart, todayEnd)
                _activeGatePasses.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadHistoricalGatePasses(todayStart: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = gatePassRepository.getHistoricalGatePasses(todayStart)
                _historicalGatePasses.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadActiveVisitors(todayStart: String, todayEnd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = visitorRepository.getActiveVisitors(todayStart, todayEnd)
                _activeVisitors.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadActiveInterInstitutionalGatePasses(todayStart: String, todayEnd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = interInstitutionalGatePassRepository.getActiveGatePasses(todayStart, todayEnd)
                _activeInterInstitutional.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadHistoricalVisitors(todayStart: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = visitorRepository.getHistoricalVisitors(todayStart)
                _historicalVisitors.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadHistoricalInterInstitutionalGatePasses(todayStart: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = interInstitutionalGatePassRepository.getHistoricalGatePasses(todayStart)
                _historicalInterInstitutional.postValue(list)
            } catch (e: Exception) {}
        }
    }

    private val _rangeGatePasses = MutableLiveData<List<GatePassEntity>>()
    val rangeGatePasses: LiveData<List<GatePassEntity>> = _rangeGatePasses

    private val _rangeInterInstitutional = MutableLiveData<List<GatePassEntity>>()
    val rangeInterInstitutional: LiveData<List<GatePassEntity>> = _rangeInterInstitutional

    private val _rangeVisitors = MutableLiveData<List<VisitorEntity>>()
    val rangeVisitors: LiveData<List<VisitorEntity>> = _rangeVisitors

    fun loadGatePassesByRange(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = gatePassRepository.getGatePassesByDateRange(startDate, endDate)
                _rangeGatePasses.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadInterInstitutionalByRange(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = interInstitutionalGatePassRepository.getGatePassesByDateRange(startDate, endDate)
                _rangeInterInstitutional.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadVisitorsByRange(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = visitorRepository.getVisitorsByDateRange(startDate, endDate)
                _rangeVisitors.postValue(list)
            } catch (e: Exception) {}
        }
    }

    private val _selfGatePasses = MutableLiveData<List<GatePassEntity>>()
    val selfGatePasses: LiveData<List<GatePassEntity>> = _selfGatePasses

    private val _selfInterInstitutional = MutableLiveData<List<GatePassEntity>>()
    val selfInterInstitutional: LiveData<List<GatePassEntity>> = _selfInterInstitutional

    fun loadSelfGatePasses(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = gatePassRepository.getGatePassesByEmail(email)
                _selfGatePasses.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun loadSelfInterInstitutional(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = interInstitutionalGatePassRepository.getGatePassesByEmail(email)
                _selfInterInstitutional.postValue(list)
            } catch (e: Exception) {}
        }
    }
}
