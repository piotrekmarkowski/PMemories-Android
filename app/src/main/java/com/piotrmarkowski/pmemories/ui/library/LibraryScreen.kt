package com.piotrmarkowski.pmemories.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piotrmarkowski.pmemories.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.piotrmarkowski.pmemories.data.ProjectEntity
import com.piotrmarkowski.pmemories.data.ProjectWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Library" tab — Etap 3, Android analog of iOS `LibraryView`: saved Memory
 * projects grouped by the year of their REAL trip date (earliest photo/video
 * capture date, `LibraryViewModel.groupedProjects`), each with Rename / Edit
 * Date / Delete, and playback of the exported video when one exists
 * (`ProjectEntity.exportedMediaUri`). Opening a saved project back into the
 * Studio editor is its own, not-yet-built feature — `StudioViewModel` only
 * knows how to create a brand new project right now — so rows here are
 * view/manage only, the same scope as the iOS grouping+date-edit work item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(modifier: Modifier = Modifier, viewModel: LibraryViewModel = viewModel()) {
    val groups by viewModel.groupedProjects.collectAsState()

    var renamingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var editingDateProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var editingDateInitialMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var playingUri by remember { mutableStateOf<String?>(null) }

    if (groups.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.library_empty_state),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item {
                Text(
                    stringResource(R.string.library),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            groups.forEach { group ->
                item(key = "year-${group.year}") {
                    Text(
                        group.year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(group.projects, key = { it.project.id }) { details ->
                    ProjectRow(
                        details = details,
                        effectiveDateMillis = viewModel.effectiveTripDateMillis(details),
                        onPlay = { uri -> playingUri = uri },
                        onRename = {
                            renameText = details.project.title
                            renamingProject = details.project
                        },
                        onEditDate = {
                            editingDateInitialMillis = viewModel.effectiveTripDateMillis(details)
                            editingDateProject = details.project
                        },
                        onDelete = { viewModel.delete(details.project) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    renamingProject?.let { project ->
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rename(project, renameText)
                    renamingProject = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    editingDateProject?.let { project ->
        // `key(project.id)` forces a fresh `DatePickerState` per project —
        // without it, re-opening this dialog for a DIFFERENT project while
        // Compose still remembers the previous state (same call site)
        // would show the LAST-edited project's date instead of this one's.
        androidx.compose.runtime.key(project.id) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editingDateInitialMillis)
            DatePickerDialog(
                onDismissRequest = { editingDateProject = null },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.updateManualTripDate(project, it) }
                        editingDateProject = null
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    TextButton(onClick = { editingDateProject = null }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    playingUri?.let { uri ->
        FullScreenVideoPlayer(uri = uri, onDismiss = { playingUri = null })
    }
}

@Composable
private fun ProjectRow(
    details: ProjectWithDetails,
    effectiveDateMillis: Long,
    onPlay: (String) -> Unit,
    onRename: () -> Unit,
    onEditDate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val exportedUri = details.project.exportedMediaUri

    ListItem(
        headlineContent = { Text(details.project.title.ifBlank { stringResource(R.string.untitled) }) },
        supportingContent = {
            val itemsLabel = pluralStringResource(R.plurals.items_count, details.items.size, details.items.size)
            Text("$itemsLabel • ${dateFormatter.format(Date(effectiveDateMillis))}")
        },
        leadingContent = if (exportedUri != null) {
            {
                IconButton(onClick = { onPlay(exportedUri) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.play))
                }
            }
        } else null,
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_date)) },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        onClick = { menuExpanded = false; onEditDate() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    )
}

/**
 * Full-screen playback for a project's exported video — Android analog of
 * iOS `LibraryView.FullScreenVideoPlayer` (`AVPlayer`/`VideoPlayer`). Plain
 * `Dialog` sized to fill the screen rather than a dedicated Activity/nav
 * route — same reasoning as iOS choosing `.fullScreenCover` over `.sheet`:
 * no partial-height chrome, just the video and a close button.
 */
@Composable
private fun FullScreenVideoPlayer(uri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { PlayerView(context).apply { this.player = player } }
            )
            IconButton(onClick = onDismiss, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
