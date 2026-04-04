package com.trackrat.android.ui.onboarding

import androidx.lifecycle.ViewModel
import com.trackrat.android.data.models.Station
import com.trackrat.android.data.models.Stations
import com.trackrat.android.data.models.TransitSystem
import com.trackrat.android.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the onboarding flow.
 * Manages transit system selection, primary departure/destination station selection,
 * and persisting those choices to DataStore.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _selectedSystem = MutableStateFlow<TransitSystem?>(null)
    val selectedSystem: StateFlow<TransitSystem?> = _selectedSystem.asStateFlow()

    private val _primaryDeparture = MutableStateFlow<Station?>(null)
    val primaryDeparture: StateFlow<Station?> = _primaryDeparture.asStateFlow()

    private val _primaryDestination = MutableStateFlow<Station?>(null)
    val primaryDestination: StateFlow<Station?> = _primaryDestination.asStateFlow()

    private val _departureSearchResults = MutableStateFlow<List<Station>>(emptyList())
    val departureSearchResults: StateFlow<List<Station>> = _departureSearchResults.asStateFlow()

    private val _destinationSearchResults = MutableStateFlow<List<Station>>(emptyList())
    val destinationSearchResults: StateFlow<List<Station>> = _destinationSearchResults.asStateFlow()

    fun selectSystem(system: TransitSystem) {
        _selectedSystem.value = system
    }

    fun selectPrimaryDeparture(station: Station) {
        _primaryDeparture.value = station
        _departureSearchResults.value = emptyList()
    }

    fun clearPrimaryDeparture() {
        _primaryDeparture.value = null
    }

    fun selectPrimaryDestination(station: Station) {
        _primaryDestination.value = station
        _destinationSearchResults.value = emptyList()
    }

    fun clearPrimaryDestination() {
        _primaryDestination.value = null
    }

    fun searchDepartureStations(query: String) {
        _departureSearchResults.value = if (query.isBlank()) emptyList() else Stations.search(query)
    }

    fun searchDestinationStations(query: String) {
        _destinationSearchResults.value = if (query.isBlank()) emptyList() else Stations.search(query)
    }

    /**
     * Persists all selections and marks onboarding as complete.
     * Must be called from a coroutine; does not launch internally.
     */
    suspend fun completeOnboarding() {
        preferences.setPreferredSystem(_selectedSystem.value?.code)
        preferences.setPrimaryDepartureStation(_primaryDeparture.value?.code)
        preferences.setPrimaryDestinationStation(_primaryDestination.value?.code)
        preferences.setOnboardingCompleted(true)
    }
}
