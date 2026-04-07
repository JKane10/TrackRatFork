package com.trackrat.android.ui.traindetail

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackrat.android.R
import com.trackrat.android.data.models.StopDetail
import com.trackrat.android.ui.components.GlassmorphicCard
import com.trackrat.android.ui.trainlist.Tuple4
import com.trackrat.android.utils.Constants
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainDetailScreen(
    trainId: String,
    date: String? = null,
    originCode: String? = null,
    destinationCode: String? = null,
    viewModel: TrainDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load train details when screen opens and save origin/destination codes
    LaunchedEffect(trainId, date, originCode, destinationCode) {
        originCode?.let { viewModel.setOriginCode(it) }
        destinationCode?.let { viewModel.setDestinationCode(it) }
        viewModel.loadTrainDetails(trainId, date)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.train_detail_title, trainId),
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.train?.let { train ->
                            Text(
                                text = stringResource(R.string.train_detail_route, train.route.origin, train.route.destination),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
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
                uiState.isLoading && uiState.train == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 32.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF6600)
                            )
                            Text(stringResource(R.string.train_detail_loading))
                        }
                    }
                }
                
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.train_detail_error_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.error?.message ?: stringResource(R.string.train_detail_unknown_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6600)
                                )
                            ) {
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }
                }
                
                uiState.train == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                text = stringResource(R.string.train_detail_not_found),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.train_detail_not_found_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.train?.let { train ->

                            
                            // Track Predictions section (iOS-style segmented visualization)
                            if (viewModel.shouldShowPredictions()) {
                                item {
                                    SegmentedTrackPredictionBar(
                                        platformPredictions = uiState.platformPredictions ?: emptyMap(),
                                        isLoading = uiState.isLoadingPredictions
                                    )
                                }
                            }
                            
                            // Journey stops
                            item {
                                Text(
                                    text = stringResource(R.string.train_detail_journey_stops),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            val stops = train.stops
                            if (stops.isNotEmpty()) {
                                items(stops) { stop ->
                                    StopCard(
                                        stop = stop,
                                        isOrigin = stop.station.code == train.route.originCode,
                                        isTerminal = stop.station.code == train.route.destinationCode
                                    )
                                }
                            }
                            
                            // Last updated
                            item {
                                if (uiState.lastUpdated > 0) {
                                    Text(
                                        text = stringResource(R.string.updated_time, formatLastUpdated(uiState.lastUpdated)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// Old TrackPredictionsCard removed - replaced with SegmentedTrackPredictionBar

@Composable
fun StopCard(
    stop: StopDetail,
    isOrigin: Boolean = false,
    isTerminal: Boolean = false
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = when {
            isOrigin -> Color(Constants.BRAND_ORANGE).copy(alpha = 0.15f)
            isTerminal -> Color.White.copy(alpha = 0.12f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        padding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stop indicator
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = when {
                    isOrigin -> Color(0xFFFF6600)
                    isTerminal -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            
            // Station info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.station.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isOrigin || isTerminal) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            // Time info
            Column(horizontalAlignment = Alignment.End) {
                // Departure time
                stop.scheduledDeparture?.let { scheduledDep ->
                    Text(
                        text = scheduledDep.format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Arrival time (if different from departure)
                stop.scheduledArrival?.let { scheduledArr ->
                    if (stop.scheduledDeparture == null || scheduledArr != stop.scheduledDeparture) {
                        Text(
                            text = stringResource(R.string.train_detail_arrival, scheduledArr.format(DateTimeFormatter.ofPattern("h:mm a"))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Track if available
                if (!stop.track.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.train_detail_track, stop.track),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF6600),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    status: String,
    isBoarding: Boolean
) {
    val (backgroundColor, textColor) = when {
        isBoarding -> Color(0xFFFF6600) to Color.White
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
fun PredictionChipDetailed(
    prediction: com.trackrat.android.data.models.PredictionData
) {
    // Get confidence-based styling (confidence is 0.0-1.0 float)
    val (backgroundColor, textColor, fontWeight, confidenceIcon) = when {
        prediction.confidence >= 0.8f -> {
            // High confidence: Bold with checkmark
            Tuple4(
                Color(0xFFFF6600),
                Color.White,
                FontWeight.Bold,
                "✓"
            )
        }
        prediction.confidence >= 0.5f -> {
            // Medium confidence: Normal styling  
            Tuple4(
                Color(0xFFFF6600).copy(alpha = 0.15f),
                Color(0xFFFF6600),
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

    Column(horizontalAlignment = Alignment.End) {
        // Prediction chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor
        ) {
            Text(
                text = "🦉 ${prediction.primaryPrediction}$confidenceIcon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = fontWeight,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        
        // Confidence percentage
        Text(
            text = prediction.confidenceText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


private fun formatTime(timeString: String): String {
    // Extract time portion if it's an ISO string, otherwise return as-is
    return try {
        if (timeString.contains("T")) {
            // ISO format: extract HH:mm
            val timePart = timeString.substringAfter("T").substringBefore("-")
            timePart.substring(0, 5) // Get HH:mm
        } else {
            timeString
        }
    } catch (e: Exception) {
        Log.e(null, e.message.toString())
        timeString
    }
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

@Preview(showBackground = true)
@Composable
fun TrainDetailScreenPreview() {
    TrainDetailScreen(
        trainId = "1234",
        onNavigateBack = {}
    )
}
