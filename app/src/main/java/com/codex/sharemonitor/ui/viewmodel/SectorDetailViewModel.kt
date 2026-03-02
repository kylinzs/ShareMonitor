package com.codex.sharemonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.sharemonitor.data.quotes.QuotesRepository
import com.codex.sharemonitor.data.watchlist.WatchlistRepository
import com.codex.sharemonitor.domain.model.Constituent
import com.codex.sharemonitor.domain.model.Sector
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 板块详情页 UI 状态。 */
data class SectorDetailUiState(
    val sector: Sector?,
    val constituents: List<Constituent>,
    val isRefreshing: Boolean,
)

/**
 * 板块详情页 ViewModel。
 *
 * 职责：
 * - 根据 `sectorId` 加载板块基本信息与成分股列表；
 * - 支持刷新成分股、以及从成分股加入自选。
 */
class SectorDetailViewModel(
    private val sectorId: String,
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

    private val sectorFlow = MutableStateFlow<Sector?>(null)
    private val refreshingFlow = MutableStateFlow(false)

    val uiState: StateFlow<SectorDetailUiState> = combine(
        sectorFlow,
        quotesRepository.observeCachedSectorConstituents(sectorId),
        refreshingFlow,
    ) { sector, constituents, refreshing ->
        SectorDetailUiState(sector = sector, constituents = constituents, isRefreshing = refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SectorDetailUiState(sector = null, constituents = emptyList(), isRefreshing = false),
    )

    init {
        viewModelScope.launch {
            val sectors = quotesRepository.getSectors()
            sectorFlow.value = sectors.firstOrNull { it.id == sectorId }
            refresh(force = false)
        }
    }

    fun refresh(force: Boolean) {
        val sector = sectorFlow.value ?: return
        viewModelScope.launch {
            refreshingFlow.value = true
            try {
                val outcome = quotesRepository.refreshMarketSectorConstituents(sector = sector, force = force)
                notifyRefreshOutcome(outcome)
            } finally {
                refreshingFlow.value = false
            }
        }
    }

    fun addToWatchlist(constituent: Constituent) {
        viewModelScope.launch {
            val inserted = watchlistRepository.addSymbol(constituent.symbol)
            snackbarChannel.trySend(if (inserted) "已加入自选" else "已在自选中")
        }
    }
}
