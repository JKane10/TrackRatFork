package com.trackrat.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trackrat.android.navigation.TrackRatDestinations
import com.trackrat.android.navigation.createTrackRatNavigator
import com.trackrat.android.ui.advanced.AdvancedConfigScreen
import com.trackrat.android.ui.favorites.FavoriteStationsScreen
import com.trackrat.android.ui.main.MainViewModel
import com.trackrat.android.ui.onboarding.OnboardingScreen
import com.trackrat.android.ui.profile.ProfileScreen
import com.trackrat.android.ui.theme.TrackRatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle deep link from intent
        handleDeepLink(intent)

        setContent {
            TrackRatTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TrackRatAppNavHost(deepLinkUri = deepLinkUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            deepLinkUri = intent.data
        }
    }
}

@Composable
fun TrackRatAppNavHost(deepLinkUri: Uri? = null) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val onboardingCompleted by mainViewModel.onboardingCompleted.collectAsState()
    val navController = rememberNavController()

    // Wait for the first DataStore emission before rendering the NavHost.
    // This is imperceptible in practice since DataStore reads are near-instant.
    if (onboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val navigator = navController.createTrackRatNavigator()
    val startDestination = if (onboardingCompleted == true) "map_container" else TrackRatDestinations.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding (first launch only)
        composable(TrackRatDestinations.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("map_container") {
                        popUpTo(TrackRatDestinations.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Map container as root (with embedded sheet navigation)
        composable("map_container") {
            com.trackrat.android.ui.map.MapContainerScreen(
                mainNavController = navController,
                deepLinkUri = deepLinkUri
            )
        }

        // Profile screen (full-screen overlay)
        composable(TrackRatDestinations.Profile.route) {
            ProfileScreen(navigator = navigator)
        }

        // Favorite Stations screen (full-screen overlay)
        composable(TrackRatDestinations.FavoriteStations.route) {
            FavoriteStationsScreen(navigator = navigator)
        }

        // Advanced Configuration screen (full-screen overlay)
        composable(TrackRatDestinations.AdvancedConfig.route) {
            AdvancedConfigScreen(navigator = navigator)
        }
    }
}
