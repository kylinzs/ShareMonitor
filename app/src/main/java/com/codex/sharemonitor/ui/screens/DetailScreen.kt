package com.codex.sharemonitor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.sharemonitor.data.AppContainer
import com.codex.sharemonitor.domain.model.HistoryPoint
import com.codex.sharemonitor.domain.model.HistoryType
import com.codex.sharemonitor.ui.format.formatHm
import com.codex.sharemonitor.ui.viewmodel.AppViewModelFactory
import com.codex.sharemonitor.ui.viewmodel.DetailViewModel

/**
 * 个股详情页。
 *
 * 说明：展示报价与走势（分时/日K），支持刷新与自选切换；在未开盘/分时不可用时会提示并回退显示最近收盘。
 */
@Composable
fun DetailScreen(
    appContainer: AppContainer,
    symbolKey: String,
    snackbarHostState: SnackbarHostState,
) {
    val vm: DetailViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(symbolKey) {
        vm.setSymbolKey(symbolKey)
    }

    LaunchedEffect(vm) {
        vm.snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.symbol?.name ?: "详情", style = MaterialTheme.typography.titleLarge)
                state.symbol?.let { Text("${it.exchange.name} ${it.code}", style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = { vm.toggleWatchlist() }) {
                Icon(
                    imageVector = if (state.isInWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "自选",
                )
            }
            IconButton(onClick = { vm.refresh(force = false) }, enabled = !state.isRefreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }

        val quote = state.quote
        Column(modifier = Modifier.padding(top = 12.dp)) {
            quote?.let { q ->
                val ts = q.quoteTimeEpochMillis ?: q.fetchedAtEpochMillis
                Text("时间：${formatHm(ts)} · ${q.sourceName}", style = MaterialTheme.typography.bodySmall)
            }
            state.marketStatusMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text("最新价：${quote?.lastPrice?.let { String.format("%.2f", it) } ?: "--"}")
            Text("涨跌：${quote?.change?.let { String.format("%.2f", it) } ?: "--"}  ${quote?.changePct?.let { String.format("%.2f%%", it) } ?: ""}")
            Text("开/高/低：${quote?.open ?: "--"} / ${quote?.high ?: "--"} / ${quote?.low ?: "--"}")
            Text("成交量：${quote?.volume ?: "--"}")
        }

        Row(modifier = Modifier.padding(top = 12.dp)) {
            FilterChip(
                selected = state.historyType == HistoryType.Intraday,
                onClick = { vm.setHistoryType(HistoryType.Intraday) },
                label = { Text("分时") },
            )
            FilterChip(
                selected = state.historyType == HistoryType.DailyK,
                onClick = { vm.setHistoryType(HistoryType.DailyK) },
                label = { Text("日K") },
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        val series = state.history
        val fallbackSeries = state.fallbackDailyHistory
        val usingFallback = state.historyType == HistoryType.Intraday && fallbackSeries != null
        val displaySeries = when {
            state.historyType == HistoryType.Intraday && usingFallback -> fallbackSeries
            else -> series
        }

        if (displaySeries == null || displaySeries.points.isEmpty()) {
            Text("暂无走势数据", modifier = Modifier.padding(top = 12.dp))
        } else {
            if (usingFallback) {
                Text(
                    "分时不可用，已显示最近收盘（日K）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (state.historyType == HistoryType.Intraday && !usingFallback) {
                IntradayLineChart(points = displaySeries.points, modifier = Modifier.padding(top = 12.dp).height(200.dp).fillMaxWidth())
            } else {
                DailyCandlestickChart(points = displaySeries.points, modifier = Modifier.padding(top = 12.dp).height(220.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun IntradayLineChart(points: List<HistoryPoint>, modifier: Modifier = Modifier) {
    val closes = points.map { it.close }
    val min = closes.minOrNull() ?: return
    val max = closes.maxOrNull() ?: return
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val step = if (points.size <= 1) 0f else w / (points.size - 1)

        var prev: Offset? = null
        points.forEachIndexed { i, p ->
            val x = step * i
            val y = h - (((p.close - min) / range).toFloat() * h)
            val cur = Offset(x, y)
            val prevOffset = prev
            if (prevOffset != null) {
                drawLine(
                    color = lineColor,
                    start = prevOffset,
                    end = cur,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
            prev = cur
        }
    }
}

@Composable
private fun DailyCandlestickChart(points: List<HistoryPoint>, modifier: Modifier = Modifier) {
    val lows = points.mapNotNull { it.low ?: it.close }
    val highs = points.mapNotNull { it.high ?: it.close }
    val min = lows.minOrNull() ?: return
    val max = highs.maxOrNull() ?: return
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val candleWidth = (w / points.size).coerceAtLeast(6f)
        points.forEachIndexed { i, p ->
            val open = p.open ?: p.close
            val close = p.close
            val high = p.high ?: maxOf(open, close)
            val low = p.low ?: minOf(open, close)

            val xCenter = (i + 0.5f) * (w / points.size)
            fun yFor(v: Double): Float = h - (((v - min) / range).toFloat() * h)

            val color = if (close >= open) Color(0xFFd32f2f) else Color(0xFF2e7d32)
            val yHigh = yFor(high)
            val yLow = yFor(low)
            val yOpen = yFor(open)
            val yClose = yFor(close)

            drawLine(
                color = color,
                start = Offset(xCenter, yHigh),
                end = Offset(xCenter, yLow),
                strokeWidth = 2f,
            )

            val top = minOf(yOpen, yClose)
            val bottom = maxOf(yOpen, yClose)
            drawLine(
                color = color,
                start = Offset(xCenter - candleWidth / 2f, top),
                end = Offset(xCenter + candleWidth / 2f, top),
                strokeWidth = 2f,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = color,
                start = Offset(xCenter - candleWidth / 2f, bottom),
                end = Offset(xCenter + candleWidth / 2f, bottom),
                strokeWidth = 2f,
                cap = StrokeCap.Square,
            )
            if (bottom - top > 2f) {
                drawLine(
                    color = color,
                    start = Offset(xCenter - candleWidth / 2f, top),
                    end = Offset(xCenter - candleWidth / 2f, bottom),
                    strokeWidth = 4f,
                    cap = StrokeCap.Square,
                )
                drawLine(
                    color = color,
                    start = Offset(xCenter + candleWidth / 2f, top),
                    end = Offset(xCenter + candleWidth / 2f, bottom),
                    strokeWidth = 4f,
                    cap = StrokeCap.Square,
                )
            }
        }

        drawRect(
            color = outlineColor,
            style = Stroke(width = 2f),
        )
    }
}
