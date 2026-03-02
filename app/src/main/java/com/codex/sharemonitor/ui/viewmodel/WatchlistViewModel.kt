package com.codex.sharemonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.sharemonitor.data.quotes.RefreshPolicies
import com.codex.sharemonitor.data.quotes.QuotesRepository
import com.codex.sharemonitor.data.settings.SettingsRepository
import com.codex.sharemonitor.data.watchlist.WatchlistRepository
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.WatchlistEntry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 自选列表项的 UI 映射结果。
 *
 * @property quote 可能为空：首次进入页面或网络失败时仅展示标的基本信息。
 * @property isStale 是否“可能已过期”（基于设置的刷新间隔与 [Quote.fetchedAtEpochMillis] 计算）。
 */
data class WatchlistItemUi(
    val entry: WatchlistEntry,
    val quote: Quote?,
    val isStale: Boolean,
)

/** 自选页 UI 状态。 */
data class WatchlistUiState(
    val items: List<WatchlistItemUi>,
    val isRefreshing: Boolean,
)

/**
 * 自选页 ViewModel。
 *
 * 职责：
 * - 订阅自选列表与缓存报价，并计算“是否过期”的提示；
 * - 支持下拉/按钮刷新（走仓储的最小间隔与节流策略）；
 * - 支持置顶、移动与移除。
 */
class WatchlistViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val settingsRepository: SettingsRepository,
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

    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlistRepository.observeEntries(),
        quotesRepository.observeAllCachedQuotes(),
        settingsRepository.settingsFlow,
        refreshing,
    ) { entries, quoteMap, settings, isRefreshing ->
        val intervalMs = settings.refreshIntervalMinutes.coerceAtLeast(1) * 60_000L
        val now = System.currentTimeMillis()
        val items = entries.map { entry ->
            val quote = quoteMap[entry.symbol.key]
            val isStale = quote?.let { RefreshPolicies.isStale(fetchedAtEpochMillis = it.fetchedAtEpochMillis, nowEpochMillis = now, staleAfterMs = intervalMs) } ?: false
            WatchlistItemUi(entry = entry, quote = quote, isStale = isStale)
        }
        WatchlistUiState(items = items, isRefreshing = isRefreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WatchlistUiState(items = emptyList(), isRefreshing = false),
    )

    init {
        viewModelScope.launch {
            refresh(force = false)
        }
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch {
            refreshInternal(force = force)
        }
    }

    private suspend fun refreshInternal(force: Boolean) {
        refreshing.value = true
        try {
            val outcome = quotesRepository.refreshWatchlistQuotes(force = force)
            notifyRefreshOutcome(outcome)
        } finally {
            refreshing.value = false
        }
    }

    fun togglePinned(symbolKey: String, pinned: Boolean) {
        viewModelScope.launch {
            watchlistRepository.setPinned(symbolKey, pinned)
        }
    }

    fun remove(symbolKey: String) {
        viewModelScope.launch {
            watchlistRepository.removeSymbol(symbolKey)
            snackbarChannel.trySend("已移除")
        }
    }

    fun moveUp(symbolKey: String) {
        viewModelScope.launch { watchlistRepository.moveUp(symbolKey) }
    }

    fun moveDown(symbolKey: String) {
        viewModelScope.launch { watchlistRepository.moveDown(symbolKey) }
    }
}
