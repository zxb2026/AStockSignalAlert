package com.stockalert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stockalert.databinding.ActivityMainBinding
import com.stockalert.ui.MainViewModel
import com.stockalert.ui.StockAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private val adapter = StockAdapter()
    private var autoJob: Job? = null
    private var autoEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.recyclerStock.layoutManager = LinearLayoutManager(this)
        b.recyclerStock.adapter = adapter

        b.btnRefresh.setOnClickListener { vmRefresh() }
        b.btnAutoRefresh.setOnClickListener { toggleAuto() }
        b.btnSettings.setOnClickListener { showFilterSummary() }

        requestNotificationPermission()

        lifecycleScope.launch {
            vm.results.collect { list ->
                adapter.submit(list)
                b.txtSignalCount.text = "共 ${list.size} 只触发信号"
            }
        }
        lifecycleScope.launch {
            vm.passCount.collect { b.txtPassCount.text = "通过筛选 $it 只" }
        }
        lifecycleScope.launch {
            vm.loading.collect { b.progressBar.isVisible = it }
        }
        lifecycleScope.launch {
            vm.lastUpdate.collect { b.txtLastUpdate.text = "更新 $it" }
        }
        lifecycleScope.launch {
            vm.error.collect { e -> e?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() } }
        }

        vmRefresh()
        updateMarketPhase()
    }

    private fun vmRefresh() {
        lifecycleScope.launch { vm.refresh() }
    }

    private fun toggleAuto() {
        autoEnabled = !autoEnabled
        b.btnAutoRefresh.text = if (autoEnabled) "自动刷新:开" else "自动刷新:关"
        if (autoEnabled) startAutoLoop() else stopAutoLoop()
    }

    private fun startAutoLoop() {
        autoJob?.cancel()
        autoJob = lifecycleScope.launch {
            while (autoEnabled) {
                val phase = currentPhase()
                val interval = when (phase) {
                    1 -> 5_000L
                    2 -> 30_000L
                    0 -> 30_000L
                    else -> 60_000L
                }
                vm.refresh()
                updateMarketPhase()
                delay(interval)
            }
        }
    }

    private fun stopAutoLoop() {
        autoJob?.cancel()
        autoJob = null
    }

    private fun currentPhase(): Int {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return 5
        val now = cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE)
        return when {
            now < 915 -> 0
            now < 925 -> 1
            now < 1130 -> 2
            now < 1300 -> 3
            now < 1500 -> 2
            else -> 4
        }
    }

    private fun updateMarketPhase() {
        val phase = currentPhase()
        val label = when (phase) {
            0 -> "盘前"
            1 -> "集合竞价中"
            2 -> "连续竞价中"
            3 -> "午间休市"
            4 -> "已收盘"
            else -> "休市"
        }
        b.txtMarketPhase.text = "$label · ${now()}"
    }

    private fun now(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)

    private fun showFilterSummary() {
        val msg = "筛选规则（全局）：\n" +
            "· 仅沪深A股+创业板（剔除科创板/北交所）\n" +
            "· 股价 2 < P < 15\n" +
            "· 集合竞价涨幅 < 6.8%\n" +
            "· 自动过滤 ST / 停牌 / 退市 / 一字板 / 多元金融\n" +
            "· 技术信号：MACD金叉 / 均线多头·金叉 / KDJ金叉 / 放量突破"
        android.app.AlertDialog.Builder(this)
            .setTitle("筛选规则")
            .setMessage(msg)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onDestroy() {
        autoJob?.cancel()
        super.onDestroy()
    }
}
