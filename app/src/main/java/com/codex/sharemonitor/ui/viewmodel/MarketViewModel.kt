package com.codex.sharemonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.sharemonitor.data.quotes.QuotesRepository
import com.codex.sharemonitor.domain.model.Exchange
import com.codex.sharemonitor.domain.model.Index
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.Sector
import com.codex.sharemonitor.domain.model.Symbol
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** “行情”页 Tab。 */
enum class MarketTab { Indices, Sectors }
/** 板块列表排序方式。 */
enum class SectorSort { ChangePctDesc, NameAsc }

/** 指数列表的 UI 映射结果（指数基本信息 + 对应 symbol + 缓存报价）。 */
data class IndexUi(
    val index: Index,
    val symbol: Symbol,
    val quote: Quote?,
)

/** 板块列表的 UI 映射结果。 */
data class SectorUi(
    val sector: Sector,
)

/** 行情页 UI 状态（指数/板块）。 */
data class MarketUiState(
    val tab: MarketTab,
    val indices: List<IndexUi>,
    val sectors: List<SectorUi>,
    val sectorSort: SectorSort,
    val isRefreshing: Boolean,
)

/**
 * 行情页 ViewModel。
 *
 * 说明：
 * - 指数与板块数据固定走 Eastmoney（与用户选择的“报价数据源”解耦）；
 * - UI 主要订阅缓存并触发刷新，刷新频率受仓储的最小间隔/节流限制。
 */
class MarketViewModel(
    private val quotesRepository: QuotesRepository,
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

    private val tabFlow = MutableStateFlow(MarketTab.Indices)
    private val indicesFlow = MutableStateFlow<List<Index>>(emptyList())
    private val sortFlow = MutableStateFlow(SectorSort.ChangePctDesc)
    private val refreshingFlow = MutableStateFlow(false)

    private data class Base(
        val tab: MarketTab,
        val sort: SectorSort,
        val refreshing: Boolean,
    )

    private data class Data(
        val indices: List<Index>,
        val quoteMap: Map<String, Quote>,
        val sectors: List<Sector>,
    )

    private val baseFlow: Flow<Base> = combine(
        tabFlow,
        sortFlow,
        refreshingFlow,
    ) { tab, sort, refreshing ->
        Base(tab = tab, sort = sort, refreshing = refreshing)
    }

    private val dataFlow: Flow<Data> = combine(
        indicesFlow,
        quotesRepository.observeAllCachedQuotes(),
        quotesRepository.observeCachedSectors(),
    ) { indices, quoteMap, sectors ->
        Data(indices = indices, quoteMap = quoteMap, sectors = sectors)
    }

    val uiState: StateFlow<MarketUiState> = combine(
        baseFlow,
        dataFlow,
    ) { base, data ->
        val indexUi = data.indices.mapNotNull { idx ->
            val symbol = idx.toSymbol() ?: return@mapNotNull null
            IndexUi(index = idx, symbol = symbol, quote = data.quoteMap[symbol.key])
        }

        val sortedSectors = when (base.sort) {
            SectorSort.ChangePctDesc -> data.sectors.sortedWith(
                compareByDescending<Sector> { it.changePct ?: Double.NEGATIVE_INFINITY }.thenBy { it.name }
            )
            SectorSort.NameAsc -> data.sectors.sortedBy { it.name }
        }

        MarketUiState(
            tab = base.tab,
            indices = indexUi,
            sectors = sortedSectors.map { SectorUi(it) },
            sectorSort = base.sort,
            isRefreshing = base.refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MarketUiState(
            tab = MarketTab.Indices,
            indices = emptyList(),
            sectors = emptyList(),
            sectorSort = SectorSort.ChangePctDesc,
            isRefreshing = false,
        ),
    )

    init {
        viewModelScope.launch {
            indicesFlow.value = quotesRepository.getIndices()
            refreshAll(force = false)
        }
    }

    fun setTab(tab: MarketTab) {
        tabFlow.value = tab
    }

    fun setSectorSort(sort: SectorSort) {
        sortFlow.value = sort
    }

    fun refreshAll(force: Boolean) {
        viewModelScope.launch {
            refreshingFlow.value = true
            try {
                val indicesOutcome = quotesRepository.refreshMarketIndicesQuotes(force = force)
                notifyRefreshOutcome(indicesOutcome)

                val sectorsOutcome = quotesRepository.refreshMarketSectors(force = force)
                notifyRefreshOutcome(sectorsOutcome)
            } finally {
                refreshingFlow.value = false
            }
        }
    }

    private fun Index.toSymbol(): Symbol? {
        val market = secId.substringBefore('.', missingDelimiterValue = "").toIntOrNull() ?: return null
        val exchange = when (market) {
            1 -> Exchange.SH
            0 -> Exchange.SZ
            else -> return null
        }
        return Symbol(exchange = exchange, code = code, name = name)
    }
}
