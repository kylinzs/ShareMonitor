package com.codex.sharemonitor.data.quotes.tencent

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
import okhttp3.ResponseBody
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 腾讯数据源实现。
 *
 * 说明：
 * - 搜索主要通过 Smartbox 接口；
 * - 报价优先尝试 qt 快照接口，失败时回退到分钟线序列推导（open/high/low/last）；
 * - 历史走势支持分时与日K（qfq/不复权结果以接口返回为准）。
 */
class TencentQuotesDataSource(
    private val minuteApi: TencentMinuteApi,
    private val fqKlineApi: TencentFqKlineApi,
    private val qtQuoteApi: TencentQtQuoteApi,
    private val smartboxApi: TencentSmartboxApi,
) : QuotesDataSource {
    override val sourceName: String = "腾讯"

    override suspend fun searchSymbols(query: String): List<Symbol> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val resp = runCatching { smartboxApi.suggest(query = q) }.getOrNull() ?: return emptyList()
        val text = resp.decodeTencentText()
        return parseSmartboxSymbols(text)
    }

    override suspend fun getQuotes(symbols: List<Symbol>): List<Quote> {
        val now = System.currentTimeMillis()
        val unique = symbols.distinctBy { it.key }
        if (unique.isEmpty()) return emptyList()

        val codes = unique.map { tencentCode(it) }.distinct()
        val snapshots = runCatching {
            val resp = qtQuoteApi.quote(codes = codes.joinToString(","))
            parseQtQuoteResponse(resp.decodeTencentText())
        }.getOrNull()

        return if (snapshots != null) {
            unique.mapNotNull { symbol ->
                val key = tencentCode(symbol)
                snapshots[key]?.toQuote(symbol = symbol, now = now, sourceName = sourceName)
                    ?: getQuoteFromMinuteSeries(symbol = symbol, now = now)
            }
        } else {
            unique.mapNotNull { getQuoteFromMinuteSeries(symbol = it, now = now) }
        }
    }

    override suspend fun getQuote(symbol: Symbol): Quote? {
        return getQuotes(listOf(symbol)).firstOrNull()
    }

    override suspend fun getHistory(symbol: Symbol, type: HistoryType): HistorySeries? {
        val now = System.currentTimeMillis()
        val code = tencentCode(symbol)
        return when (type) {
            HistoryType.Intraday -> {
                val resp = minuteApi.minuteQuery(code = code)
                val raw = resp.data?.get(code)?.data?.data.orEmpty()
                val points = raw.mapNotNull { parseMinutePoint(it) }
                HistorySeries(type = type, points = points, fetchedAtEpochMillis = now, sourceName = sourceName)
            }

            HistoryType.DailyK -> {
                val param = "$code,day,,,180,qfq"
                val resp = fqKlineApi.fqKlineGet(param = param)
                val stock = resp.data?.get(code)
                val rows = stock?.qfqDay ?: stock?.day.orEmpty()
                val points = rows.mapNotNull { parseDailyRow(it) }
                HistorySeries(type = type, points = points, fetchedAtEpochMillis = now, sourceName = sourceName)
            }
        }
    }

    override suspend fun getIndices(): List<Index> = emptyList()

    override suspend fun getSectors(): List<Sector> = emptyList()

    override suspend fun getSectorConstituents(sector: Sector): List<Constituent> = emptyList()

    private fun tencentCode(symbol: Symbol): String {
        val prefix = when (symbol.exchange) {
            Exchange.SH -> "sh"
            Exchange.SZ -> "sz"
            Exchange.BJ -> "bj"
        }
        return "$prefix${symbol.code}"
    }

    private data class TencentQtSnapshot(
        val name: String?,
        val code: String?,
        val lastPrice: Double?,
        val prevClose: Double?,
        val open: Double?,
        val high: Double?,
        val low: Double?,
        val volume: Long?,
        val quoteTimeEpochMillis: Long?,
        val change: Double?,
        val changePct: Double?,
    ) {
        fun toQuote(symbol: Symbol, now: Long, sourceName: String): Quote {
            val computedChange = QuoteMath.computeChange(lastPrice = lastPrice, prevClose = prevClose, providedChange = change)
            val computedChangePct = QuoteMath.computeChangePct(change = computedChange, prevClose = prevClose, providedChangePct = changePct)
            return Quote(
                symbolKey = symbol.key,
                lastPrice = lastPrice,
                change = computedChange,
                changePct = computedChangePct,
                open = open,
                high = high,
                low = low,
                prevClose = prevClose,
                volume = volume,
                quoteTimeEpochMillis = quoteTimeEpochMillis,
                fetchedAtEpochMillis = now,
                sourceName = sourceName,
            )
        }
    }

    private fun parseQtQuoteResponse(text: String): Map<String, TencentQtSnapshot> {
        val map = LinkedHashMap<String, TencentQtSnapshot>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val eq = line.indexOf('=')
            if (eq <= 0) continue

            val rawKey = line.substring(0, eq).trim()
            val key = rawKey
                .removePrefix("v_")
                .removePrefix("s_")
                .removePrefix("v_s_")
            val valuePart = line.substring(eq + 1)
            val firstQuote = valuePart.indexOf('"')
            val lastQuote = valuePart.lastIndexOf('"')
            if (firstQuote < 0 || lastQuote <= firstQuote) continue

            val payload = valuePart.substring(firstQuote + 1, lastQuote)
            if (payload.isEmpty()) continue

            val fields = payload.split('~')
            if (fields.size < 6) continue

            val name = fields.getOrNull(1)
            val code = fields.getOrNull(2)
            val lastPrice = fields.getOrNull(3)?.toDoubleOrNull()
            val prevClose = fields.getOrNull(4)?.toDoubleOrNull()
            val open = fields.getOrNull(5)?.toDoubleOrNull()
            val volume = fields.getOrNull(6)?.toDoubleOrNull()?.toLong()
            val quoteTimeEpochMillis = parseTencentQuoteTimeEpochMillis(fields.getOrNull(30))
            val change = fields.getOrNull(31)?.toDoubleOrNull()
            val changePct = fields.getOrNull(32)?.toDoubleOrNull()
            val high = fields.getOrNull(33)?.toDoubleOrNull()
            val low = fields.getOrNull(34)?.toDoubleOrNull()

            map[key] = TencentQtSnapshot(
                name = name,
                code = code,
                lastPrice = lastPrice,
                prevClose = prevClose,
                open = open,
                high = high,
                low = low,
                volume = volume,
                quoteTimeEpochMillis = quoteTimeEpochMillis,
                change = change,
                changePct = changePct,
            )
        }
        return map
    }

    private fun parseTencentQuoteTimeEpochMillis(value: String?): Long? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return null

        return when {
            v.length == 14 && v.all { it.isDigit() } -> {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
                val dt = runCatching { LocalDateTime.parse(v, formatter) }.getOrNull() ?: return null
                dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            v.contains(":") -> {
                val formats = listOf(
                    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US),
                    DateTimeFormatter.ofPattern("HH:mm", Locale.US),
                )
                for (formatter in formats) {
                    val t = runCatching { LocalTime.parse(v, formatter) }.getOrNull() ?: continue
                    val dt = LocalDate.now().atTime(t)
                    return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                null
            }

            else -> null
        }
    }

    private fun parseSmartboxSymbols(text: String): List<Symbol> {
        val hintLine = text.lineSequence().firstOrNull { it.contains("v_hint=\"") } ?: return emptyList()
        val hint = Regex("v_hint=\"(.*)\"").find(hintLine)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (hint.isEmpty() || hint == "N") return emptyList()

        val decoded = decodeUnicodeEscapes(hint)
        val records = decoded.split('^').filter { it.isNotBlank() }
        return records.mapNotNull { record ->
            val fields = record.split('~')
            parseSmartboxRecord(fields)
        }.distinctBy { it.key }
    }

    private fun parseSmartboxRecord(fields: List<String>): Symbol? {
        if (fields.size < 3) return null
        var exchange = fields[0].trim().lowercase(Locale.US)
        var symbol = fields[1].trim()
        val name = fields[2].trim()
        val type = fields.getOrNull(4)?.trim().orEmpty()

        if (type.isNotEmpty() && !type.contains("GP", ignoreCase = true)) return null

        if (symbol.contains(".")) {
            val parts = symbol.split(".", limit = 2)
            if (parts.size == 2) {
                symbol = parts[0]
                exchange = parts[1].lowercase(Locale.US)
            }
        } else {
            val lower = symbol.lowercase(Locale.US)
            if (lower.length > 2 && (lower.startsWith("sh") || lower.startsWith("sz") || lower.startsWith("bj"))) {
                exchange = lower.substring(0, 2)
                symbol = lower.substring(2)
            }
        }

        val code = symbol.filter { it.isDigit() }
        if (code.isEmpty()) return null

        val ex = when (exchange) {
            "sh" -> Exchange.SH
            "sz" -> Exchange.SZ
            "bj" -> Exchange.BJ
            else -> return null
        }

        return Symbol(exchange = ex, code = code, name = name)
    }

    private fun decodeUnicodeEscapes(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                val n = value[i + 1]
                if (n == 'u' && i + 5 < value.length) {
                    val hex = value.substring(i + 2, i + 6)
                    val ch = hex.toIntOrNull(16)?.toChar()
                    if (ch != null) {
                        sb.append(ch)
                        i += 6
                        continue
                    }
                }
            }
            sb.append(c)
            i += 1
        }
        return sb.toString()
    }

    private fun ResponseBody.decodeTencentText(): String {
        val bytes = runCatching { bytes() }.getOrElse { return "" }
        return runCatching { String(bytes, Charset.forName("GBK")) }.getOrElse { String(bytes, Charsets.UTF_8) }
    }

    private suspend fun getQuoteFromMinuteSeries(symbol: Symbol, now: Long): Quote? {
        val code = tencentCode(symbol)
        val series = runCatching { minuteApi.minuteQuery(code = code) }.getOrNull()
        val data = series?.data?.get(code)?.data?.data.orEmpty()
        if (data.isEmpty()) return null

        val points = data.mapNotNull { parseMinutePoint(it) }
        if (points.isEmpty()) return null

        val open = points.first().close
        val last = points.last().close
        val high = points.maxOf { it.close }
        val low = points.minOf { it.close }

        return Quote(
            symbolKey = symbol.key,
            lastPrice = last,
            change = null,
            changePct = null,
            open = open,
            high = high,
            low = low,
            prevClose = null,
            volume = null,
            quoteTimeEpochMillis = points.last().epochMillis,
            fetchedAtEpochMillis = now,
            sourceName = sourceName,
        )
    }

    private fun parseMinutePoint(value: String): HistoryPoint? {
        val parts = value.trim().split(" ")
        if (parts.size < 2) return null

        val time = parts[0]
        val price = parts[1].toDoubleOrNull() ?: return null
        val epoch = parseTodayHmEpochMillis(time) ?: return null
        return HistoryPoint(epochMillis = epoch, close = price)
    }

    private fun parseTodayHmEpochMillis(hhmm: String): Long? {
        if (hhmm.length != 4) return null
        val hour = hhmm.substring(0, 2).toIntOrNull() ?: return null
        val minute = hhmm.substring(2, 4).toIntOrNull() ?: return null
        val date = LocalDate.now()
        val dt = date.atTime(LocalTime.of(hour, minute))
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseDailyRow(row: List<String>): HistoryPoint? {
        if (row.size < 6) return null
        val date = parseDateEpochMillis(row[0]) ?: return null
        val open = row[1].toDoubleOrNull()
        val close = row[2].toDoubleOrNull() ?: return null
        val high = row[3].toDoubleOrNull()
        val low = row[4].toDoubleOrNull()
        val volume = row[5].toLongOrNull()
        return HistoryPoint(
            epochMillis = date,
            open = open,
            close = close,
            high = high,
            low = low,
            volume = volume,
        )
    }

    private fun parseDateEpochMillis(value: String): Long? {
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
