package com.stockalert.engine

import com.stockalert.data.model.KLine
import com.stockalert.engine.indicator.Indicator

object SignalEngine {

    fun detect(klines: List<KLine>): List<String> {
        if (klines.size < 60) return emptyList()
        val signals = mutableListOf<String>()

        val closes = klines.map { it.close }

        val (dif, dea) = Indicator.macd(closes)
        if (dif.size >= 3 && dea.size >= 3) {
            val n = dif.size
            if (dif[n - 2] <= dea[n - 2] && dif[n - 1] > dea[n - 1]) {
                signals.add("MACD金叉")
            }
        }

        val ma5 = Indicator.ma(closes, 5)
        val ma10 = Indicator.ma(closes, 10)
        val ma20 = Indicator.ma(closes, 20)
        val n = closes.size
        if (ma5[n - 1] > ma10[n - 1] && ma10[n - 1] > ma20[n - 1]) {
            signals.add("均线多头")
        } else if (n >= 2 && ma5[n - 2] <= ma10[n - 2] && ma5[n - 1] > ma10[n - 1]) {
            signals.add("均线金叉")
        }

        val (k, d) = Indicator.kdj(klines)
        if (k.size >= 3 && d.size >= 3) {
            val kn = k.size
            if (k[kn - 2] <= d[kn - 2] && k[kn - 1] > d[kn - 1] && k[kn - 1] < 80) {
                signals.add("KDJ金叉")
            }
        }

        if (n >= 21) {
            val vol = klines.map { it.volume.toDouble() }
            val avg20 = vol.subList(n - 20, n - 1).average()
            val todayVol = vol[n - 1]
            val high20 = klines.subList(n - 20, n - 1).maxOfOrNull { it.high } ?: 0.0
            val breakAbove = closes[n - 1] > high20
            if (avg20 > 0 && todayVol >= avg20 * 1.5 && breakAbove) {
                signals.add("放量突破")
            }
        }

        return signals
    }
}
