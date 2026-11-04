package com.stockalert.data.model

data class KLine(
    val date: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val amount: Double
)
