package com.codex.sharemonitor.data.quotes

import com.codex.sharemonitor.data.settings.SettingsRepository
import com.codex.sharemonitor.data.settings.QuoteDataSourceId
import com.codex.sharemonitor.data.watchlist.WatchlistRepository
import com.codex.sharemonitor.domain.model.Constituent
import com.codex.sharemonitor.domain.model.DataError
import com.codex.sharemonitor.domain.model.HistorySeries
import com.codex.sharemonitor.domain.model.HistoryType
import com.codex.sharemonitor.domain.model.Index
import com.codex.sharemonitor.domain.model.Quote
import com.codex.sharemonitor.domain.model.Sector
import com.codex.sharemonitor.domain.model.Symbol
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.ceil

/**
 * 行情仓储（Repository）。
 *
 * 职责：
 * - 根据设置选择当前使用的数据源（Eastmoney/Tencent/Mock）。
 * - 维护内存缓存（报价/历史/板块等），并通过 Flow 供 UI 订阅。
 * - 封装刷新策略：最小刷新间隔、短时间节流、同 scope 的并发合并（避免重复请求）。
 * - 搜索时做多数据源回退，并把“命中数据源/已尝试数据源”暴露给 UI 进行提示。
 *
 * 设计取舍：
 * - 缓存是进程内缓存（非持久化），用于提升 UI 响应；是否持久化由上层需求决定。
 * - 刷新策略以“用户可理解”为主：间隔未到时提示剩余时间，频繁点击时提示节流。
 */
class QuotesRepository(
    private val settingsRepository: SettingsRepository,
    private val watchlistRepository: WatchlistRepository,
    private val mockQuotesDataSource: QuotesDataSource,
    private val eastmoneyQuotesDataSource: QuotesDataSource,
    private val tencentQuotesDataSource: QuotesDataSource,
) {
    data class SearchSymbolsResult(
        val symbols: List<Symbol>,
        val matchedSourceName: String?,
        val attemptedSources: List<String>,
        val error: DataError?,
    )

    private val dataSourceFlow: Flow<QuotesDataSource> = settingsRepository.settingsFlow.map { settings ->
        when (settings.quoteDataSourceId) {
            QuoteDataSourceId.Eastmoney -> eastmoneyQuotesDataSource
            QuoteDataSourceId.Mock -> mockQuotesDataSource
            QuoteDataSourceId.Tencent -> tencentQuotesDataSource
        }
    }

    data class RefreshOutcome(
        val kind: Kind,
        val message: String? = null,
        val error: DataError? = null,
    ) {
        enum class Kind {
            Success,
            SkippedInterval,
            Throttled,
            Failed,
        }
    }

    private data class HistoryKey(
        val symbolKey: String,
        val type: HistoryType,
    )

    private data class ScopeState(
        var lastAttemptEpochMillis: Long = 0L,
        var lastSuccessEpochMillis: Long = 0L,
        var inFlight: CompletableDeferred<RefreshOutcome>? = null,
    )

    private val quoteCache: MutableStateFlow<Map<String, Quote>> = MutableStateFlow(emptyMap())
    private val historyCache: MutableStateFlow<Map<HistoryKey, HistorySeries>> = MutableStateFlow(emptyMap())
    private val sectorCache: MutableStateFlow<List<Sector>> = MutableStateFlow(emptyList())
    private val sectorConstituentsCache: MutableStateFlow<Map<String, List<Constituent>>> = MutableStateFlow(emptyMap())

    private val refreshMutex = Mutex()
    private val scopeStates: MutableMap<String, ScopeState> = LinkedHashMap()

    fun observeAllCachedQuotes(): StateFlow<Map<String, Quote>> = quoteCache.asStateFlow()

    fun observeCachedQuote(symbolKey: String): Flow<Quote?> = quoteCache.map { it[symbolKey] }

    fun observeCachedHistory(symbolKey: String, type: HistoryType): Flow<HistorySeries?> {
        return historyCache.map { it[HistoryKey(symbolKey = symbolKey, type = type)] }
    }

    fun observeCachedSectors(): StateFlow<List<Sector>> = sectorCache.asStateFlow()

    fun observeCachedSectorConstituents(sectorId: String): Flow<List<Constituent>> {
        return sectorConstituentsCache.map { it[sectorId].orEmpty() }
    }

    fun observeWatchlistQuotesCached(): Flow<List<Pair<Symbol, Quote?>>> {
        return combine(
            watchlistRepository.observeSymbols(),
            quoteCache,
        ) { symbols, cached ->
            symbols.map { it to cached[it.key] }
        }
    }

    suspend fun refreshWatchlistQuotes(force: Boolean = false): RefreshOutcome {
        val scope = "quotes:watchlist"
        val symbols = watchlistRepository.observeSymbols().first()
        return refreshQuotes(scope = scope, symbols = symbols, force = force, dataSource = dataSourceFlow.first())
    }

    suspend fun refreshQuote(symbol: Symbol, force: Boolean = false): RefreshOutcome {
        val scope = "quote:${symbol.key}"
        return refreshQuotes(scope = scope, symbols = listOf(symbol), force = force, dataSource = dataSourceFlow.first())
    }

    suspend fun refreshMarketIndicesQuotes(force: Boolean = false): RefreshOutcome {
        val indices = getIndices()
        val symbols = indices.mapNotNull { indexToSymbol(it) }
        val scope = "market:indices:quotes"
        return refreshQuotes(scope = scope, symbols = symbols, force = force, dataSource = eastmoneyQuotesDataSource)
    }

    private suspend fun refreshQuotes(
        scope: String,
        symbols: List<Symbol>,
        force: Boolean,
        dataSource: QuotesDataSource,
    ): RefreshOutcome {
        if (symbols.isEmpty()) return RefreshOutcome(kind = RefreshOutcome.Kind.Success)

        val intervalMs = refreshIntervalMs()
        val now = System.currentTimeMillis()
        val throttleMs = 1_500L

        val deferredOrEarlyReturn = refreshMutex.withLock {
            val state = scopeStates.getOrPut(scope) { ScopeState() }
            val inFlight = state.inFlight
            // 同 scope 合并并发刷新：如果已经有请求在飞行中，复用其结果，避免重复打接口。
            if (inFlight != null) return@withLock inFlight to null

            // 最小刷新间隔：成功刷新后，在 interval 内再次刷新直接提示“距离下次更新还有 X 分钟”。
            if (!force && state.lastSuccessEpochMillis > 0 && (now - state.lastSuccessEpochMillis) < intervalMs) {
                val remainingMs = intervalMs - (now - state.lastSuccessEpochMillis)
                val minutes = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
                return@withLock null to RefreshOutcome(
                    kind = RefreshOutcome.Kind.SkippedInterval,
                    message = "距离下次更新还有 $minutes 分钟",
                )
            }

            // 短时间节流：避免用户连续点击触发大量请求。
            if (!force && state.lastAttemptEpochMillis > 0 && (now - state.lastAttemptEpochMillis) < throttleMs) {
                return@withLock null to RefreshOutcome(kind = RefreshOutcome.Kind.Throttled, message = "操作太频繁，请稍后重试")
            }

            state.lastAttemptEpochMillis = now
            val deferred = CompletableDeferred<RefreshOutcome>()
            state.inFlight = deferred
            deferred to null
        }

        val (deferred, earlyReturn) = deferredOrEarlyReturn
        if (earlyReturn != null) return earlyReturn
        if (deferred == null) return RefreshOutcome(kind = RefreshOutcome.Kind.Failed, message = "刷新状态错误")

        var outcome: RefreshOutcome? = null
        outcome = try {
            val quotes = retryWithBackoff { dataSource.getQuotes(symbols) }
            if (quotes.isEmpty()) {
                RefreshOutcome(
                    kind = RefreshOutcome.Kind.Failed,
                    error = DataError.Parse(userMessage = "未获取到行情数据"),
                    message = "未获取到行情数据",
                )
            } else {
            val map = quotes.associateBy { it.symbolKey }
            quoteCache.value = quoteCache.value.toMutableMap().apply { putAll(map) }
            RefreshOutcome(kind = RefreshOutcome.Kind.Success)
            }
        } catch (t: Throwable) {
            val error = t.toDataError()
            RefreshOutcome(kind = RefreshOutcome.Kind.Failed, error = error, message = error.userMessage)
        } finally {
            refreshMutex.withLock {
                val state = scopeStates.getOrPut(scope) { ScopeState() }
                val resolved = outcome
                if (resolved?.kind == RefreshOutcome.Kind.Success) state.lastSuccessEpochMillis = now
                state.inFlight = null
            }
        }

        deferred.complete(outcome)
        return outcome
    }

    suspend fun refreshHistory(symbol: Symbol, type: HistoryType, force: Boolean = false): RefreshOutcome {
        val scope = "history:${symbol.key}:$type"
        val intervalMs = refreshIntervalMs()
        val now = System.currentTimeMillis()
        val throttleMs = 1_500L

        val deferredOrEarlyReturn = refreshMutex.withLock {
            val state = scopeStates.getOrPut(scope) { ScopeState() }
            val inFlight = state.inFlight
            if (inFlight != null) return@withLock inFlight to null

            if (!force && state.lastSuccessEpochMillis > 0 && (now - state.lastSuccessEpochMillis) < intervalMs) {
                val remainingMs = intervalMs - (now - state.lastSuccessEpochMillis)
                val minutes = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
                return@withLock null to RefreshOutcome(
                    kind = RefreshOutcome.Kind.SkippedInterval,
                    message = "距离下次更新还有 $minutes 分钟",
                )
            }

            if (!force && state.lastAttemptEpochMillis > 0 && (now - state.lastAttemptEpochMillis) < throttleMs) {
                return@withLock null to RefreshOutcome(kind = RefreshOutcome.Kind.Throttled, message = "操作太频繁，请稍后重试")
            }

            state.lastAttemptEpochMillis = now
            val deferred = CompletableDeferred<RefreshOutcome>()
            state.inFlight = deferred
            deferred to null
        }

        val (deferred, earlyReturn) = deferredOrEarlyReturn
        if (earlyReturn != null) return earlyReturn
        if (deferred == null) return RefreshOutcome(kind = RefreshOutcome.Kind.Failed, message = "刷新状态错误")

        var outcome: RefreshOutcome? = null
        outcome = try {
            val dataSource = dataSourceFlow.first()
            val series = retryWithBackoff { dataSource.getHistory(symbol, type) }
            if (series != null) {
                historyCache.value = historyCache.value.toMutableMap().apply {
                    put(HistoryKey(symbolKey = symbol.key, type = type), series)
                }
            }
            RefreshOutcome(kind = RefreshOutcome.Kind.Success)
        } catch (t: Throwable) {
            val error = t.toDataError()
            RefreshOutcome(kind = RefreshOutcome.Kind.Failed, error = error, message = error.userMessage)
        } finally {
            refreshMutex.withLock {
                val state = scopeStates.getOrPut(scope) { ScopeState() }
                val resolved = outcome
                if (resolved?.kind == RefreshOutcome.Kind.Success) state.lastSuccessEpochMillis = now
                state.inFlight = null
            }
        }

        deferred.complete(outcome)
        return outcome
    }

    suspend fun refreshMarketSectors(force: Boolean = false): RefreshOutcome {
        val scope = "market:sectors"
        val intervalMs = refreshIntervalMs()
        val now = System.currentTimeMillis()
        val throttleMs = 1_500L

        val deferredOrEarlyReturn = refreshMutex.withLock {
            val state = scopeStates.getOrPut(scope) { ScopeState() }
            val inFlight = state.inFlight
            if (inFlight != null) return@withLock inFlight to null

            if (!force && state.lastSuccessEpochMillis > 0 && (now - state.lastSuccessEpochMillis) < intervalMs) {
                val remainingMs = intervalMs - (now - state.lastSuccessEpochMillis)
                val minutes = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
                return@withLock null to RefreshOutcome(
                    kind = RefreshOutcome.Kind.SkippedInterval,
                    message = "距离下次更新还有 $minutes 分钟",
                )
            }

            if (!force && state.lastAttemptEpochMillis > 0 && (now - state.lastAttemptEpochMillis) < throttleMs) {
                return@withLock null to RefreshOutcome(kind = RefreshOutcome.Kind.Throttled, message = "操作太频繁，请稍后重试")
            }

            state.lastAttemptEpochMillis = now
            val deferred = CompletableDeferred<RefreshOutcome>()
            state.inFlight = deferred
            deferred to null
        }

        val (deferred, earlyReturn) = deferredOrEarlyReturn
        if (earlyReturn != null) return earlyReturn
        if (deferred == null) return RefreshOutcome(kind = RefreshOutcome.Kind.Failed, message = "刷新状态错误")

        var outcome: RefreshOutcome? = null
        outcome = try {
            val sectors = retryWithBackoff { eastmoneyQuotesDataSource.getSectors() }
            sectorCache.value = sectors
            RefreshOutcome(kind = RefreshOutcome.Kind.Success)
        } catch (t: Throwable) {
            val error = t.toDataError()
            RefreshOutcome(kind = RefreshOutcome.Kind.Failed, error = error, message = error.userMessage)
        } finally {
            refreshMutex.withLock {
                val state = scopeStates.getOrPut(scope) { ScopeState() }
                val resolved = outcome
                if (resolved?.kind == RefreshOutcome.Kind.Success) state.lastSuccessEpochMillis = now
                state.inFlight = null
            }
        }

        deferred.complete(outcome)
        return outcome
    }

    suspend fun refreshMarketSectorConstituents(sector: Sector, force: Boolean = false): RefreshOutcome {
        val scope = "market:sector:${sector.id}"
        val intervalMs = refreshIntervalMs()
        val now = System.currentTimeMillis()
        val throttleMs = 1_500L

        val deferredOrEarlyReturn = refreshMutex.withLock {
            val state = scopeStates.getOrPut(scope) { ScopeState() }
            val inFlight = state.inFlight
            if (inFlight != null) return@withLock inFlight to null

            if (!force && state.lastSuccessEpochMillis > 0 && (now - state.lastSuccessEpochMillis) < intervalMs) {
                val remainingMs = intervalMs - (now - state.lastSuccessEpochMillis)
                val minutes = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
                return@withLock null to RefreshOutcome(
                    kind = RefreshOutcome.Kind.SkippedInterval,
                    message = "距离下次更新还有 $minutes 分钟",
                )
            }

            if (!force && state.lastAttemptEpochMillis > 0 && (now - state.lastAttemptEpochMillis) < throttleMs) {
                return@withLock null to RefreshOutcome(kind = RefreshOutcome.Kind.Throttled, message = "操作太频繁，请稍后重试")
            }

            state.lastAttemptEpochMillis = now
            val deferred = CompletableDeferred<RefreshOutcome>()
            state.inFlight = deferred
            deferred to null
        }

        val (deferred, earlyReturn) = deferredOrEarlyReturn
        if (earlyReturn != null) return earlyReturn
        if (deferred == null) return RefreshOutcome(kind = RefreshOutcome.Kind.Failed, message = "刷新状态错误")

        var outcome: RefreshOutcome? = null
        outcome = try {
            val list = retryWithBackoff { eastmoneyQuotesDataSource.getSectorConstituents(sector) }
            sectorConstituentsCache.value = sectorConstituentsCache.value.toMutableMap().apply { put(sector.id, list) }
            RefreshOutcome(kind = RefreshOutcome.Kind.Success)
        } catch (t: Throwable) {
            val error = t.toDataError()
            RefreshOutcome(kind = RefreshOutcome.Kind.Failed, error = error, message = error.userMessage)
        } finally {
            refreshMutex.withLock {
                val state = scopeStates.getOrPut(scope) { ScopeState() }
                val resolved = outcome
                if (resolved?.kind == RefreshOutcome.Kind.Success) state.lastSuccessEpochMillis = now
                state.inFlight = null
            }
        }

        deferred.complete(outcome)
        return outcome
    }

    private suspend fun refreshIntervalMs(): Long {
        val minutes = settingsRepository.settingsFlow.first().refreshIntervalMinutes.coerceAtLeast(1)
        return minutes * 60_000L
    }

    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 400L,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs
        var last: Throwable? = null
        while (attempt < maxAttempts) {
            attempt += 1
            try {
                return block()
            } catch (t: Throwable) {
                last = t
                if (attempt >= maxAttempts) break
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(2_000L)
            }
        }
        throw last ?: IllegalStateException("retryWithBackoff failed")
    }

    private fun Throwable.toDataError(): DataError {
        return when (this) {
            is HttpException -> {
                if (code() == 429) DataError.RateLimited() else DataError.Network()
            }
            is IOException -> DataError.Network()
            is JsonDataException, is JsonEncodingException, is NumberFormatException -> DataError.Parse()
            else -> DataError.Unknown()
        }
    }

    suspend fun searchSymbolsDetailed(query: String): SearchSymbolsResult {
        val q = query.trim()
        if (q.isEmpty()) {
            return SearchSymbolsResult(
                symbols = emptyList(),
                matchedSourceName = null,
                attemptedSources = emptyList(),
                error = null,
            )
        }

        val attempted = mutableListOf<String>()
        var anySuccess = false
        var firstError: DataError? = null

        suspend fun attempt(ds: QuotesDataSource): List<Symbol>? {
            attempted += ds.sourceName
            return try {
                val res = ds.searchSymbols(q)
                anySuccess = true
                res
            } catch (t: Throwable) {
                if (firstError == null) firstError = t.toDataError()
                null
            }
        }

        val eastmoney = attempt(eastmoneyQuotesDataSource)
        if (!eastmoney.isNullOrEmpty()) {
            return SearchSymbolsResult(
                symbols = eastmoney,
                matchedSourceName = eastmoneyQuotesDataSource.sourceName,
                attemptedSources = attempted,
                error = null,
            )
        }

        val tencent = attempt(tencentQuotesDataSource)
        if (!tencent.isNullOrEmpty()) {
            return SearchSymbolsResult(
                symbols = tencent,
                matchedSourceName = tencentQuotesDataSource.sourceName,
                attemptedSources = attempted,
                error = null,
            )
        }

        val mock = attempt(mockQuotesDataSource)
        if (!mock.isNullOrEmpty()) {
            return SearchSymbolsResult(
                symbols = mock,
                matchedSourceName = mockQuotesDataSource.sourceName,
                attemptedSources = attempted,
                error = null,
            )
        }

        return SearchSymbolsResult(
            symbols = emptyList(),
            matchedSourceName = null,
            attemptedSources = attempted,
            error = if (anySuccess) null else firstError,
        )
    }

    suspend fun searchSymbols(query: String): List<Symbol> {
        return searchSymbolsDetailed(query).symbols
    }

    suspend fun getQuote(symbol: Symbol): Quote? {
        return quoteCache.value[symbol.key] ?: dataSourceFlow.first().getQuote(symbol)
    }

    suspend fun getHistory(symbol: Symbol, type: HistoryType): HistorySeries? {
        return historyCache.value[HistoryKey(symbolKey = symbol.key, type = type)] ?: dataSourceFlow.first().getHistory(symbol, type)
    }

    suspend fun getIndices(): List<Index> {
        return eastmoneyQuotesDataSource.getIndices()
    }

    suspend fun getSectors(): List<Sector> {
        if (sectorCache.value.isNotEmpty()) return sectorCache.value
        return eastmoneyQuotesDataSource.getSectors().also { sectorCache.value = it }
    }

    suspend fun getSectorConstituents(sector: Sector): List<Constituent> {
        val cached = sectorConstituentsCache.value[sector.id]
        if (cached != null) return cached
        return eastmoneyQuotesDataSource.getSectorConstituents(sector).also { list ->
            sectorConstituentsCache.value = sectorConstituentsCache.value.toMutableMap().apply { put(sector.id, list) }
        }
    }

    private fun indexToSymbol(index: Index): Symbol? {
        val market = index.secId.substringBefore('.', missingDelimiterValue = "").toIntOrNull() ?: return null
        val exchange = when (market) {
            1 -> com.codex.sharemonitor.domain.model.Exchange.SH
            0 -> com.codex.sharemonitor.domain.model.Exchange.SZ
            else -> return null
        }
        return Symbol(exchange = exchange, code = index.code, name = index.name)
    }
}
