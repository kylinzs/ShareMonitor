package com.codex.sharemonitor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.codex.sharemonitor.data.AppContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.sharemonitor.ui.viewmodel.AppViewModelFactory
import com.codex.sharemonitor.ui.viewmodel.WatchlistViewModel
import com.codex.sharemonitor.ui.format.formatHm

/**
 * 自选页（首页）。
 *
 * 说明：展示自选列表、支持刷新/置顶/排序/移除，并可跳转到个股详情或搜索页。
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun WatchlistScreen(
    appContainer: AppContainer,
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    val vm: WatchlistViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("自选", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.refresh(force = false) }, enabled = !state.isRefreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }

        if (state.items.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
                Text("还没有添加自选股", style = MaterialTheme.typography.titleMedium)
                Text("去搜索添加股票到自选列表。", modifier = Modifier.padding(top = 8.dp))
                Button(onClick = onNavigateToSearch, modifier = Modifier.padding(top = 12.dp)) {
                    Text("去搜索")
                }
            }
            return
        }

        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.isRefreshing,
            onRefresh = { vm.refresh(force = false) },
        )

        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            LazyColumn {
                items(state.items, key = { it.entry.symbol.key }) { item ->
                    val symbol = item.entry.symbol
                    val quote = item.quote

                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable { onOpenDetail(symbol.key) }
                    ) {
                        Row {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(symbol.name, style = MaterialTheme.typography.titleMedium)
                                Text("${symbol.exchange.name} ${symbol.code}", style = MaterialTheme.typography.bodySmall)
                                quote?.let { q ->
                                    val ts = q.quoteTimeEpochMillis ?: q.fetchedAtEpochMillis
                                    Text("更新 ${formatHm(ts)} · ${q.sourceName}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (item.isStale) {
                                    Text("可能已过期", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Column {
                                Text(
                                    quote?.lastPrice?.let { String.format("%.2f", it) } ?: "--",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    quote?.changePct?.let { String.format("%.2f%%", it) } ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            IconButton(onClick = { vm.togglePinned(symbol.key, pinned = !item.entry.pinned) }) {
                                Icon(
                                    imageVector = if (item.entry.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "置顶",
                                )
                            }
                            IconButton(onClick = { vm.moveUp(symbol.key) }) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "上移")
                            }
                            IconButton(onClick = { vm.moveDown(symbol.key) }) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "下移")
                            }
                            IconButton(onClick = { vm.remove(symbol.key) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "移除")
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
