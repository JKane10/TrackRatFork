package com.trackrat.android.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackrat.android.data.models.Station
import com.trackrat.android.data.models.Stations
import com.trackrat.android.data.models.TransitSystem
import com.trackrat.android.ui.components.GlassmorphicCard
import com.trackrat.android.ui.components.GlassmorphicSearchCard
import kotlinx.coroutines.launch

/**
 * Onboarding flow shown to first-time users.
 *
 * Page 0 – Transit system selection
 * Page 1 – Primary departure station
 * Page 2 – Primary destination station
 *
 * Selections are persisted via [OnboardingViewModel.completeOnboarding] when the
 * user taps "Get Started" on the final page, or "Skip" to save without selections.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val selectedSystem by viewModel.selectedSystem.collectAsState()
    val primaryDeparture by viewModel.primaryDeparture.collectAsState()
    val primaryDestination by viewModel.primaryDestination.collectAsState()
    val departureSearchResults by viewModel.departureSearchResults.collectAsState()
    val destinationSearchResults by viewModel.destinationSearchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Welcome to TrackRat",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (pagerState.currentPage > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> TransitSystemPage(
                        selectedSystem = selectedSystem,
                        onSystemSelected = { system ->
                            viewModel.selectSystem(system)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> StationPickerPage(
                        icon = Icons.Default.Home,
                        title = "Primary Departure Station",
                        subtitle = "Where do you typically start your journey?",
                        selectedStation = primaryDeparture,
                        searchResults = departureSearchResults,
                        defaultStations = Stations.DEPARTURE_STATIONS,
                        excludeStation = null,
                        onStationSelected = { viewModel.selectPrimaryDeparture(it) },
                        onStationCleared = { viewModel.clearPrimaryDeparture() },
                        onSearch = { viewModel.searchDepartureStations(it) }
                    )
                    2 -> StationPickerPage(
                        icon = Icons.Default.Work,
                        title = "Primary Destination Station",
                        subtitle = "Where do you typically travel to?",
                        selectedStation = primaryDestination,
                        searchResults = destinationSearchResults,
                        defaultStations = Stations.ALL_STATIONS,
                        excludeStation = primaryDeparture,
                        onStationSelected = { viewModel.selectPrimaryDestination(it) },
                        onStationCleared = { viewModel.clearPrimaryDestination() },
                        onSearch = { viewModel.searchDestinationStations(it) }
                    )
                }
            }

            // Bottom navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (index == pagerState.currentPage)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            scope.launch {
                                viewModel.completeOnboarding()
                                onComplete()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (pagerState.currentPage < 2) Icons.Default.ArrowForward else Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (pagerState.currentPage) {
                            0 -> if (selectedSystem != null) "Next" else "Skip"
                            1 -> if (primaryDeparture != null) "Next" else "Skip"
                            else -> if (primaryDestination != null) "Get Started" else "Skip"
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Page 0 – Transit system selection
// ---------------------------------------------------------------------------

@Composable
private fun TransitSystemPage(
    selectedSystem: TransitSystem?,
    onSystemSelected: (TransitSystem) -> Unit
) {
    var showAdditional by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Which transit system\ndo you use the most?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "You can always change this later",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(TransitSystem.mainSystems) { system ->
                SystemCard(
                    system = system,
                    isSelected = system == selectedSystem,
                    onClick = { onSystemSelected(system) }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdditional = !showAdditional }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (showAdditional) "▲  Hide Additional Systems" else "▼  Additional Systems",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "beta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (showAdditional) {
                items(TransitSystem.betaSystems) { system ->
                    SystemCard(
                        system = system,
                        isSelected = system == selectedSystem,
                        onClick = { onSystemSelected(system) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemCard(
    system: TransitSystem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = system.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pages 1 & 2 – Station picker (reused for departure and destination)
// ---------------------------------------------------------------------------

@Composable
private fun StationPickerPage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selectedStation: Station?,
    searchResults: List<Station>,
    defaultStations: List<Station>,
    excludeStation: Station?,
    onStationSelected: (Station) -> Unit,
    onStationCleared: () -> Unit,
    onSearch: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    val displayedStations = remember(searchResults, defaultStations, excludeStation, selectedStation) {
        val base = if (searchResults.isNotEmpty()) searchResults else defaultStations
        base.filter { it.code != excludeStation?.code }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Selected station chip
        AnimatedVisibility(visible = selectedStation != null) {
            selectedStation?.let { station ->
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = station.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    onStationCleared()
                                    searchText = ""
                                    onSearch("")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear selection",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search bar
        GlassmorphicSearchCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { query ->
                    searchText = query
                    onSearch(query.trim())
                },
                placeholder = {
                    Text(
                        text = "Search stations",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = if (searchText.isNotBlank()) {
                    {
                        IconButton(onClick = {
                            searchText = ""
                            onSearch("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else null,
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

        Spacer(Modifier.height(8.dp))

        // Station list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(displayedStations, key = { it.code }) { station ->
                val isSelected = station.code == selectedStation?.code
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStationSelected(station) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = station.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
