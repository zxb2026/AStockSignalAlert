package com.stockalert.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockalert.data.api.SinaApi
import com.stockalert.data.model.StockQuote
import com.stockalert.engine.FilterEngine
import com.stockalert.engine.SignalEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StockResult(
    val quote: StockQuote,
    val signals: List<String>
) {
    val hasSignal: Boolean get() = signals.isNotEmpty()
}

class MainViewModel(private val api: SinaApi = SinaApi()) : ViewModel() {

    private val _results = MutableStateFlow<List<StockResult>>(emptyList())
    val results: StateFlow<List<StockResult>> = _results

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _lastUpdate = MutableStateFlow("--:--:--")
    val lastUpdate: StateFlow<String> = _lastUpdate

    private val _passCount = MutableStateFlow(0)
    val passCount: StateFlow<Int> = _passCount

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun refresh() {
        if (_loading.value) return
        _loading.value = true
        _error.value = null
        try {
            val all = api.fetchMarketSnapshot()

            val candidates = all.filter {
                api.shouldIncludeCode(
                    (if (it.market == 1) "sh" else "sz") + it.code,
                    it.code
                ) && FilterEngine.pass(it)
            }

            val pass = candidates.size
            _passCount.value = pass

            val computed = withContext(Dispatchers.IO) {
                candidates.map { c ->
                    async {
                        val sym = api.symbolOf(c)
                        val kl = api.fetchKLine(sym, 120)
                        StockResult(c, SignalEngine.detect(kl))
                    }
                }.awaitAll()
            }

            val withSignal = computed.filter { it.hasSignal }
            _results.value = withSignal
            _lastUpdate.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
        } catch (e: Exception) {
            _error.value = "刷新失败：" + (e.message ?: e.javaClass.simpleName)
        } finally {
            _loading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
