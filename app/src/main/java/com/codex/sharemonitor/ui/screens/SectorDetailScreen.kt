package com.codex.sharemonitor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.sharemonitor.data.AppContainer
import com.codex.sharemonitor.ui.format.formatHm
import com.codex.sharemonitor.ui.viewmodel.SectorDetailViewModel

/**
 * 板块详情页。
 *
 * 说明：展示板块成分股列表，支持刷新与将成分股加入自选。
 */
@Composable
fun SectorDetailScreen(
    appContainer: AppContainer,
    sectorId: String,
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (String) -> Unit,
) {
    val vm: SectorDetailViewModel = viewModel(
        key = "sector:$sectorId",
        factory = SectorDetailViewModelFactory(appContainer = appContainer, sectorId = sectorId),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(state.sector?.name ?: "板块", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.refresh(force = false) }, enabled = !state.isRefreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }
        Text(state.sector?.id ?: sectorId, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(state.constituents, key = { it.symbol.key }) { c ->
                val symbol = c.symbol
                val quote = c.quote
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable { onOpenDetail(symbol.key) }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(symbol.name, style = MaterialTheme.typography.titleMedium)
                        val ts = quote?.let { it.quoteTimeEpochMillis ?: it.fetchedAtEpochMillis }
                        Text(
                            if (ts != null) "${symbol.exchange.name} ${symbol.code} · 更新 ${formatHm(ts)}" else "${symbol.exchange.name} ${symbol.code}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(quote?.lastPrice?.let { String.format("%.2f", it) } ?: "--")
                        Text(quote?.changePct?.let { String.format("%.2f%%", it) } ?: "")
                    }
                    IconButton(onClick = { vm.addToWatchlist(c) }) {
                        Icon(Icons.Filled.Add, contentDescription = "加入自选")
                    }
                }
            }
        }
    }
}

private class SectorDetailViewModelFactory(
    private val appContainer: AppContainer,
    private val sectorId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SectorDetailViewModel(
            sectorId = sectorId,
            quotesRepository = appContainer.quotesRepository,
            watchlistRepository = appContainer.watchlistRepository,
        ) as T
    }
}
