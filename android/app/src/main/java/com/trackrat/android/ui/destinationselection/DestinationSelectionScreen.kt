package com.trackrat.android.ui.destinationselection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackrat.android.data.Stations
import com.trackrat.android.ui.components.GlassmorphicCard
import com.trackrat.android.ui.components.GlassmorphicSearchCard
import com.trackrat.android.ui.map.MapContainerViewModel
import com.trackrat.android.ui.stationselection.StationSelectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSelectionScreen(
    originStation: String,
    mapViewModel: MapContainerViewModel,
    viewModel: StationSelectionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToTrains: (destinationCode: String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val displayedStations by viewModel.displayedDestinationStations.collectAsState()
    val primaryDestinationCode by viewModel.primaryDestinationCode.collectAsState()
    var searchText by remember { mutableStateOf("") }

    // Filter out origin station from available destinations
    val availableStations = remember(displayedStations, originStation) {
        displayedStations.filter { it.code != originStation }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Where would you like to go?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar
            GlassmorphicSearchCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.searchDestinations(it.trim())
                    },
                    placeholder = {
                        Text(
                            "Search destinations...",
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

            // Removed "Show all departures" button as backend doesn't support it

            // Station list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableStations) { station ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectDestination(station)
                                // Animate map to show the route
                                val fromCoords = Stations.getCoordinates(originStation)
                                val toCoords = Stations.getCoordinates(station.code)
                                if (fromCoords != null && toCoords != null) {
                                    mapViewModel.animateToRoute(fromCoords, toCoords)
                                }
                                onNavigateToTrains(station.code)
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
                            if (station.code == primaryDestinationCode) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = "Primary destination station",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
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

                // Show message if no stations found
                if (availableStations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchText.isNotBlank()) {
                                    "No stations found matching \"$searchText\""
                                } else {
                                    "No stations available"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Preview removed - requires MapContainerViewModel which cannot be instantiated in preview