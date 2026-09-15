package com.piotrmarkowski.pmemories.ui.studio

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piotrmarkowski.pmemories.R
import com.piotrmarkowski.pmemories.studio.ColorStyle
import com.piotrmarkowski.pmemories.studio.ExportBitrate
import com.piotrmarkowski.pmemories.studio.ExportResolution
import com.piotrmarkowski.pmemories.studio.ExportState
import com.piotrmarkowski.pmemories.studio.StudioViewModel
import com.piotrmarkowski.pmemories.studio.TransitionStyle
import java.io.File

/**
 * Single scrollable `LazyColumn` for the whole screen (header controls as
 * regular items, then the clip list, then export/status) — a previous
 * version split a fixed header `Column` from a nested `LazyColumn` for the
 * clips, which starved the clip list of vertical space once filters/
 * resolution/caption controls were added (clips existed — confirmed via
 * ViewModel logging — they just had no room to render). One scrolling
 * list avoids that entirely.
 */
@Composable
fun StudioScreen(modifier: Modifier = Modifier, viewModel: StudioViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) viewModel.addMedia(uris) }

    var captionText by remember { mutableStateOf("") }

    // 25.08.2026 — drag&drop reorder state: zwykłe `mutableStateOf`/
    // `mutableStateMapOf` hoisted tutaj (nie w `LazyListState`), bo
    // reorder liczony jest po WYSOKOŚCI WŁASNEGO wiersza, nie po
    // absolutnych indeksach listy — patrz komentarz przy `items(state.items...)`.
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text(
                "Studio",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    pickMedia.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                }) { Text(stringResource(R.string.add_photos_videos)) }

                OutlinedButton(onClick = { viewModel.debugSeedFromMediaStore("PMemoriesTest") }) {
                    Text("Debug: seed zdjęcia")
                }
                OutlinedButton(onClick = { viewModel.debugSeedVideoAndMusic("PMemoriesTest") }) {
                    Text("Debug: seed wideo+muzyka")
                }
                OutlinedButton(onClick = { viewModel.addDebugPipOverlay("PMemoriesTest") }) {
                    Text("Debug: seed PiP")
                }
            }
        }

        if (state.overlays.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.overlays.forEach { overlay ->
                        OutlinedButton(onClick = { viewModel.removeOverlay(overlay.id) }) {
                            Text("✕ PiP ${overlay.cornerRawValue} ${"%.1f".format(overlay.globalStartTime)}-${"%.1f".format(overlay.globalStartTime + overlay.duration)}s")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ColorStyle.entries.forEach { style ->
                    val label = when (style) {
                        ColorStyle.NONE -> "No filter"
                        ColorStyle.VINTAGE -> "Vintage"
                        ColorStyle.BLACK_AND_WHITE -> "B&W"
                        ColorStyle.VIBRANT -> "Vibrant"
                        ColorStyle.CINEMATIC -> "Cinematic"
                        ColorStyle.WARM -> "Warm"
                    }
                    if (state.colorStyle == style) {
                        Button(onClick = { }) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setColorStyle(style) }) { Text(label) }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExportResolution.entries.forEach { resolution ->
                    if (state.exportResolution == resolution) {
                        Button(onClick = { }) { Text(resolution.label) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setExportResolution(resolution) }) { Text(resolution.label) }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExportBitrate.entries.forEach { bitrate ->
                    if (state.exportBitrate == bitrate) {
                        Button(onClick = { }) { Text(bitrate.label) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setExportBitrate(bitrate) }) { Text(bitrate.label) }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.caption_time_range)) },
                    singleLine = true
                )
                Button(onClick = {
                    if (captionText.isNotBlank()) {
                        viewModel.addDebugCaption(captionText)
                        captionText = ""
                    }
                }) { Text(stringResource(R.string.add)) }
            }
        }

        if (state.captions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.captions.forEach { caption ->
                        OutlinedButton(onClick = { viewModel.removeCaption(caption.id) }) {
                            Text("✕ ${caption.text}")
                        }
                    }
                }
            }
        }

        if (state.items.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.studio_empty_state),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(state.items, key = { it.id }) { item ->
                val index = state.items.indexOf(item)
                val isDragged = item.id == draggedItemId
                val elevation by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (isDragged) 8.dp else 0.dp, label = "dragElevation"
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .onSizeChanged { itemHeights[item.id] = it.height }
                        .graphicsLayer { translationY = if (isDragged) dragOffsetPx else 0f }
                        .zIndex(if (isDragged) 1f else 0f)
                        .alpha(if (isDragged) 0.9f else 1f),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = elevation)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 25.08.2026 — uchwyt do przeciągania (długi
                            // przycisk + przeciągnięcie w pionie), zastępuje
                            // przyciski góra/dół. Lokalne przesunięcie po
                            // wysokości WŁASNEGO wiersza (zmierzonej przez
                            // `onSizeChanged`) zamiast śledzenia absolutnych
                            // indeksów `LazyListState` — prostsze i wystarcza,
                            // bo lista klipów to i tak jedyna przeciągalna
                            // sekcja w tym ekranie (reszta to nagłówek).
                            Text(
                                "☰",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .pointerInput(item.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedItemId = item.id
                                                dragOffsetPx = 0f
                                            },
                                            onDragEnd = {
                                                draggedItemId = null
                                                dragOffsetPx = 0f
                                            },
                                            onDragCancel = {
                                                draggedItemId = null
                                                dragOffsetPx = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetPx += dragAmount.y
                                                val rowHeight = itemHeights[item.id] ?: return@detectDragGesturesAfterLongPress
                                                val currentIndex = state.items.indexOfFirst { it.id == item.id }
                                                if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                                if (dragOffsetPx > rowHeight / 2f && currentIndex < state.items.lastIndex) {
                                                    viewModel.moveItem(currentIndex, currentIndex + 1)
                                                    dragOffsetPx -= rowHeight
                                                } else if (dragOffsetPx < -rowHeight / 2f && currentIndex > 0) {
                                                    viewModel.moveItem(currentIndex, currentIndex - 1)
                                                    dragOffsetPx += rowHeight
                                                }
                                            }
                                        )
                                    }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (item.isVideo) stringResource(R.string.video_numbered, index + 1)
                                    else stringResource(R.string.photo_numbered, index + 1)
                                )
                                Text(
                                    "${"%.1f".format(item.duration)}s · speed ${"%.2f".format(item.speed)}x · rot ${item.rotationDegrees}° · vol ${"%.2f".format(item.originalVolume)}x",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    viewModel.updateItem(item.id) { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
                                }) { Text("↻") }
                                IconButton(onClick = { viewModel.removeItem(item.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove))
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(onClick = {
                                viewModel.updateItem(item.id) {
                                    it.copy(speed = (it.speed - 0.25).coerceIn(0.25, 3.0))
                                }
                            }) { Text("Speed −") }
                            OutlinedButton(onClick = {
                                viewModel.updateItem(item.id) {
                                    it.copy(speed = (it.speed + 0.25).coerceIn(0.25, 3.0))
                                }
                            }) { Text("Speed +") }
                            OutlinedButton(onClick = {
                                viewModel.updateItem(item.id) {
                                    it.copy(duration = (it.duration - 0.5).coerceAtLeast(0.5))
                                }
                            }) { Text("Dur −") }
                            OutlinedButton(onClick = {
                                viewModel.updateItem(item.id) {
                                    it.copy(duration = it.duration + 0.5)
                                }
                            }) { Text("Dur +") }
                            if (item.isVideo) {
                                OutlinedButton(onClick = {
                                    viewModel.updateItem(item.id) {
                                        it.copy(trimStart = (it.trimStart - 0.5).coerceAtLeast(0.0))
                                    }
                                }) { Text("Trim −") }
                                OutlinedButton(onClick = {
                                    viewModel.updateItem(item.id) {
                                        it.copy(trimStart = it.trimStart + 0.5)
                                    }
                                }) { Text("Trim +") }
                                OutlinedButton(onClick = {
                                    viewModel.splitItem(item.id, item.duration / 2)
                                }) { Text("Split") }
                                OutlinedButton(onClick = {
                                    viewModel.updateItem(item.id) {
                                        it.copy(originalVolume = (it.originalVolume - 0.25).coerceIn(0.0, 2.0))
                                    }
                                }) { Text("Vol −") }
                                OutlinedButton(onClick = {
                                    viewModel.updateItem(item.id) {
                                        it.copy(originalVolume = (it.originalVolume + 0.25).coerceIn(0.0, 2.0))
                                    }
                                }) { Text("Vol +") }
                            }
                            OutlinedButton(onClick = {
                                viewModel.updateItem(item.id) { it.copy(cropFill = !it.cropFill) }
                            }) { Text(if (item.cropFill) "Crop: fill" else "Crop: fit") }
                            // 25.08.2026 — Etap 2 transitions: styl przejścia
                            // WCHODZĄCEGO klipu (ten sam wzorzec co iOS —
                            // przechowywane na klipie który wchodzi, nie na
                            // tym który wychodzi). Bez sensu dla pierwszego
                            // klipu (nic przed nim nie wchodzi).
                            if (index > 0) {
                                OutlinedButton(onClick = {
                                    viewModel.updateItem(item.id) {
                                        val current = TransitionStyle.fromRaw(it.transitionStyleRawValue)
                                        val entries = TransitionStyle.entries
                                        val next = if (current == null) entries.first()
                                        else entries.getOrNull(entries.indexOf(current) + 1)
                                        it.copy(transitionStyleRawValue = next?.rawValue)
                                    }
                                }) {
                                    Text("Trans: ${TransitionStyle.fromRaw(item.transitionStyleRawValue)?.name ?: "cut"}")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = state.items.isNotEmpty() && state.exportState !is ExportState.Exporting,
                    onClick = {
                        // `context.getString` (plain Context method, NOT
                        // `stringResource`) — needs the timestamp fresh at
                        // CLICK time, not once per recomposition, so it
                        // can't be hoisted out of this lambda the way
                        // `signInFailedMessage` was in `OnboardingScreen`.
                        val title = context.getString(R.string.default_project_title, System.currentTimeMillis().toString())
                        viewModel.saveProject(title = title)
                        viewModel.startExport(exportOutputFile(context))
                    }
                ) { Text(stringResource(R.string.save_and_export)) }
            }
        }

        item {
            when (val exportState = state.exportState) {
                is ExportState.Exporting -> Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.exporting))
                }
                is ExportState.Done -> Text(
                    if (exportState.galleryUri != null) stringResource(R.string.export_done_gallery, exportState.galleryUri)
                    else stringResource(R.string.export_done_path, exportState.path),
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                is ExportState.Error -> Text(
                    stringResource(R.string.export_error, exportState.message),
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    color = MaterialTheme.colorScheme.error
                )
                ExportState.Idle -> Unit
            }
        }
    }
}

private fun exportOutputFile(context: Context): File {
    val dir = context.getExternalFilesDir("exports") ?: context.filesDir
    dir.mkdirs()
    return File(dir, "pmemories_export_${System.currentTimeMillis()}.mp4")
}
