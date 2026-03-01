package com.afhara.mywebsocket.feature.dashboard.persentation

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.afhara.mywebsocket.data.model.CryptoTicker
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onCryptoClick: (String) -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle-aware WebSocket connect/disconnect
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onEvent(DashboardEvent.OnScreenResume)
                Lifecycle.Event.ON_PAUSE -> viewModel.onEvent(DashboardEvent.OnScreenLeave)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DashboardEffect.NavigateToDetail -> onCryptoClick(effect.symbol)
                is DashboardEffect.ShowError -> { /* TODO: snackbar */ }
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
                    Button(onClick = { viewModel.onEvent(DashboardEvent.RetryLoad) }) {
                        Text("Retry")
                    }
                }
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Real-time crypto prices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        state.tickers
                    ) { ticker ->
                        CryptoCard(
                            ticker = ticker,
                            flash = state.priceFlash[ticker.id] ?: PriceFlash.NONE,
                            onClick = { viewModel.onEvent(DashboardEvent.OnCryptoClick(ticker.symbol)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoCard(
    ticker: CryptoTicker,
    flash: PriceFlash,
    onClick: () -> Unit,
) {
    val isPositive = ticker.priceChangePercent >= 0
    val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    // Flash animation
    val flashColor = when (flash) {
        PriceFlash.UP -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        PriceFlash.DOWN -> Color(0xFFF44336).copy(alpha = 0.15f)
        PriceFlash.NONE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val animatedColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = 300),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = animatedColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Coin name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = ticker.symbol.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$${formatPrice(ticker.price)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (isPositive) "+" else ""}${formatPercent(ticker.priceChangePercent)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = changeColor,
            )
        }
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