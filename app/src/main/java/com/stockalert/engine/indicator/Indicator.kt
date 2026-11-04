package com.stockalert.engine.indicator

import com.stockalert.data.model.KLine

object Indicator {

    fun ma(values: List<Double>, period: Int): List<Double> {
        val out = MutableList(values.size) { 0.0 }
        var sum = 0.0
        for (i in values.indices) {
            sum += values[i]
            if (i >= period) sum -= values[i - period]
            if (i >= period - 1) out[i] = sum / period
        }
        return out
    }

    fun macd(closes: List<Double>, fast: Int = 12, slow: Int = 26, signal: Int = 9): Pair<List<Double>, List<Double>> {
        val emaFast = ema(closes, fast)
        val emaSlow = ema(closes, slow)
        val dif = emaFast.zip(emaSlow) { a, b -> a - b }
        val dea = ema(dif, signal)
        return dif to dea
    }

    fun kdj(klines: List<KLine>, n: Int = 9): Pair<List<Double>, List<Double>> {
        val size = klines.size
        val rsv = MutableList(size) { 0.0 }
        val k = MutableList(size) { 50.0 }
        val d = MutableList(size) { 50.0 }
        for (i in 0 until size) {
            val start = Math.max(0, i - n + 1)
            var high = Double.MIN_VALUE; var low = Double.MAX_VALUE
            for (j in start..i) {
                high = Math.max(high, klines[j].high)
                low = Math.min(low, klines[j].low)
            }
            val range = high - low
            rsv[i] = if (range > 0) (klines[i].close - low) / range * 100 else 50.0
            if (i == 0) {
                k[0] = 50.0; d[0] = 50.0
            } else {
                k[i] = (2.0 / 3) * k[i - 1] + (1.0 / 3) * rsv[i]
                d[i] = (2.0 / 3) * d[i - 1] + (1.0 / 3) * k[i]
            }
        }
        return k to d
    }

    private fun ema(data: List<Double>, period: Int): List<Double> {
        if (data.isEmpty()) return emptyList()
        val out = MutableList(data.size) { 0.0 }
        val alpha = 2.0 / (period + 1)
        out[0] = data[0]
        for (i in 1 until data.size) out[i] = alpha * data[i] + (1 - alpha) * out[i - 1]
        return out
    }
}
