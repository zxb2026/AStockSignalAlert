package com.stockalert.data.model

data class StockQuote(
    val code: String,
    val market: Int,
    val name: String,
    val price: Double,
    val changePct: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val prevClose: Double,
    val volume: Long,
    val turnover: Double,
    val sector: String
)
