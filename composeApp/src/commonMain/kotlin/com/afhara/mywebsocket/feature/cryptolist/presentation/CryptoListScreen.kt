package com.afhara.mywebsocket.feature.cryptolist.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.afhara.mywebsocket.data.model.CryptoTicker
import com.afhara.mywebsocket.feature.dashboard.presentation.PriceFlash
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CryptoListScreen(
    onCryptoClick: (String) -> Unit,
    viewModel: CryptoListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()

    // Lifecycle-aware WebSocket
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onEvent(CryptoListEvent.OnScreenResume)
                Lifecycle.Event.ON_PAUSE -> viewModel.onEvent(CryptoListEvent.OnScreenLeave)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pagination: detect when scrolled near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 3 && !state.isLoadingMore && state.hasMorePages
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.onEvent(CryptoListEvent.LoadMore)
        }
    }

    // Side effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CryptoListEffect.NavigateToDetail -> onCryptoClick(effect.id)
                is CryptoListEffect.ShowError -> { /* TODO: snackbar */ }
            }
        }
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading market data...")
                }
            }
        }

        state.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onEvent(CryptoListEvent.RetryLoad) }) {
                        Text("Retry")
                    }
                }
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Market",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.tickers.size} coins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(30.dp),
                    )
                    Text(
                        text = "Coin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(100.dp),
                    )
                    Text(
                        text = "24h %",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(70.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                LazyColumn(state = listState) {
                    items(
                        items = state.tickers,
                        key = { it.id },
                    ) { ticker ->
                        CryptoListItem(
                            ticker = ticker,
                            flash = state.priceFlash[ticker.id] ?: PriceFlash.NONE,
                            onClick = { viewModel.onEvent(CryptoListEvent.OnCryptoClick(ticker.id)) },
                        )
                        HorizontalDivider()
                    }

                    // Loading more indicator
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoListItem(
    ticker: CryptoTicker,
    flash: PriceFlash,
    onClick: () -> Unit,
) {
    val isPositive = ticker.priceChangePercent >= 0
    val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    val flashColor = when (flash) {
        PriceFlash.UP -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        PriceFlash.DOWN -> Color(0xFFF44336).copy(alpha = 0.1f)
        PriceFlash.NONE -> Color.Transparent
    }
    val animatedColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = 300),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank
        Text(
            text = "${ticker.marketCapRank ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp),
        )

        // Name + Symbol
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticker.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ticker.symbol.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Price
        Card(
            colors = CardDefaults.cardColors(containerColor = animatedColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "$${formatPrice(ticker.price)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(100.dp).padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }

        // 24h change
        Text(
            text = "${if (isPositive) "+" else ""}${formatPercent(ticker.priceChangePercent)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = changeColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(70.dp),
        )
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 1.0) {
        val parts = price.toLong().toString().reversed().chunked(3).joinToString(",").reversed()
        val decimal = ((price % 1) * 100).toInt().toString().padStart(2, '0')
        "$parts.$decimal"
    } else {
        val str = price.toString()
        val dotIndex = str.indexOf('.')
        if (dotIndex == -1) str
        else str.take((dotIndex + 7).coerceAtMost(str.length))
    }
}

private fun formatPercent(value: Double): String {
    val abs = if (value < 0) -value else value
    val intPart = abs.toLong()
    val decPart = ((abs % 1) * 100).toInt().toString().padStart(2, '0')
    val result = "$intPart.$decPart"
    return if (value < 0) "-$result" else result
}