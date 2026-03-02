package com.codex.sharemonitor

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.codex.sharemonitor.data.AppContainer
import com.codex.sharemonitor.ui.AppScaffold
import com.codex.sharemonitor.ui.theme.ShareMonitorTheme

/**
 * 应用根 Composable。
 *
 * 说明：统一应用主题与顶层 Scaffold（导航、Snackbar 等）。
 */
@Composable
fun ShareMonitorApp(appContainer: AppContainer) {
    ShareMonitorTheme {
        Surface {
            AppScaffold(appContainer = appContainer)
        }
    }
}
