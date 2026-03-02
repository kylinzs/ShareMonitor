package com.codex.sharemonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codex.sharemonitor.data.AppContainer

/**
 * 应用级 ViewModel 工厂。
 *
 * 说明：集中管理 ViewModel 的依赖注入（基于 [AppContainer]），避免在各 Screen 中重复拼装依赖。
 */
class AppViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WatchlistViewModel::class.java) -> {
                WatchlistViewModel(
                    watchlistRepository = appContainer.watchlistRepository,
                    settingsRepository = appContainer.settingsRepository,
                    quotesRepository = appContainer.quotesRepository,
                ) as T
            }

            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(
                    quotesRepository = appContainer.quotesRepository,
                    watchlistRepository = appContainer.watchlistRepository,
                ) as T
            }

            modelClass.isAssignableFrom(DetailViewModel::class.java) -> {
                DetailViewModel(
                    quotesRepository = appContainer.quotesRepository,
                    watchlistRepository = appContainer.watchlistRepository,
                ) as T
            }

            modelClass.isAssignableFrom(MarketViewModel::class.java) -> {
                MarketViewModel(
                    quotesRepository = appContainer.quotesRepository,
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
