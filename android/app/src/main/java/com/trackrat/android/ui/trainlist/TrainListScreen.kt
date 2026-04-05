package com.trackrat.android.ui.trainlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackrat.android.data.models.TrainV2
import com.trackrat.android.ui.components.ErrorContent
import com.trackrat.android.ui.components.GlassmorphicCard
import com.trackrat.android.ui.components.TrainListSkeleton
import com.trackrat.android.utils.Constants
import com.trackrat.android.utils.HapticFeedbackHelper
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainListScreen(
    fromStation: String,
    toStation: String?,
    viewModel: TrainListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onTrainClicked: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    // Load trains when screen opens
    LaunchedEffect(fromStation, toStation) {
        viewModel.loadTrains(fromStation, toStation)
    }

    // Auto-refresh every 30 seconds (matching iOS app)
    LaunchedEffect(fromStation, toStation) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            HapticFeedbackHelper.performRefreshHaptic(context, uiState.hapticFeedbackEnabled)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (toStation != null) 
                                "Trains to ${uiState.toStationName ?: toStation}"
                            else 
                                "All Departures",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "From ${uiState.fromStationName ?: fromStation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        HapticFeedbackHelper.performLightHaptic(hapticFeedback, uiState.hapticFeedbackEnabled)
                        onNavigateBack() 
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        HapticFeedbackHelper.performMediumHaptic(hapticFeedback, uiState.hapticFeedbackEnabled)
                        viewModel.refresh() 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading && uiState.trains.isEmpty() -> {
                    TrainListSkeleton()
                }
                
                uiState.error != null -> {
                    val error = uiState.error
                    if (error != null) {
                        ErrorContent(
                            error = error,
                            canRetry = uiState.canRetry,
                            onRetryClick = { 
                                HapticFeedbackHelper.performMediumHaptic(hapticFeedback, uiState.hapticFeedbackEnabled)
                                viewModel.retry() 
                            },
                            hapticFeedbackEnabled = uiState.hapticFeedbackEnabled
                        )
                    }
                }
                
                uiState.trains.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No Trains Found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "There are currently no trains scheduled for this route.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Last updated indicator
                        item {
                            if (uiState.lastUpdated > 0) {
                                Text(
                                    text = "Updated ${formatLastUpdated(uiState.lastUpdated)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        
                        items(uiState.trains) { train ->
                            TrainCard(
                                train = train,
                                fromStation = fromStation,
                                viewModel = viewModel,
                                onClick = { 
                                    HapticFeedbackHelper.performLightHaptic(hapticFeedback, uiState.hapticFeedbackEnabled)
                                    onTrainClicked(train.trainId) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainCard(
    train: TrainV2,
    fromStation: String,
    viewModel: TrainListViewModel,
    onClick: () -> Unit
) {
    val isBoarding = viewModel.isTrainBoarding(train) && !train.track.isNullOrEmpty()
    
    // Use orange card for boarding trains, glassmorphic for others
    if (isBoarding) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(Constants.BRAND_ORANGE)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            TrainCardContent(
                train = train,
                fromStation = fromStation,
                viewModel = viewModel,
                isBoarding = true,
                textColor = Color.White
            )
        }
    } else {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            cornerRadius = 12.dp,
            padding = 10.dp
        ) {
            TrainCardContent(
                train = train,
                fromStation = fromStation,
                viewModel = viewModel,
                isBoarding = false,
                textColor = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun TrainCardContent(
    train: TrainV2,
    fromStation: String,
    viewModel: TrainListViewModel,
    isBoarding: Boolean,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (isBoarding) 10.dp else 0.dp), // GlassmorphicCard already has padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: train number + destination
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Train ${train.trainId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "to ${train.destination ?: train.terminalStationName}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: departure → arrival, track chip, status chip
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val arrivalTime = formatArrivalTime(train)
            val timeText = if (arrivalTime != null)
                "${formatDepartureTime(train, fromStation)} → $arrivalTime"
            else
                formatDepartureTime(train, fromStation)
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            if (!train.track.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBoarding)
                        Color.White.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Tr ${train.track}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBoarding) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            } else if (train.prediction != null) {
                PredictionChip(prediction = train.prediction)
            }

            StatusChip(
                status = viewModel.getTrainDisplayStatus(train),
                isBoarding = viewModel.isTrainBoarding(train)
            )
        }
    }
}

@Composable
fun StatusChip(
    status: String,
    isBoarding: Boolean
) {
    val (backgroundColor, textColor) = when {
        isBoarding -> Color(Constants.BRAND_ORANGE) to Color.White
        status.contains("DELAYED", ignoreCase = true) -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        status.contains("CANCELLED", ignoreCase = true) -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        status.contains("DEPARTED", ignoreCase = true) -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = if (isBoarding) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PredictionChip(
    prediction: com.trackrat.android.data.models.PredictionData?
) {
    if (prediction == null) return
    // Get confidence-based styling (confidence is 0.0-1.0 float)
    val styling = when {
        prediction.confidence >= 0.8f -> {
            // High confidence: Bold with checkmark
            Tuple4(
                Color(Constants.BRAND_ORANGE),
                Color.White,
                FontWeight.Bold,
                "✓"
            )
        }
        prediction.confidence >= 0.5f -> {
            // Medium confidence: Normal styling
            Tuple4(
                Color(Constants.BRAND_ORANGE).copy(alpha = 0.15f),
                Color(Constants.BRAND_ORANGE),
                FontWeight.Medium,
                ""
            )
        }
        else -> {
            // Low confidence: Gray with question mark
            Tuple4(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                FontWeight.Normal,
                "?"
            )
        }
    }
    
    val backgroundColor = styling.first
    val textColor = styling.second
    val fontWeight = styling.third
    val confidenceIcon = styling.fourth

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = "🦉 ${prediction.primaryPrediction}$confidenceIcon",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = fontWeight,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// Helper class for multiple return values
data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatDepartureTime(train: TrainV2, fromStation: String): String {
    return train.getScheduledDepartureTime(fromStation)?.format(
        DateTimeFormatter.ofPattern("h:mm a")
    ) ?: "N/A"
}

private fun formatArrivalTime(train: TrainV2): String? {
    return train.scheduledArrival?.format(DateTimeFormatter.ofPattern("h:mm a"))
}

private fun formatLastUpdated(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000} min ago"
        else -> "${diff / 3600_000} hr ago"
    }
}

//@Preview(showBackground = true)
//@Composable
//fun TrainListScreenPreview() {
//    TrainListScreen(
//        fromStation = "NY",
//        toStation = "NP",
//        onNavigateBack = {},
//        onTrainClicked = {}
//    )
//}