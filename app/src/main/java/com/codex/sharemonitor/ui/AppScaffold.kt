package com.codex.sharemonitor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codex.sharemonitor.data.AppContainer
import com.codex.sharemonitor.ui.screens.DetailScreen
import com.codex.sharemonitor.ui.screens.MarketScreen
import com.codex.sharemonitor.ui.screens.SearchScreen
import com.codex.sharemonitor.ui.screens.SectorDetailScreen
import com.codex.sharemonitor.ui.screens.SettingsScreen
import com.codex.sharemonitor.ui.screens.WatchlistScreen

/**
 * 应用主 Scaffold（底部导航 + 路由）。
 *
 * 说明：
 * - 负责顶层导航图（NavHost）与 BottomNavigation 的联动；
 * - 统一 SnackbarHostState 并注入到各页面（用于展示网络/操作提示）；
 * - detail/sector 等二级页面通过 route 参数传递 key/id。
 */
@Composable
fun AppScaffold(appContainer: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val detailRoute = "detail/{symbolKey}"
    val sectorRoute = "sector/{sectorId}"

    val items = listOf(
        TopLevelDestination.Watchlist,
        TopLevelDestination.Search,
        TopLevelDestination.Market,
        TopLevelDestination.Settings,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Watchlist.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.Watchlist.route) {
                WatchlistScreen(
                    appContainer = appContainer,
                    snackbarHostState = snackbarHostState,
                    onOpenDetail = { symbolKey -> navController.navigate("detail/$symbolKey") },
                    onNavigateToSearch = { navController.navigate(TopLevelDestination.Search.route) },
                )
            }
            composable(TopLevelDestination.Search.route) {
                SearchScreen(
                    appContainer = appContainer,
                    snackbarHostState = snackbarHostState,
                    onOpenDetail = { symbolKey -> navController.navigate("detail/$symbolKey") },
                )
            }
            composable(TopLevelDestination.Market.route) {
                MarketScreen(
                    appContainer = appContainer,
                    snackbarHostState = snackbarHostState,
                    onOpenSector = { sectorId -> navController.navigate("sector/$sectorId") },
                )
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(appContainer = appContainer)
            }
            composable(
                route = detailRoute,
                arguments = listOf(navArgument("symbolKey") { nullable = false }),
            ) { backStackEntry ->
                DetailScreen(
                    appContainer = appContainer,
                    symbolKey = backStackEntry.arguments?.getString("symbolKey") ?: "",
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(
                route = sectorRoute,
                arguments = listOf(navArgument("sectorId") { nullable = false }),
            ) { backStackEntry ->
                SectorDetailScreen(
                    appContainer = appContainer,
                    sectorId = backStackEntry.arguments?.getString("sectorId") ?: "",
                    snackbarHostState = snackbarHostState,
                    onOpenDetail = { symbolKey -> navController.navigate("detail/$symbolKey") },
                )
            }
        }
    }
}
