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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.sharemonitor.data.AppContainer
import com.codex.sharemonitor.ui.viewmodel.AppViewModelFactory
import com.codex.sharemonitor.ui.viewmodel.SearchViewModel

/**
 * 股票搜索页。
 *
 * 说明：展示搜索结果，并在查询时提示“命中数据源/已尝试数据源”，便于用户理解回退策略与排障。
 */
@Composable
fun SearchScreen(
    appContainer: AppContainer,
    snackbarHostState: SnackbarHostState,
    onOpenDetail: (String) -> Unit,
) {
    val vm: SearchViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("搜索", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            label = { Text("代码 / 名称") },
        )

        if (state.query.isNotBlank()) {
            val matched = state.matchedSourceName
            val attempted = state.attemptedSources
            // 透明化数据源信息：命中哪个数据源、失败时已尝试哪些数据源（便于排障与用户理解）。
            val sourceText = when {
                matched != null -> "数据源：$matched"
                attempted.isNotEmpty() -> "已尝试：${attempted.joinToString(" / ")}"
                else -> "数据源：--"
            }
            Text(sourceText, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
        }

        state.errorMessage?.let { err ->
            Text(err, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
            Button(onClick = vm::retry, modifier = Modifier.padding(top = 8.dp)) {
                Text("重试")
            }
        }

        if (!state.isLoading && state.query.isNotBlank() && state.results.isEmpty() && state.errorMessage == null) {
            Text("没有匹配结果", modifier = Modifier.padding(top = 12.dp))
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(state.results, key = { it.key }) { symbol ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable { onOpenDetail(symbol.key) }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(symbol.name, style = MaterialTheme.typography.titleMedium)
                        Text("${symbol.exchange.name} ${symbol.code}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { vm.addToWatchlist(symbol) }) {
                        Icon(Icons.Filled.Add, contentDescription = "加入自选")
                    }
                }
            }
        }
    }
}
