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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codex.sharemonitor.data.AppContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.sharemonitor.ui.viewmodel.AppViewModelFactory
import com.codex.sharemonitor.ui.viewmodel.MarketTab
import com.codex.sharemonitor.ui.viewmodel.MarketViewModel
import com.codex.sharemonitor.ui.viewmodel.SectorSort
import com.codex.sharemonitor.ui.format.formatHm

/**
 * 行情页（指数/板块）。
 *
 * 说明：指数与板块行情固定使用 Eastmoney 数据源；此页主要展示缓存并触发刷新。
 */
@Composable
fun MarketScreen(
    appContainer: AppContainer,
    snackbarHostState: SnackbarHostState,
    onOpenSector: (String) -> Unit,
) {
    val vm: MarketViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("行情", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.refreshAll(force = false) }, enabled = !state.isRefreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }

        val tabIndex = if (state.tab == MarketTab.Indices) 0 else 1
        TabRow(selectedTabIndex = tabIndex, modifier = Modifier.padding(top = 12.dp)) {
            Tab(
                selected = state.tab == MarketTab.Indices,
                onClick = { vm.setTab(MarketTab.Indices) },
                text = { Text("指数") },
            )
            Tab(
                selected = state.tab == MarketTab.Sectors,
                onClick = { vm.setTab(MarketTab.Sectors) },
                text = { Text("板块") },
            )
        }

        if (state.tab == MarketTab.Indices) {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(state.indices, key = { it.symbol.key }) { item ->
                    val quote = item.quote
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.index.name, style = MaterialTheme.typography.titleMedium)
                            val ts = quote?.let { it.quoteTimeEpochMillis ?: it.fetchedAtEpochMillis }
                            Text(
                                if (ts != null) "${item.index.code} · 更新 ${formatHm(ts)}" else item.index.code,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column {
                            Text(quote?.lastPrice?.let { String.format("%.2f", it) } ?: "--")
                            Text(quote?.changePct?.let { String.format("%.2f%%", it) } ?: "")
                        }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.padding(top = 12.dp)) {
                FilterChip(
                    selected = state.sectorSort == SectorSort.ChangePctDesc,
                    onClick = { vm.setSectorSort(SectorSort.ChangePctDesc) },
                    label = { Text("涨跌幅") },
                )
                FilterChip(
                    selected = state.sectorSort == SectorSort.NameAsc,
                    onClick = { vm.setSectorSort(SectorSort.NameAsc) },
                    label = { Text("名称") },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(state.sectors, key = { it.sector.id }) { item ->
                    val sector = item.sector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clickable { onOpenSector(sector.id) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sector.name, style = MaterialTheme.typography.titleMedium)
                            val ts = sector.quoteTimeEpochMillis
                            Text(
                                if (ts != null) "${sector.id} · 更新 ${formatHm(ts)}" else sector.id,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(sector.changePct?.let { String.format("%.2f%%", it) } ?: "")
                    }
                }
            }
        }
    }
}
