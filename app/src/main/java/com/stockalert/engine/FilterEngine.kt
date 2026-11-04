package com.stockalert.engine

import com.stockalert.data.api.SectorProvider
import com.stockalert.data.model.StockQuote

object FilterEngine {

    val MIN_PRICE = 2.0
    val MAX_PRICE = 15.0
    val MAX_AUCTION_PCT = 6.8

    fun pass(quote: StockQuote): Boolean {
        if (!inScope(quote)) return false
        if (isSt(quote)) return false
        if (isSuspended(quote)) return false
        if (isDelisted(quote)) return false
        if (!priceOk(quote)) return false
        if (!auctionPctOk(quote)) return false
        if (isOneWordLimit(quote)) return false
        if (SectorProvider.isMultiFinance(quote)) return false
        return true
    }

    private fun inScope(quote: StockQuote): Boolean {
        val code = quote.code
        if (code.startsWith("688") || code.startsWith("689")) return false
        if (quote.market != 0 && quote.market != 1) return false
        if (code.startsWith("4") || code.startsWith("8") || code.startsWith("92")) return false
        return true
    }

    private fun priceOk(quote: StockQuote): Boolean =
        quote.price > MIN_PRICE && quote.price < MAX_PRICE

    private fun auctionPctOk(quote: StockQuote): Boolean =
        quote.changePct < MAX_AUCTION_PCT

    private fun isSt(quote: StockQuote): Boolean {
        val n = quote.name
        return n.contains("ST") || n.contains("st") || n.startsWith("S")
    }

    private fun isSuspended(quote: StockQuote): Boolean =
        quote.price <= 0 || quote.volume <= 0

    private fun isDelisted(quote: StockQuote): Boolean {
        val n = quote.name
        return n.contains("退")
    }

    private fun isOneWordLimit(quote: StockQuote): Boolean {
        if (quote.prevClose <= 0) return false
        val limitPct = if (quote.market == 0 && quote.code.startsWith("30")) 0.20 else 0.10
        val limitPrice = Math.round(quote.prevClose * (1 + limitPct) * 100.0) / 100.0
        val atLimit = quote.changePct >= limitPct * 100 - 0.3
        val o = quote.open; val h = quote.high; val l = quote.low
        return atLimit && o > 0 && h > 0 && l > 0 &&
            Math.abs(o - h) < 0.001 && Math.abs(o - l) < 0.001 &&
            Math.abs(o - limitPrice) < 0.01
    }

    fun rejectReason(quote: StockQuote): String {
        if (!inScope(quote)) return "非沪深A股/创业板"
        if (isSt(quote)) return "ST股"
        if (isSuspended(quote)) return "停牌/无成交"
        if (isDelisted(quote)) return "退市"
        if (!priceOk(quote)) return "股价不在2~15"
        if (!auctionPctOk(quote)) return "竞价涨幅>=6.8%"
        if (isOneWordLimit(quote)) return "一字板"
        if (SectorProvider.isMultiFinance(quote)) return "多元金融"
        return ""
    }
}
