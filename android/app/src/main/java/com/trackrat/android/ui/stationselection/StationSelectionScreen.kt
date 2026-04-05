package com.trackrat.android.ui.stationselection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackrat.android.ui.components.GlassmorphicCard
import com.trackrat.android.ui.components.GlassmorphicCardElevated
import com.trackrat.android.ui.components.GlassmorphicSearchCard
import com.trackrat.android.ui.map.MapContainerViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSelectionScreen(
    mapViewModel: MapContainerViewModel,
    viewModel: StationSelectionViewModel = hiltViewModel(),
    onNavigateToDestination: (originCode: String) -> Unit,
    onNavigateToTrainDetail: (trainId: String) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val departureStations by viewModel.displayedDepartureStations.collectAsState()
    val ratSenseSuggestions by viewModel.ratSenseSuggestions.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Reset map to default view when navigating back to station selection
    LaunchedEffect(Unit) {
        mapViewModel.clearSelectedRoute()
        mapViewModel.resetToDefaultView()
    }

    // Check if search text looks like a train number
    val isTrainSearch = searchText.isNotBlank() && (
        searchText.all { it.isDigit() } || // NJT trains (all digits)
        searchText.startsWith("A", ignoreCase = true) // Amtrak trains
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Where would you like to leave from?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // RatSense AI Suggestions
            if (ratSenseSuggestions.isNotEmpty() && searchText.isBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RatSense Suggestions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ratSenseSuggestions.take(2).forEach { suggestion ->
                        GlassmorphicCardElevated(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Navigate to destination selection with suggested from station
                                    onNavigateToDestination(suggestion.from)
                                }
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${suggestion.from} → ${suggestion.to}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = suggestion.reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Suggested",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search bar
            GlassmorphicSearchCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        if (!isTrainSearch) {
                            viewModel.searchStations(it.trim())
                        }
                    },
                    placeholder = {
                        Text(
                            "Search stations or train number",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Train search result or station list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If searching for train, show train search card
                if (isTrainSearch) {
                    item {
                        GlassmorphicCardElevated(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onNavigateToTrainDetail(searchText.trim()) 
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Train ${searchText.trim()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (searchText.startsWith("A", ignoreCase = true)) 
                                            "Amtrak Train" 
                                        else 
                                            "NJ Transit Train",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    Icons.Default.Train,
                                    contentDescription = "Train",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // Show station list
                    items(departureStations) { station ->
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectOrigin(station)
                                    // Navigate to destination selection
                                    onNavigateToDestination(station.code)
                                },
                            padding = 10.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                // Heart icon for favoriting
                                val isFavorited by viewModel.isStationFavorited(station.code).collectAsState(initial = false)
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            viewModel.toggleFavoriteStation(station.code)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                                        tint = MaterialTheme.colorScheme.primary
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

// Simplified station selection - old complex content functions removed

// Preview removed - requires MapContainerViewModel which cannot be instantiated in preview
