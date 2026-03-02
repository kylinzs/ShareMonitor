package com.codex.sharemonitor.data.quotes.eastmoney

import com.codex.sharemonitor.data.quotes.QuotesDataSource
import com.codex.sharemonitor.data.quotes.QuoteMath
import com.codex.sharemonitor.domain.model.Constituent
import com.codex.sharemonitor.domain.model.Exchange
import com.codex.sharemonitor.domain.model.HistoryPoint
import com.codex.sharemonitor.domain.model.HistorySeries
import com.codex.sharemonitor.domain.model.HistoryType
import com.codex.sharemonitor.domain.model.Index
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.Sector
import com.codex.sharemonitor.domain.model.Symbol
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 东方财富（Eastmoney）数据源实现。
 *
 * 说明：
 * - 使用多个 Eastmoney 公开接口：搜索、报价、历史走势、板块与成分股；
 * - 对返回字段做归一化映射到 domain model；
 * - 对部分字段名/结构变化做容错（例如搜索接口字段大小写/别名）。
 */
class EastmoneyQuotesDataSource(
    private val searchApi: EastmoneySearchApi,
    private val quotesApi: EastmoneyQuotesApi,
    private val historyApi: EastmoneyHistoryApi,
) : QuotesDataSource {
    override val sourceName: String = "东方财富"

    override suspend fun searchSymbols(query: String): List<Symbol> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val resp = searchApi.suggest(input = q)
        val table = resp.quotationCodeTable ?: resp.quotationCodeTableLower
        val items = (table?.data ?: table?.dataLower).orEmpty()
        return items.mapNotNull { item ->
            val quoteId = item.quoteId ?: item.quoteIdAlt ?: item.quoteIdAlt2 ?: ""
            val code = item.code ?: item.codeLower ?: return@mapNotNull null
            val name = item.name ?: item.nameLower ?: return@mapNotNull null

            val exchange = when (quoteId.substringBefore('.', missingDelimiterValue = "")) {
                "1" -> Exchange.SH
                else -> if (code.startsWith("8") || code.startsWith("4")) Exchange.BJ else Exchange.SZ
            }

            Symbol(exchange = exchange, code = code, name = name)
        }
    }

    override suspend fun getQuotes(symbols: List<Symbol>): List<Quote> {
        return symbols.mapNotNull { getQuote(it) }
    }

    override suspend fun getQuote(symbol: Symbol): Quote? {
        val now = System.currentTimeMillis()
        val secId = secIdFor(symbol)
        val fields = listOf(
            "f43",
            "f44",
            "f45",
            "f46",
            "f47",
            "f57",
            "f58",
            "f60",
            "f169",
            "f170",
            "f171",
        ).joinToString(",")

        val resp = quotesApi.stockGet(secId = secId, fields = fields)
        val d = resp.data ?: return null
        val lastPrice = d.lastPrice
        // Eastmoney 偶发会返回 data 结构但核心字段为空（可能是接口限制/字段变化/被拦截等）。
        // 这种场景下继续返回 Quote 会导致 UI 只显示占位符且不触发错误提示，因此直接视为“无报价”。
        if (lastPrice == null) return null

        val computedChange = QuoteMath.computeChange(lastPrice = lastPrice, prevClose = d.prevClose, providedChange = d.change)
        val computedChangePct = QuoteMath.computeChangePct(change = computedChange, prevClose = d.prevClose, providedChangePct = d.changePct)

        return Quote(
            symbolKey = symbol.key,
            lastPrice = lastPrice,
            change = computedChange,
            changePct = computedChangePct,
            open = d.open,
            high = d.high,
            low = d.low,
            prevClose = d.prevClose,
            volume = d.volume,
            quoteTimeEpochMillis = d.quoteTimeEpochMillis,
            fetchedAtEpochMillis = now,
            sourceName = sourceName,
        )
    }

    override suspend fun getHistory(symbol: Symbol, type: HistoryType): HistorySeries? {
        val now = System.currentTimeMillis()
        val secId = secIdFor(symbol)
        return when (type) {
            HistoryType.Intraday -> {
                val resp = historyApi.trends2Get(secId = secId, ndays = 1)
                val trends = resp.data?.trends.orEmpty()
                val points = trends.mapNotNull { parseIntradayTrend(it) }
                HistorySeries(type = type, points = points, fetchedAtEpochMillis = now, sourceName = sourceName)
            }

            HistoryType.DailyK -> {
                val resp = historyApi.klineGet(secId = secId, klt = 101, limit = 180)
                val klines = resp.data?.klines.orEmpty()
                val points = klines.mapNotNull { parseDailyKline(it) }
                HistorySeries(type = type, points = points, fetchedAtEpochMillis = now, sourceName = sourceName)
            }
        }
    }

    override suspend fun getIndices(): List<Index> {
        return listOf(
            Index(secId = "1.000001", code = "000001", name = "上证指数"),
            Index(secId = "0.399001", code = "399001", name = "深证成指"),
            Index(secId = "0.399006", code = "399006", name = "创业板指"),
        )
    }

    override suspend fun getSectors(): List<Sector> {
        val fields = "f12,f14,f2,f3,f4,f124"
        val industries = runCatching {
            quotesApi.clistGet(fs = "m:90+t:2", pageSize = 200, fields = fields)
        }.getOrNull()
        val concepts = runCatching {
            quotesApi.clistGet(fs = "m:90+t:3", pageSize = 200, fields = fields)
        }.getOrNull()

        val merged = (industries?.data?.diff.orEmpty() + concepts?.data?.diff.orEmpty())
            .mapNotNull { item ->
                val code = item.code ?: return@mapNotNull null
                val name = item.name ?: return@mapNotNull null
                Sector(
                    id = code,
                    name = name,
                    lastPrice = item.lastPrice,
                    change = item.change,
                    changePct = item.changePct,
                    quoteTimeEpochMillis = item.quoteTimeEpochSeconds?.let { it * 1000L },
                )
            }

        return merged.distinctBy { it.id }
    }

    override suspend fun getSectorConstituents(sector: Sector): List<Constituent> {
        val now = System.currentTimeMillis()
        val fields = "f12,f13,f14,f2,f3,f4,f124"
        val resp = quotesApi.clistGet(fs = "b:${sector.id}", pageSize = 200, fields = fields)
        val diff = resp.data?.diff.orEmpty()
        return diff.mapNotNull { item ->
            val code = item.code ?: return@mapNotNull null
            val name = item.name ?: return@mapNotNull null
            val exchange = when (item.market) {
                1 -> Exchange.SH
                0 -> if (code.startsWith("8") || code.startsWith("4")) Exchange.BJ else Exchange.SZ
                else -> if (code.startsWith("8") || code.startsWith("4")) Exchange.BJ else Exchange.SZ
            }
            val symbol = Symbol(exchange = exchange, code = code, name = name)
            val quoteTimeEpochMillis = item.quoteTimeEpochSeconds?.let { it * 1000L }
            Constituent(
                symbol = symbol,
                quote = Quote(
                    symbolKey = symbol.key,
                    lastPrice = item.lastPrice,
                    change = item.change,
                    changePct = item.changePct,
                    open = null,
                    high = null,
                    low = null,
                    prevClose = null,
                    volume = null,
                    quoteTimeEpochMillis = quoteTimeEpochMillis,
                    fetchedAtEpochMillis = now,
                    sourceName = sourceName,
                ),
            )
        }
    }

    private fun secIdFor(symbol: Symbol): String {
        val market = when (symbol.exchange) {
            Exchange.SH -> 1
            Exchange.SZ -> 0
            Exchange.BJ -> 0
        }
        return "$market.${symbol.code}"
    }

    private fun parseIntradayTrend(value: String): HistoryPoint? {
        val parts = value.split(",")
        if (parts.size < 3) return null

        val epochMillis = parseLocalDateTimeEpochMillis(parts[0]) ?: return null
        val close = parts[2].toDoubleOrNull() ?: return null
        return HistoryPoint(epochMillis = epochMillis, close = close)
    }

    private fun parseDailyKline(value: String): HistoryPoint? {
        val parts = value.split(",")
        if (parts.size < 6) return null

        val epochMillis = parseLocalDateEpochMillis(parts[0]) ?: return null
        val open = parts[1].toDoubleOrNull()
        val close = parts[2].toDoubleOrNull() ?: return null
        val high = parts[3].toDoubleOrNull()
        val low = parts[4].toDoubleOrNull()
        val volume = parts[5].toLongOrNull()

        return HistoryPoint(
            epochMillis = epochMillis,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume,
        )
    }

    private fun parseLocalDateTimeEpochMillis(value: String): Long? {
        val formats = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        )
        for (formatter in formats) {
            val ldt = runCatching { LocalDateTime.parse(value, formatter) }.getOrNull() ?: continue
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return null
    }

    private fun parseLocalDateEpochMillis(value: String): Long? {
        val formats = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
        )
        for (formatter in formats) {
            val ld = runCatching { LocalDate.parse(value, formatter) }.getOrNull() ?: continue
            return ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return null
    }
}
