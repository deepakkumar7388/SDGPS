package com.example.digitalpass

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalpass.database.GatePassEntity
import com.example.digitalpass.database.VisitorEntity
import kotlinx.coroutines.launch

class PassSyncViewModel(
    private val gatePassRepository: GatePassRepository,
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

    private val _activeVisitors = MutableLiveData<List<VisitorEntity>>()
    val activeVisitors: LiveData<List<VisitorEntity>> = _activeVisitors

    private val _historicalVisitors = MutableLiveData<List<VisitorEntity>>()
    val historicalVisitors: LiveData<List<VisitorEntity>> = _historicalVisitors

    private var isSyncingGatePasses = false
    private var isSyncingVisitors = false

    fun triggerGatePassSync(token: String) {
        if (isSyncingGatePasses) return
        isSyncingGatePasses = true
        viewModelScope.launch {
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

    fun triggerVisitorSync(token: String) {
        if (isSyncingVisitors) return
        isSyncingVisitors = true
        viewModelScope.launch {
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
        viewModelScope.launch {
            val list = gatePassRepository.getActiveGatePasses(todayStart, todayEnd)
            _activeGatePasses.postValue(list)
        }
    }

    fun loadHistoricalGatePasses(todayStart: String) {
        viewModelScope.launch {
            val list = gatePassRepository.getHistoricalGatePasses(todayStart)
            _historicalGatePasses.postValue(list)
        }
    }

    fun loadActiveVisitors(todayStart: String, todayEnd: String) {
        viewModelScope.launch {
            val list = visitorRepository.getActiveVisitors(todayStart, todayEnd)
            _activeVisitors.postValue(list)
        }
    }

    fun loadHistoricalVisitors(todayStart: String) {
        viewModelScope.launch {
            val list = visitorRepository.getHistoricalVisitors(todayStart)
            _historicalVisitors.postValue(list)
        }
    }

    private val _rangeGatePasses = MutableLiveData<List<GatePassEntity>>()
    val rangeGatePasses: LiveData<List<GatePassEntity>> = _rangeGatePasses

    private val _rangeVisitors = MutableLiveData<List<VisitorEntity>>()
    val rangeVisitors: LiveData<List<VisitorEntity>> = _rangeVisitors

    fun loadGatePassesByRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            val list = gatePassRepository.getGatePassesByDateRange(startDate, endDate)
            _rangeGatePasses.postValue(list)
        }
    }

    fun loadVisitorsByRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            val list = visitorRepository.getVisitorsByDateRange(startDate, endDate)
            _rangeVisitors.postValue(list)
        }
    }

    private val _selfGatePasses = MutableLiveData<List<GatePassEntity>>()
    val selfGatePasses: LiveData<List<GatePassEntity>> = _selfGatePasses

    fun loadSelfGatePasses(email: String) {
        viewModelScope.launch {
            val list = gatePassRepository.getGatePassesByEmail(email)
            _selfGatePasses.postValue(list)
        }
    }
}
