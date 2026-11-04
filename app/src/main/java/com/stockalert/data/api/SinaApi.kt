package com.stockalert.data.api

import com.stockalert.data.model.KLine
import com.stockalert.data.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SinaApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun fetchMarketSnapshot(): List<StockQuote> = withContext(Dispatchers.IO) {
        coroutineScope {
            val sh = async { fetchNode("sh_a") }
            val sz = async { fetchNode("sz_a") }
            sh.await() + sz.await()
        }
    }

    private suspend fun fetchNode(node: String): List<StockQuote> = withContext(Dispatchers.IO) {
        val out = mutableListOf<StockQuote>()
        val total = nodeCount(node)
        val pageSize = 100
        val pages = Math.max(1, Math.ceil(total / pageSize.toDouble()).toInt())
        (1..pages).map { p ->
            async {
                val url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php" +
                    "/Market_Center.getHQNodeData?page=$p&num=$pageSize&sort=symbol&asc=1&node=$node"
                parseList(get(url))
            }
        }.awaitAll().forEach { out.addAll(it) }
        out
    }

    private fun nodeCount(node: String): Int {
        val url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php" +
            "/Market_Center.getHQNodeStockCount?node=$node"
        val body = get(url) ?: return 0
        return body.trim().trim('"').toIntOrNull() ?: 0
    }

    suspend fun fetchKLine(symbol: String, datalen: Int = 120): List<KLine> =
        withContext(Dispatchers.IO) {
            val url = "https://quotes.sina.cn/cn/api/jsonp_v2.php/var%20_=/CN_MarketDataService" +
                ".getKLineData?symbol=$symbol&scale=240&ma=no&datalen=$datalen"
            val body = get(url) ?: return@withContext emptyList()
            val jsonArr = extractJsonArray(body) ?: return@withContext emptyList()
            buildList {
                for (i in 0 until jsonArr.length()) {
                    val j = jsonArr.getJSONObject(i)
                    add(
                        KLine(
                            date = j.optString("day"),
                            open = j.optDouble("open", 0.0),
                            close = j.optDouble("close", 0.0),
                            high = j.optDouble("high", 0.0),
                            low = j.optDouble("low", 0.0),
                            volume = (j.optDouble("volume", 0.0) / 100).toLong(),
                            amount = 0.0
                        )
                    )
                }
            }
        }

    fun shouldIncludeCode(symbol: String, code: String): Boolean {
        if (symbol.startsWith("bj")) return false
        if (code.startsWith("688") || code.startsWith("689")) return false
        return true
    }

    fun symbolOf(quote: StockQuote): String {
        val pre = if (quote.market == 1) "sh" else "sz"
        return pre + quote.code
    }

    private fun parseList(body: String?): List<StockQuote> {
        if (body.isNullOrBlank()) return emptyList()
        val arr = extractJsonArray(body) ?: return emptyList()
        val out = mutableListOf<StockQuote>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val symbol = j.optString("symbol")
            val code = j.optString("code")
            if (symbol.isEmpty() || code.isEmpty()) continue
            val market = if (symbol.startsWith("sh")) 1 else if (symbol.startsWith("sz")) 0 else -1
            if (market == -1) continue
            out.add(
                StockQuote(
                    code = code,
                    market = market,
                    name = j.optString("name"),
                    price = j.optDouble("trade", 0.0),
                    changePct = j.optDouble("changepercent", 0.0),
                    open = j.optDouble("open", 0.0),
                    high = j.optDouble("high", 0.0),
                    low = j.optDouble("low", 0.0),
                    prevClose = j.optDouble("settlement", 0.0),
                    volume = j.optLong("volume", 0L) / 100,
                    turnover = j.optDouble("turnoverratio", 0.0),
                    sector = ""
                )
            )
        }
        return out
    }

    private fun extractJsonArray(body: String): JSONArray? {
        val start = body.indexOf('[')
        val end = body.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return try {
            JSONArray(body.substring(start, end + 1))
        } catch (e: Exception) {
            null
        }
    }

    private fun get(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Referer", "https://finance.sina.com.cn/")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        } catch (e: Exception) {
            null
        }
    }
}
