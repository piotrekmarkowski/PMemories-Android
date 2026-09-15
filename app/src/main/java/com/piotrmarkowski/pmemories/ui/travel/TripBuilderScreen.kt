package com.piotrmarkowski.pmemories.ui.travel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piotrmarkowski.pmemories.travel.CityResult
import com.piotrmarkowski.pmemories.travel.CitySearchProvider
import com.piotrmarkowski.pmemories.travel.DraftStop
import com.piotrmarkowski.pmemories.travel.TransportMode
import com.piotrmarkowski.pmemories.travel.TravelViewModel
import kotlinx.coroutines.launch

/**
 * Building/editing a single trip — Android analog of iOS `TravelMapView`'s
 * stop-editing sheet. Two ways to populate `draftStops`: manual city search
 * (`CitySearchProvider`, on-submit not live-typing — see that file's own
 * doc for why) or Smart Route (`SmartRouteDetector`, from a photo
 * selection or the whole library).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripBuilderScreen(onDone: () -> Unit, viewModel: TravelViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var titleInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var wasSaving by remember { mutableStateOf(false) }

    // Navigate back once a save that was IN PROGRESS finishes successfully
    // (`draftStops` only clears on the success path inside `saveTrip` — see
    // its doc — so this can't fire after a failed save).
    androidx.compose.runtime.LaunchedEffect(state.isSaving, state.draftStops) {
        if (wasSaving && !state.isSaving && state.draftStops.isEmpty()) onDone()
        wasSaving = state.isSaving
    }

    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) viewModel.detectFromPhotos(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build Route") },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTrip(titleInput) },
                        enabled = state.draftStops.isNotEmpty() && !state.isSaving
                    ) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        else Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Trip title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickPhotos.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) { Text("Detect from photos") }
                OutlinedButton(onClick = { viewModel.detectFromAllPhotos() }) { Text("Detect from all photos") }
            }
            if (state.isDetecting) {
                Spacer(Modifier.height(8.dp))
                Row {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Scanning photo locations…")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Add a stop manually", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("City name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    isSearching = true
                    scope.launch {
                        searchResults = CitySearchProvider.search(context, searchQuery)
                        isSearching = false
                    }
                },
                enabled = searchQuery.isNotBlank() && !isSearching
            ) { Text("Search") }

            searchResults.forEach { result ->
                ListItem(
                    headlineContent = { Text(result.cityName) },
                    supportingContent = { Text(result.country ?: "") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TransportMode.entries.forEach { mode ->
                        OutlinedButton(onClick = {
                            viewModel.addManualStop(result, mode)
                            searchResults = emptyList()
                            searchQuery = ""
                        }) { Text(mode.emoji) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Stops (${state.draftStops.size})", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.draftStops, key = { it.localId }) { stop ->
                    DraftStopRow(
                        stop = stop,
                        onRemove = { viewModel.removeDraftStop(stop.localId) },
                        onTransportChange = { viewModel.updateDraftTransport(stop.localId, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftStopRow(stop: DraftStop, onRemove: () -> Unit, onTransportChange: (TransportMode) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column {
        ListItem(
            headlineContent = { Text(stop.cityName) },
            supportingContent = { Text(stop.country ?: "") },
            leadingContent = {
                Box {
                    Text(
                        stop.transport.emoji,
                        modifier = Modifier.clickable { menuExpanded = true }
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        TransportMode.entries.forEach { mode ->
                            DropdownMenuItem(text = { Text("${mode.emoji} ${mode.name}") }, onClick = {
                                onTransportChange(mode)
                                menuExpanded = false
                            })
                        }
                    }
                }
            },
            trailingContent = {
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
            }
        )
    }
}
