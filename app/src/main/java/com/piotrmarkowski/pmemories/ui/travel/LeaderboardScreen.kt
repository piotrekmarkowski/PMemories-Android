package com.piotrmarkowski.pmemories.ui.travel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piotrmarkowski.pmemories.leaderboard.LeaderboardEntry
import com.piotrmarkowski.pmemories.leaderboard.LeaderboardService
import com.piotrmarkowski.pmemories.travel.TravelAchievementsCalculator
import com.piotrmarkowski.pmemories.travel.TravelViewModel

/**
 * First screen that actually CALLS `LeaderboardService` on Android — Etap 4
 * built the service, Etap 7 is what finally gives it real, non-zero data to
 * submit (same dependency iOS's own Leaderboard had on Travel Map from the
 * start). Submits on open (mirrors iOS `LeaderboardView.task`), then loads
 * the top list.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun LeaderboardScreen(onBack: () -> Unit, viewModel: TravelViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.trips) {
        isLoading = true
        try {
            val score = TravelAchievementsCalculator.explorerScore(state.trips)
            LeaderboardService.submitCurrentScore(
                score = score.total,
                km = score.totalKm,
                countries = TravelAchievementsCalculator.rawCountries(state.trips),
                cities = TravelAchievementsCalculator.rawCities(state.trips),
                elevationM = TravelAchievementsCalculator.rawElevationGainMeters(state.trips),
                avatarFrame = "none"
            )
            entries = LeaderboardService.topEntries()
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Ranking") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    errorMessage ?: "",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                entries.isEmpty() -> Text("No entries yet — be the first!", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn {
                    items(entries, key = { it.userId }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.displayName) },
                            supportingContent = { Text("${entry.countries} countries · ${entry.cities} cities") },
                            trailingContent = { Text("${entry.score.toInt()}") }
                        )
                    }
                }
            }
        }
    }
}
