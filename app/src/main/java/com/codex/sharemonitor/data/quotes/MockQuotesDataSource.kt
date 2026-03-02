package com.codex.sharemonitor.data.quotes

import com.codex.sharemonitor.domain.model.Constituent
import com.codex.sharemonitor.domain.model.Exchange
import com.codex.sharemonitor.domain.model.HistoryPoint
import com.codex.sharemonitor.domain.model.HistorySeries
import com.codex.sharemonitor.domain.model.HistoryType
import com.codex.sharemonitor.domain.model.Index
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.Sector
import com.codex.sharemonitor.domain.model.Symbol
import kotlin.math.sin

/**
 * Mock 行情数据源（用于离线开发/演示）。
 *
 * 特点：
 * - 搜索仅在固定 symbol 列表中匹配；
 * - 报价/历史数据使用时间函数生成“可变化”的确定性序列，便于观察 UI 刷新与曲线渲染；
 * - 不代表真实市场规则，仅用于 MVP 验证。
 */
class MockQuotesDataSource : QuotesDataSource {
    override val sourceName: String = "Mock"

    private val symbols: List<Symbol> = listOf(
        Symbol(Exchange.SH, "600519", "贵州茅台"),
        Symbol(Exchange.SZ, "000001", "平安银行"),
        Symbol(Exchange.SZ, "300750", "宁德时代"),
        Symbol(Exchange.BJ, "830799", "艾融软件"),
    )

    private val indices: List<Index> = listOf(
        Index(secId = "1.000001", code = "000001", name = "上证指数"),
        Index(secId = "0.399001", code = "399001", name = "深证成指"),
        Index(secId = "0.399006", code = "399006", name = "创业板指"),
    )

    private val sectors: List<Sector> = listOf(
        Sector(id = "BK001", name = "半导体"),
        Sector(id = "BK002", name = "新能源车"),
        Sector(id = "BK003", name = "AI"),
    )

    override suspend fun searchSymbols(query: String): List<Symbol> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return symbols.filter { it.code.contains(q) || it.name.contains(q) }
    }

    override suspend fun getQuotes(symbols: List<Symbol>): List<Quote> {
        val now = System.currentTimeMillis()
        return symbols.mapIndexed { index, symbol ->
            val base = 10.0 + index * 50.0
            val wave = sin((now / 60_000.0) + index) * 0.5
            val last = base + wave
            Quote(
                symbolKey = symbol.key,
                lastPrice = last,
                change = wave,
                changePct = (wave / base) * 100.0,
                open = base,
                high = base + 1.0,
                low = base - 1.0,
                prevClose = base - 0.2,
                volume = 1_000_000L + (index * 10_000L),
                quoteTimeEpochMillis = now,
                fetchedAtEpochMillis = now,
                sourceName = sourceName,
            )
        }
    }

    override suspend fun getQuote(symbol: Symbol): Quote? {
        return getQuotes(listOf(symbol)).firstOrNull()
    }

    override suspend fun getHistory(symbol: Symbol, type: HistoryType): HistorySeries? {
        val now = System.currentTimeMillis()
        val points = when (type) {
            HistoryType.Intraday -> (0 until 240).map { i ->
                val t = now - (240 - i) * 60_000L
                val p = 10.0 + sin(i / 10.0) * 0.8
                HistoryPoint(epochMillis = t, close = p)
            }

            HistoryType.DailyK -> (0 until 180).map { i ->
                val t = now - (180 - i) * 86_400_000L
                val close = 10.0 + sin(i / 8.0) * 1.5
                HistoryPoint(
                    epochMillis = t,
                    open = close - 0.2,
                    high = close + 0.6,
                    low = close - 0.7,
                    close = close,
                    volume = 900_000L + i * 1_000L,
                )
            }
        }

        return HistorySeries(
            type = type,
            points = points,
            fetchedAtEpochMillis = now,
            sourceName = sourceName,
        )
    }

    override suspend fun getIndices(): List<Index> = indices

    override suspend fun getSectors(): List<Sector> = sectors

    override suspend fun getSectorConstituents(sector: Sector): List<Constituent> {
        val count = when (sector.id) {
            "BK001" -> 3
            "BK002" -> 2
            else -> 4
        }
        return symbols.take(count).map { Constituent(it) }
    }
}
