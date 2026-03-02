package com.codex.sharemonitor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codex.sharemonitor.data.AppContainer
import com.codex.sharemonitor.data.settings.AppSettings
import kotlinx.coroutines.launch

/**
 * 设置页。
 *
 * 说明：配置报价数据源、自动刷新与刷新间隔等，设置持久化到 DataStore。
 */
@Composable
fun SettingsScreen(appContainer: AppContainer) {
    val settings by appContainer.settingsRepository.settingsFlow.collectAsState(initial = AppSettings.default)
    val scope = rememberCoroutineScope()
    val supportedSources = appContainer.settingsRepository.supportedDataSources()
    val refreshIntervals = listOf(15, 30, 60)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("设置", style = MaterialTheme.typography.titleLarge)

        Text("数据源", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        supportedSources.forEach { source ->
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        scope.launch { appContainer.settingsRepository.setQuoteDataSourceId(source) }
                    },
            ) {
                RadioButton(
                    selected = settings.quoteDataSourceId == source,
                    onClick = { scope.launch { appContainer.settingsRepository.setQuoteDataSourceId(source) } },
                )
                Text(source.displayName, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text("自动刷新", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Switch(
                checked = settings.autoRefreshEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { appContainer.settingsRepository.setAutoRefreshEnabled(enabled) }
                },
            )
            Text(
                if (settings.autoRefreshEnabled) "已开启" else "已关闭",
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }

        Text("刷新间隔", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        refreshIntervals.forEach { minutes ->
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { scope.launch { appContainer.settingsRepository.setRefreshIntervalMinutes(minutes) } },
            ) {
                RadioButton(
                    selected = settings.refreshIntervalMinutes == minutes,
                    onClick = { scope.launch { appContainer.settingsRepository.setRefreshIntervalMinutes(minutes) } },
                )
                Text("$minutes 分钟", modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text(
            "免责声明：本应用仅供自用参考，行情数据来源于公开接口，可能存在延迟与误差。",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
