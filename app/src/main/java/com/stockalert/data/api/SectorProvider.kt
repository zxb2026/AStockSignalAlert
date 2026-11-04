package com.stockalert.data.api

import com.stockalert.data.model.StockQuote

object SectorProvider {

    val MULTI_FINANCE_CODES: Set<String> = setOf(
        "000563", // 陕国投A
        "600816", // 建元信托
        "000617", // 中油资本
        "600705", // 中航产融
        "600390", // 五矿资本
        "600318", // 新力金融
        "600643", // 爱建集团
        "600061", // 国投资本
        "000996", // 中国中期
        "600755", // 厦门国贸
        "600901", // 苏租股份
        "000415", // 渤海租赁
        "600830", // 香溢融通
        "600783", // 鲁信创投
        "600053"  // 九鼎投资
    ).toSet()

    fun isMultiFinance(quote: StockQuote): Boolean {
        val s = quote.sector
        if (s.isNotEmpty() && (s.contains("多元金融") || s.contains("信托") ||
                s.contains("期货") || s.contains("金控") || s.contains("融资租赁") ||
                s.contains("保险经纪") || s.contains("创投"))) return true
        if (quote.code in MULTI_FINANCE_CODES) return true
        val n = quote.name
        if (n.isBlank()) return false
        return n.contains("信托") || n.contains("金租") || n.contains("融资租赁") ||
            n.contains("期货") || n.contains("创投")
    }
}
