package com.codex.sharemonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.sharemonitor.data.quotes.QuotesRepository
import com.codex.sharemonitor.data.watchlist.WatchlistRepository
import com.codex.sharemonitor.domain.model.Exchange
import com.codex.sharemonitor.domain.model.HistorySeries
import com.codex.sharemonitor.domain.model.HistoryType
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.Symbol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 个股详情页 UI 状态。
 *
 * 说明：
 * - [quote] 为 UI 展示用报价，可能来自实时报价，也可能来自“最近收盘”的回退报价；
 * - [marketStatusMessage] 用于提示当前展示的是回退数据（例如未开盘时显示最近收盘）；
 * - [history] 为当前 tab 选中的历史序列；
 * - [fallbackDailyHistory] 在分时不可用/未开盘场景下用于兜底展示日K（避免页面空白）。
 */
data class DetailUiState(
    val symbol: Symbol?,
    val isInWatchlist: Boolean,
    val quote: Quote?,
    val marketStatusMessage: String?,
    val historyType: HistoryType,
    val history: HistorySeries?,
    val fallbackDailyHistory: HistorySeries?,
    val isRefreshing: Boolean,
)

/**
 * 个股详情页 ViewModel。
 *
 * 职责：
 * - 根据 symbolKey 加载报价与历史走势，并支持分时/日K切换；
 * - 提供刷新、加入/移除自选等操作；
 * - 在“当天未开盘/分时不可用”时回退显示最近收盘（来自日K最后一个交易日）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(
    private val quotesRepository: QuotesRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {
    private val snackbarChannel = Channel<String>(capacity = Channel.BUFFERED)
    val snackbarMessages: Flow<String> = snackbarChannel.receiveAsFlow()

    private fun notifyRefreshOutcome(outcome: QuotesRepository.RefreshOutcome) {
        val message = when (outcome.kind) {
            QuotesRepository.RefreshOutcome.Kind.Failed ->
                outcome.error?.userMessage ?: outcome.message ?: "刷新失败"

            else -> outcome.message
        }
        message?.let { snackbarChannel.trySend(it) }
    }

    private val symbolFlow = MutableStateFlow<Symbol?>(null)
    private val historyTypeFlow = MutableStateFlow(HistoryType.Intraday)
    private val refreshingFlow = MutableStateFlow(false)

    fun setSymbolKey(symbolKey: String) {
        val parsed = parseSymbolKey(symbolKey) ?: return
        symbolFlow.value = parsed
        viewModelScope.launch { resolveNameFromWatchlist(parsed) }
        refresh(force = false)
    }

    private val selectedHistoryFlow: Flow<HistorySeries?> = combine(
        symbolFlow,
        historyTypeFlow,
    ) { symbol, type ->
        symbol to type
    }.flatMapLatest { (symbol, type) ->
        if (symbol == null) flowOf(null) else quotesRepository.observeCachedHistory(symbolKey = symbol.key, type = type)
    }

    private val intradayHistoryFlow: Flow<HistorySeries?> = symbolFlow.flatMapLatest { symbol ->
        if (symbol == null) flowOf(null) else quotesRepository.observeCachedHistory(symbolKey = symbol.key, type = HistoryType.Intraday)
    }

    private val dailyHistoryFlow: Flow<HistorySeries?> = symbolFlow.flatMapLatest { symbol ->
        if (symbol == null) flowOf(null) else quotesRepository.observeCachedHistory(symbolKey = symbol.key, type = HistoryType.DailyK)
    }

    private data class Base(
        val symbol: Symbol?,
        val historyType: HistoryType,
        val refreshing: Boolean,
    )

    private data class Data(
        val isInWatchlist: Boolean,
        val quote: Quote?,
        val selectedHistory: HistorySeries?,
        val intradayHistory: HistorySeries?,
        val dailyHistory: HistorySeries?,
    )

    private data class Histories(
        val selectedHistory: HistorySeries?,
        val intradayHistory: HistorySeries?,
        val dailyHistory: HistorySeries?,
    )

    private val baseFlow: Flow<Base> = combine(
        symbolFlow,
        historyTypeFlow,
        refreshingFlow,
    ) { symbol, historyType, refreshing ->
        Base(symbol = symbol, historyType = historyType, refreshing = refreshing)
    }

    private val historiesFlow: Flow<Histories> = combine(
        selectedHistoryFlow,
        intradayHistoryFlow,
        dailyHistoryFlow,
    ) { selectedHistory, intradayHistory, dailyHistory ->
        Histories(
            selectedHistory = selectedHistory,
            intradayHistory = intradayHistory,
            dailyHistory = dailyHistory,
        )
    }

    private val dataFlow: Flow<Data> = combine(
        symbolFlow,
        watchlistRepository.observeEntries(),
        quotesRepository.observeAllCachedQuotes(),
        historiesFlow,
    ) { symbol, entries, quoteMap, histories ->
        val isIn = symbol?.let { s -> entries.any { it.symbol.key == s.key } } ?: false
        val quote = symbol?.let { s -> quoteMap[s.key] }
        Data(
            isInWatchlist = isIn,
            quote = quote,
            selectedHistory = histories.selectedHistory,
            intradayHistory = histories.intradayHistory,
            dailyHistory = histories.dailyHistory,
        )
    }

    val uiState: StateFlow<DetailUiState> = combine(
        baseFlow,
        dataFlow,
    ) { base, data ->
        val symbol = base.symbol
        // 报价是 UI 的主展示数据；若缺少 lastPrice，视为“不可用报价”，让回退逻辑接管（避免页面空白）。
        val quote = data.quote?.takeIf { it.lastPrice != null }

        val intradayLatestEpoch = data.intradayHistory?.points?.lastOrNull()?.epochMillis
        val quoteEpoch = quote?.quoteTimeEpochMillis
        val latestEpoch = quoteEpoch ?: intradayLatestEpoch

        // 以“最新报价/分时最后点的日期是否为今天”来粗略判断是否为“未开盘/无当日数据”场景。
        // 该判断不依赖交易日历，仅用于 UI 回退展示“最近收盘”以避免空数据体验。
        val marketClosedToday = base.historyType == HistoryType.Intraday &&
            (latestEpoch == null || !isEpochMillisToday(latestEpoch))

        val fallbackDailyQuote = symbol?.let { s -> data.dailyHistory?.toLastCloseQuote(symbol = s) }
        val derivedIntradayQuote = symbol?.let { s -> data.intradayHistory?.toIntradayDerivedQuote(symbol = s) }
        val displayQuote = when {
            marketClosedToday && fallbackDailyQuote != null -> fallbackDailyQuote
            quote != null -> quote
            derivedIntradayQuote != null -> derivedIntradayQuote
            fallbackDailyQuote != null -> fallbackDailyQuote
            else -> null
        }

        val statusMessage = when {
            marketClosedToday && fallbackDailyQuote != null -> "今日未开盘，显示最近收盘"
            quote == null && derivedIntradayQuote != null -> "报价不可用，已从分时推导"
            quote == null && fallbackDailyQuote != null -> "显示最近收盘"
            else -> null
        }

        val fallbackDailyHistory = if (base.historyType == HistoryType.Intraday && marketClosedToday) {
            data.dailyHistory?.takeIf { it.points.isNotEmpty() }
        } else null

        DetailUiState(
            symbol = symbol,
            isInWatchlist = data.isInWatchlist,
            quote = displayQuote,
            marketStatusMessage = statusMessage,
            historyType = base.historyType,
            history = data.selectedHistory,
            fallbackDailyHistory = fallbackDailyHistory,
            isRefreshing = base.refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(
            symbol = null,
            isInWatchlist = false,
            quote = null,
            marketStatusMessage = null,
            historyType = HistoryType.Intraday,
            history = null,
            fallbackDailyHistory = null,
            isRefreshing = false,
        ),
    )

    fun setHistoryType(type: HistoryType) {
        historyTypeFlow.value = type
        refresh(force = false)
    }

    fun refresh(force: Boolean) {
        val symbol = symbolFlow.value ?: return
        viewModelScope.launch {
            refreshingFlow.value = true
            try {
                val quoteOutcome = quotesRepository.refreshQuote(symbol = symbol, force = force)
                notifyRefreshOutcome(quoteOutcome)

                val requestedType = historyTypeFlow.value
                val historyOutcome = quotesRepository.refreshHistory(symbol = symbol, type = requestedType, force = force)
                notifyRefreshOutcome(historyOutcome)

                if (requestedType == HistoryType.Intraday) {
                    val intraday = quotesRepository.getHistory(symbol, HistoryType.Intraday)
                    val last = intraday?.points?.lastOrNull()?.epochMillis
                    val latest = quotesRepository.getQuote(symbol)?.quoteTimeEpochMillis ?: last
                    val closedToday = latest == null || !isEpochMillisToday(latest)
                    if (closedToday) {
                        quotesRepository.refreshHistory(symbol = symbol, type = HistoryType.DailyK, force = false)
                    }
                }
            } finally {
                refreshingFlow.value = false
            }
        }
    }

    fun toggleWatchlist() {
        val symbol = symbolFlow.value ?: return
        viewModelScope.launch {
            val entries = watchlistRepository.observeEntries().first()
            val inWatchlist = entries.any { it.symbol.key == symbol.key }
            if (inWatchlist) {
                watchlistRepository.removeSymbol(symbol.key)
                snackbarChannel.trySend("已移除自选")
            } else {
                val inserted = watchlistRepository.addSymbol(symbol)
                snackbarChannel.trySend(if (inserted) "已加入自选" else "已在自选中")
            }
        }
    }

    private suspend fun resolveNameFromWatchlist(symbol: Symbol) {
        val entries = watchlistRepository.observeEntries().first()
        val match = entries.firstOrNull { it.symbol.key == symbol.key }?.symbol ?: return
        symbolFlow.value = match
    }

    private fun parseSymbolKey(symbolKey: String): Symbol? {
        val parts = symbolKey.split(":", limit = 2)
        if (parts.size != 2) return null
        val exchange = runCatching { Exchange.valueOf(parts[0]) }.getOrNull() ?: return null
        val code = parts[1]
        return Symbol(exchange = exchange, code = code, name = code)
    }

    private fun isEpochMillisToday(epochMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date == LocalDate.now()
    }

    private fun HistorySeries.toLastCloseQuote(symbol: Symbol): Quote? {
        if (type != HistoryType.DailyK) return null
        val points = points
        if (points.isEmpty()) return null

        val last = points.last()
        val prevClose = if (points.size >= 2) points[points.lastIndex - 1].close else null
        val change = prevClose?.let { last.close - it }
        val changePct = if (prevClose != null && prevClose != 0.0 && change != null) (change / prevClose) * 100.0 else null
        val now = System.currentTimeMillis()

        return Quote(
            symbolKey = symbol.key,
            lastPrice = last.close,
            change = change,
            changePct = changePct,
            open = last.open,
            high = last.high,
            low = last.low,
            prevClose = prevClose,
            volume = last.volume,
            quoteTimeEpochMillis = last.epochMillis,
            fetchedAtEpochMillis = now,
            sourceName = sourceName,
        )
    }

    private fun HistorySeries.toIntradayDerivedQuote(symbol: Symbol): Quote? {
        if (type != HistoryType.Intraday) return null
        val points = points
        if (points.isEmpty()) return null

        val first = points.first()
        val last = points.last()
        val closes = points.map { it.close }

        val open = first.close
        val lastPrice = last.close
        val high = closes.maxOrNull()
        val low = closes.minOrNull()
        val now = System.currentTimeMillis()

        return Quote(
            symbolKey = symbol.key,
            lastPrice = lastPrice,
            change = null,
            changePct = null,
            open = open,
            high = high,
            low = low,
            prevClose = null,
            volume = null,
            quoteTimeEpochMillis = last.epochMillis,
            fetchedAtEpochMillis = now,
            sourceName = "${sourceName}（分时推导）",
        )
    }
}
