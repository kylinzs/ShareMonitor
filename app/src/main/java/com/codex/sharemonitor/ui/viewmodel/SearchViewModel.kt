package com.codex.sharemonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.sharemonitor.data.quotes.QuotesRepository
import com.codex.sharemonitor.data.watchlist.WatchlistRepository
import com.codex.sharemonitor.domain.model.Symbol
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 搜索页 UI 状态。
 *
 * 说明：为了便于排障与提升透明度，状态中会包含“命中数据源”和“已尝试数据源列表”。
 */
data class SearchUiState(
    val query: String,
    val isLoading: Boolean,
    val results: List<Symbol>,
    val matchedSourceName: String?,
    val attemptedSources: List<String>,
    val errorMessage: String?,
)

/**
 * 搜索页 ViewModel。
 *
 * 职责：
 * - 对用户输入做 debounce，减少网络请求；
 * - 通过 [QuotesRepository.searchSymbolsDetailed] 获取结果，并展示数据源命中/回退信息；
 * - 支持“重试”和“加入自选”。
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val quotesRepository: QuotesRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {
    private val snackbarChannel = Channel<String>(capacity = Channel.BUFFERED)
    val snackbarMessages: Flow<String> = snackbarChannel.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")
    private val loadingFlow = MutableStateFlow(false)
    private val resultsFlow = MutableStateFlow<List<Symbol>>(emptyList())
    private val matchedSourceFlow = MutableStateFlow<String?>(null)
    private val attemptedSourcesFlow = MutableStateFlow<List<String>>(emptyList())
    private val errorFlow = MutableStateFlow<String?>(null)

    private data class Base(
        val query: String,
        val isLoading: Boolean,
    )

    private data class Data(
        val results: List<Symbol>,
        val matchedSourceName: String?,
        val attemptedSources: List<String>,
        val errorMessage: String?,
    )

    private val baseFlow: Flow<Base> = combine(
        queryFlow,
        loadingFlow,
    ) { query, loading ->
        Base(query = query, isLoading = loading)
    }

    private val dataFlow: Flow<Data> = combine(
        resultsFlow,
        matchedSourceFlow,
        attemptedSourcesFlow,
        errorFlow,
    ) { results, matchedSource, attemptedSources, error ->
        Data(
            results = results,
            matchedSourceName = matchedSource,
            attemptedSources = attemptedSources,
            errorMessage = error,
        )
    }

    val uiState: StateFlow<SearchUiState> = combine(
        baseFlow,
        dataFlow,
    ) { base, data ->
        SearchUiState(
            query = base.query,
            isLoading = base.isLoading,
            results = data.results,
            matchedSourceName = data.matchedSourceName,
            attemptedSources = data.attemptedSources,
            errorMessage = data.errorMessage,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SearchUiState("", false, emptyList(), null, emptyList(), null),
    )

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { q ->
                    performSearch(q)
                }
        }
    }

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun retry() {
        viewModelScope.launch { performSearch(queryFlow.value) }
    }

    private suspend fun performSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            resultsFlow.value = emptyList()
            matchedSourceFlow.value = null
            attemptedSourcesFlow.value = emptyList()
            errorFlow.value = null
            return
        }

        loadingFlow.value = true
        errorFlow.value = null
        try {
            val result = quotesRepository.searchSymbolsDetailed(q)
            resultsFlow.value = result.symbols
            matchedSourceFlow.value = result.matchedSourceName
            attemptedSourcesFlow.value = result.attemptedSources
            errorFlow.value = result.error?.let { "搜索失败：${it.userMessage}" }
        } catch (t: Throwable) {
            errorFlow.value = "搜索失败，请重试"
            resultsFlow.value = emptyList()
            matchedSourceFlow.value = null
            attemptedSourcesFlow.value = emptyList()
        } finally {
            loadingFlow.value = false
        }
    }

    fun addToWatchlist(symbol: Symbol) {
        viewModelScope.launch {
            val inserted = watchlistRepository.addSymbol(symbol)
            snackbarChannel.trySend(if (inserted) "已加入自选" else "已在自选中")
        }
    }
}
