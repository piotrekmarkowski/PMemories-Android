package com.piotrmarkowski.pmemories.ui.travel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piotrmarkowski.pmemories.data.TripWithStops
import com.piotrmarkowski.pmemories.travel.ExplorerScore
import com.piotrmarkowski.pmemories.travel.TravelAchievementsCalculator
import com.piotrmarkowski.pmemories.travel.TravelViewModel

private enum class TravelSegment { TRIPS, GLOBE }

/**
 * Top-level Travel tab — segmented control (Trips / Globe) mirroring iOS
 * `TravelSegment` in `TravelMapView.swift` (21.08.2026: a real, labeled
 * segmented control, not a small icon in the toolbar — same reasoning
 * applies here, this is a first-class feature not a secondary action).
 */
@Composable
fun TravelScreen(
    onOpenTrip: (String) -> Unit,
    onNewTrip: () -> Unit,
    onOpenRanking: () -> Unit,
    onOpenPoster: () -> Unit,
    viewModel: TravelViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var segment by remember { mutableStateOf(TravelSegment.TRIPS) }

    Scaffold(
        floatingActionButton = {
            if (segment == TravelSegment.TRIPS) {
                FloatingActionButton(onClick = onNewTrip) {
                    Icon(Icons.Filled.Add, contentDescription = "New Trip")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SegmentedButton(
                    selected = segment == TravelSegment.TRIPS,
                    onClick = { segment = TravelSegment.TRIPS },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Trips") }
                SegmentedButton(
                    selected = segment == TravelSegment.GLOBE,
                    onClick = { segment = TravelSegment.GLOBE },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Globe") }
            }

            val score = remember(state.trips) { TravelAchievementsCalculator.explorerScore(state.trips) }
            ExplorerScoreCard(score = score, onClick = onOpenRanking)

            if (state.trips.isNotEmpty()) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onOpenPoster,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) { Text("My Travel Journey Poster") }
                Spacer(Modifier.height(8.dp))
            }

            when (segment) {
                TravelSegment.TRIPS -> TripsList(
                    trips = state.trips,
                    onOpenTrip = onOpenTrip,
                    onDelete = viewModel::deleteTrip,
                    onToggleFavorite = viewModel::toggleFavorite
                )
                TravelSegment.GLOBE -> WorldGlobeMap(
                    allStops = state.trips.flatMap { it.stops },
                    onStopTap = { stop -> stop.linkedProjectId?.let { /* TODO: navigate to Library once cross-nav is wired */ } },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ExplorerScoreCard(score: ExplorerScore, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Button(onClick = onClick) {
            Text("Explorer Score: ${score.total.toInt()} · ${score.tierName}")
        }
    }
}

@Composable
private fun TripsList(
    trips: List<TripWithStops>,
    onOpenTrip: (String) -> Unit,
    onDelete: (TripWithStops) -> Unit,
    onToggleFavorite: (TripWithStops) -> Unit
) {
    if (trips.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No trips yet — tap + to build your first route.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(trips, key = { it.trip.id }) { trip ->
            ListItem(
                headlineContent = { Text(trip.trip.title) },
                supportingContent = { Text("${trip.stops.size} stops") },
                leadingContent = {
                    IconButton(onClick = { onToggleFavorite(trip) }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = if (trip.trip.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onDelete(trip) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { onOpenTrip(trip.trip.id) }
            )
        }
    }
}
