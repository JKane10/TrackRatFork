package com.trackrat.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackrat.android.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel scoped to the root NavHost.
 * Provides the onboarding-completed state used to determine the app's start destination.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository
) : ViewModel() {

    /**
     * Null while preferences are still loading; true/false once known.
     */
    val onboardingCompleted: StateFlow<Boolean?> = preferences.userPreferencesFlow
        .map { it.onboardingCompleted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
}
