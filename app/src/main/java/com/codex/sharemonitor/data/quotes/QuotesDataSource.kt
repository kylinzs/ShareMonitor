package com.codex.sharemonitor.data.quotes

import com.codex.sharemonitor.domain.model.Constituent
import com.codex.sharemonitor.domain.model.HistorySeries
import com.codex.sharemonitor.domain.model.HistoryType
import com.codex.sharemonitor.domain.model.Index
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.Sector
import com.codex.sharemonitor.domain.model.Symbol

/**
 * 行情数据源抽象。
 *
 * 说明：
 * - 具体实现负责对接不同公开接口（如 Eastmoney/Tencent/Mock），并把结果映射为统一的 domain model。
 * - 业务层尽量依赖该接口，避免把“某数据源的字段/格式细节”泄漏到 UI。
 */
interface QuotesDataSource {
    /** 数据源名称（用于 UI 提示与排障）。 */
    val sourceName: String

    /** 根据代码/名称进行搜索，返回候选标的。 */
    suspend fun searchSymbols(query: String): List<Symbol>
    /** 批量拉取报价，用于自选/指数等列表。 */
    suspend fun getQuotes(symbols: List<Symbol>): List<Quote>
    /** 拉取单个标的报价（可能为空）。 */
    suspend fun getQuote(symbol: Symbol): Quote?

    /** 拉取历史走势数据（分时/日K）。 */
    suspend fun getHistory(symbol: Symbol, type: HistoryType): HistorySeries?

    /** 指数列表（用于“行情-指数”）。 */
    suspend fun getIndices(): List<Index>
    /** 板块列表（用于“行情-板块”）。 */
    suspend fun getSectors(): List<Sector>
    /** 板块成分股列表（仅返回标的基本信息，报价可能需要额外拉取）。 */
    suspend fun getSectorConstituents(sector: Sector): List<Constituent>
}
