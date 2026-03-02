package com.codex.sharemonitor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Watchlist(route = "watchlist", label = "自选", icon = Icons.Filled.Star),
    Search(route = "search", label = "搜索", icon = Icons.Filled.Search),
    Market(route = "market", label = "行情", icon = Icons.AutoMirrored.Filled.ShowChart),
    Settings(route = "settings", label = "设置", icon = Icons.Filled.Settings),
}
