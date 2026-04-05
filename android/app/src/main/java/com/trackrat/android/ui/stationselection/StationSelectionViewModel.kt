package com.trackrat.android.ui.stationselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackrat.android.data.models.Station
import com.trackrat.android.data.models.Stations
import com.trackrat.android.data.preferences.UserPreferencesRepository
import com.trackrat.android.data.repository.TrackRatRepository
import com.trackrat.android.services.RatSenseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for station selection screens
 */
@HiltViewModel
class StationSelectionViewModel @Inject constructor(
    private val repository: TrackRatRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ratSenseService: RatSenseService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        // SavedState keys for state restoration
        private const val KEY_SELECTED_ORIGIN = "selected_origin_code"
        private const val KEY_SELECTED_DESTINATION = "selected_destination_code"
        private const val KEY_SEARCH_QUERY = "search_query"
    }

    // RatSense AI suggestions
    private val _ratSenseSuggestions = MutableStateFlow<List<RatSenseService.JourneySuggestion>>(emptyList())
    val ratSenseSuggestions: StateFlow<List<RatSenseService.JourneySuggestion>> = _ratSenseSuggestions.asStateFlow()

    // Search results - shared between departure and destination searches
    private val _searchResults = MutableStateFlow<List<Station>>(emptyList())

    // Selected stations
    private val _selectedOrigin = MutableStateFlow<Station?>(null)
    val selectedOrigin: StateFlow<Station?> = _selectedOrigin.asStateFlow()

    private val _selectedDestination = MutableStateFlow<Station?>(null)
    val selectedDestination: StateFlow<Station?> = _selectedDestination.asStateFlow()

    // User preferences including favorite stations
    private val userPreferences = userPreferencesRepository.userPreferencesFlow

    val primaryDepartureCode: StateFlow<String?> = userPreferences
        .map { it.primaryDepartureStation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val primaryDestinationCode: StateFlow<String?> = userPreferences
        .map { it.primaryDestinationStation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Stations to display on the departure selection screen.
     * Order (when not searching):
     * 1. Primary departure station set during onboarding (if any)
     * 2. Favorited departure stations (or all departure stations if none favorited)
     * When searching: search results only.
     */
    val displayedDepartureStations: StateFlow<List<Station>> = combine(
        userPreferences,
        _searchResults
    ) { prefs, searchResults ->
        when {
            searchResults.isNotEmpty() -> searchResults
            else -> {
                val favoriteStationCodes = prefs.favoriteStations
                val baseList = if (favoriteStationCodes.isEmpty()) {
                    Stations.DEPARTURE_STATIONS
                } else {
                    Stations.DEPARTURE_STATIONS.filter { it.code in favoriteStationCodes }
                }
                // Prepend primary departure station, ensuring it isn't duplicated
                val primaryCode = prefs.primaryDepartureStation
                val primaryStation = primaryCode?.let { Stations.getStation(it) }
                if (primaryStation != null) {
                    listOf(primaryStation) + baseList.filter { it.code != primaryStation.code }
                } else {
                    baseList
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Stations.DEPARTURE_STATIONS
    )

    /**
     * Stations to display on the destination selection screen.
     * Order (when not searching):
     * 1. Primary destination station set during onboarding (if any)
     * 2. Favorited stations (or all stations if none favorited)
     * When searching: search results only.
     */
    val displayedDestinationStations: StateFlow<List<Station>> = combine(
        userPreferences,
        _searchResults
    ) { prefs, searchResults ->
        when {
            searchResults.isNotEmpty() -> searchResults
            else -> {
                val favoriteStationCodes = prefs.favoriteStations
                val baseList = if (favoriteStationCodes.isEmpty()) {
                    Stations.ALL_STATIONS
                } else {
                    Stations.ALL_STATIONS.filter { it.code in favoriteStationCodes }
                }
                // Prepend primary destination station, ensuring it isn't duplicated
                val primaryCode = prefs.primaryDestinationStation
                val primaryStation = primaryCode?.let { Stations.getStation(it) }
                if (primaryStation != null) {
                    listOf(primaryStation) + baseList.filter { it.code != primaryStation.code }
                } else {
                    baseList
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Stations.ALL_STATIONS
    )

    init {
        // Restore state from SavedStateHandle if available
        restoreState()

        // Load RatSense AI suggestions
        loadRatSenseSuggestions()
    }

    /**
     * Load RatSense AI journey suggestions
     */
    fun loadRatSenseSuggestions() {
        viewModelScope.launch {
            _ratSenseSuggestions.value = ratSenseService.getSuggestions()
        }
    }
    
    /**
     * Select origin station
     */
    fun selectOrigin(station: Station) {
        _selectedOrigin.value = station
        savedStateHandle[KEY_SELECTED_ORIGIN] = station.code
    }

    /**
     * Select destination station
     */
    fun selectDestination(station: Station) {
        _selectedDestination.value = station
        savedStateHandle[KEY_SELECTED_DESTINATION] = station.code
    }

    /**
     * Search stations by query.
     * Updates search results which are used by both departure and destination screens.
     */
    fun searchStations(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        _searchResults.value = if (query.isBlank()) {
            emptyList()
        } else {
            Stations.search(query)
        }
    }

    /**
     * Search destinations - alias for searchStations for clarity in destination screen.
     */
    fun searchDestinations(query: String) = searchStations(query)
    
    /**
     * Clear selections and search results
     */
    fun clearSelections() {
        _selectedOrigin.value = null
        _selectedDestination.value = null
        _searchResults.value = emptyList()

        // Clear saved state
        savedStateHandle.remove<String>(KEY_SELECTED_ORIGIN)
        savedStateHandle.remove<String>(KEY_SELECTED_DESTINATION)
        savedStateHandle.remove<String>(KEY_SEARCH_QUERY)
    }
    
    /**
     * Toggle favorite status for a station
     */
    suspend fun toggleFavoriteStation(stationCode: String) {
        userPreferencesRepository.toggleFavoriteStation(stationCode)
    }
    
    /**
     * Check if a station is favorited.
     * Returns a Flow that emits true/false as favorite status changes.
     */
    fun isStationFavorited(stationCode: String): Flow<Boolean> {
        return userPreferences.map { prefs ->
            stationCode in prefs.favoriteStations
        }
    }
    
    /**
     * Restore state from SavedStateHandle
     */
    private fun restoreState() {
        val originCode = savedStateHandle.get<String>(KEY_SELECTED_ORIGIN)
        val destinationCode = savedStateHandle.get<String>(KEY_SELECTED_DESTINATION)
        val searchQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY)
        
        // Restore selected origin
        originCode?.let { code ->
            val station = Stations.ALL_STATIONS.find { it.code == code }
            _selectedOrigin.value = station
        }
        
        // Restore selected destination
        destinationCode?.let { code ->
            val station = Stations.ALL_STATIONS.find { it.code == code }
            _selectedDestination.value = station
        }
        
        // Restore search query and results
        searchQuery?.let { query ->
            if (query.isNotBlank()) {
                _searchResults.value = Stations.search(query)
            }
        }
    }
}