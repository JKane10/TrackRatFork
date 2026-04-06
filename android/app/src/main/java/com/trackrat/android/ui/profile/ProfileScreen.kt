package com.trackrat.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackrat.android.data.models.Station
import com.trackrat.android.navigation.TrackRatNavigator
import com.trackrat.android.ui.components.GlassmorphicSearchCard
import com.trackrat.android.ui.profile.components.ProfileActionRow
import com.trackrat.android.ui.profile.components.ProfileSectionCard

/**
 * Profile screen showing settings and support options
 * Matches iOS MyProfileView design
 */
private enum class EditingStation { HOME, WORK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navigator: TrackRatNavigator
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryDeparture by viewModel.primaryDepartureStation.collectAsState()
    val primaryDestination by viewModel.primaryDestinationStation.collectAsState()

    var editingStation by remember { mutableStateOf<EditingStation?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Favorite Stations Section
            item {
                ProfileSectionCard(title = "Favorite Stations") {
                    PrimaryStationRow(
                        icon = Icons.Default.Home,
                        station = primaryDeparture,
                        onTap = { editingStation = EditingStation.HOME },
                        onClear = { viewModel.setPrimaryDeparture(null) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    PrimaryStationRow(
                        icon = Icons.Default.Work,
                        station = primaryDestination,
                        onTap = { editingStation = EditingStation.WORK },
                        onClear = { viewModel.setPrimaryDestination(null) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigator.navigateToFavoriteStations() }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Add Favorite Station",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFFF6B35)
                        )
                    }
                }
            }

            // Support Section
            item {
                ProfileSectionCard(title = "Support") {
                    ProfileActionRow(
                        icon = Icons.Default.Info,
                        text = "Report Issues",
                        subtitle = "Send new ideas too!",
                        isExternalLink = true,
                        onClick = { viewModel.openSignalLink() }
                    )
                }
            }

            // Community Section
            item {
                ProfileSectionCard(title = "Community") {
                    ProfileActionRow(
                        icon = Icons.Default.PlayArrow,
                        text = "YouTube Channel",
                        isExternalLink = true,
                        onClick = { viewModel.openYouTube() }
                    )
                    ProfileActionRow(
                        icon = Icons.Default.Favorite,
                        text = "Instagram",
                        isExternalLink = true,
                        onClick = { viewModel.openInstagram() }
                    )
                }
            }

            // Settings Section
            item {
                ProfileSectionCard(title = "Settings") {
                    ProfileActionRow(
                        icon = Icons.Default.Settings,
                        text = "Advanced Configuration",
                        subtitle = uiState.currentEnvironment?.name,
                        isExternalLink = false,
                        onClick = { navigator.navigateToAdvancedConfig() }
                    )
                }
            }
        }
    }

    // Station picker bottom sheet
    if (editingStation != null) {
        StationPickerSheet(
            sheetState = sheetState,
            title = if (editingStation == EditingStation.HOME) "Set Home Station" else "Set Work Station",
            onStationSelected = { station ->
                if (editingStation == EditingStation.HOME)
                    viewModel.setPrimaryDeparture(station.code)
                else
                    viewModel.setPrimaryDestination(station.code)
                editingStation = null
            },
            onDismiss = { editingStation = null },
            viewModel = viewModel
        )
    }
}

@Composable
private fun PrimaryStationRow(
    icon: ImageVector,
    station: Station?,
    onTap: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (station != null) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = station?.name ?: "Not set",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (station != null) FontWeight.Medium else FontWeight.Normal,
            color = if (station != null)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (station != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationPickerSheet(
    sheetState: SheetState,
    title: String,
    onStationSelected: (Station) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel
) {
    var searchText by remember { mutableStateOf("") }
    var stations by remember { mutableStateOf(viewModel.searchStations("")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            GlassmorphicSearchCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { query ->
                        searchText = query
                        stations = viewModel.searchStations(query.trim())
                    },
                    placeholder = {
                        Text(
                            "Search stations",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = if (searchText.isNotBlank()) {
                        {
                            IconButton(onClick = {
                                searchText = ""
                                stations = viewModel.searchStations("")
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(stations) { station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onStationSelected(station) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
