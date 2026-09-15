package com.piotrmarkowski.pmemories.studio

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piotrmarkowski.pmemories.data.AppDatabase
import com.piotrmarkowski.pmemories.data.CaptionEntity
import com.piotrmarkowski.pmemories.data.MediaItemEntity
import com.piotrmarkowski.pmemories.data.OverlayItemEntity
import com.piotrmarkowski.pmemories.data.ProjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.media3.transformer.Transformer
import java.io.File

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Done(val path: String, val galleryUri: Uri? = null) : ExportState
    data class Error(val message: String) : ExportState
}

/** Fixed output canvas choices — the practical "export quality control"
 * lever for Etap 2 MVP. */
enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    LANDSCAPE_720P(1280, 720, "16:9"),
    PORTRAIT_720P(720, 1280, "9:16")
}

/** Target video bitrate — wired into [androidx.media3.transformer.DefaultEncoderFactory]
 * via [androidx.media3.transformer.VideoEncoderSettings.Builder.setBitrate] in
 * [StudioExporter]. Values chosen for 720p-class output (this app's only
 * resolution tier): STANDARD matches roughly what Media3's own automatic
 * bitrate selection picks for 720p, HIGH/LOW give the user an explicit
 * quality-vs-size tradeoff instead of a fixed, invisible default. */
enum class ExportBitrate(val bps: Int, val label: String) {
    LOW(2_000_000, "Niski (mniejszy plik)"),
    STANDARD(5_000_000, "Standard"),
    HIGH(10_000_000, "Wysoki (większy plik)")
}

data class StudioUiState(
    val projectId: String = java.util.UUID.randomUUID().toString(),
    val items: List<MediaItemEntity> = emptyList(),
    val captions: List<CaptionEntity> = emptyList(),
    val overlays: List<OverlayItemEntity> = emptyList(),
    val musicUri: String? = null,
    val musicVolume: Double = 1.0,
    val colorStyle: ColorStyle = ColorStyle.NONE,
    val exportResolution: ExportResolution = ExportResolution.LANDSCAPE_720P,
    val exportBitrate: ExportBitrate = ExportBitrate.STANDARD,
    val exportState: ExportState = ExportState.Idle
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState

    private var activeTransformer: Transformer? = null

    fun addMedia(uris: List<Uri>) {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        val startOrder = _uiState.value.items.size
        val newItems = uris.mapIndexed { index, uri ->
            // Photo Picker grants persist for as long as we take the grant here;
            // best-effort — some OEM pickers/API levels don't support it, and
            // that's fine, the URI still works for this session either way.
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val mimeType = resolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video/")
            MediaItemEntity(
                projectId = _uiState.value.projectId,
                mediaUri = uri.toString(),
                isVideo = isVideo,
                duration = if (isVideo) 5.0 else 3.0,
                order = startOrder + index
            )
        }
        _uiState.update { it.copy(items = it.items + newItems) }
    }

    /** Debug seed for verifying the export pipeline without driving the
     * system Photo Picker UI — queries real MediaStore images pushed onto
     * the device under `/sdcard/Pictures/PMemoriesTest/`. Not user-facing. */
    fun debugSeedFromMediaStore(folderName: String) {
        val context = getApplication<Application>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.RELATIVE_PATH)
        val found = mutableListOf<Uri>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%$folderName%"),
            "${MediaStore.Images.Media.DATE_ADDED} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                found += Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        addMedia(found)
    }

    /** Debug seed for the video+music path (untested branch of the exporter
     * until this ran) — same idea as `debugSeedFromMediaStore`, but for
     * `MediaStore.Video` (added to the timeline) and `MediaStore.Audio`
     * (set as the project's background track). */
    fun debugSeedVideoAndMusic(folderName: String) {
        val context = getApplication<Application>()
        val resolver = context.contentResolver

        val videoProjection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.RELATIVE_PATH)
        resolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%$folderName%"),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val newItem = MediaItemEntity(
                    projectId = _uiState.value.projectId,
                    mediaUri = uri.toString(),
                    isVideo = true,
                    duration = 3.0,
                    trimStart = 0.5,
                    order = _uiState.value.items.size
                )
                _uiState.update { it.copy(items = it.items + newItem) }
            }
        }

        val audioProjection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.RELATIVE_PATH)
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioProjection,
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%$folderName%"),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                _uiState.update { it.copy(musicUri = uri.toString(), musicVolume = 0.3) }
            }
        }
    }

    fun moveItem(from: Int, to: Int) {
        _uiState.update { state ->
            val mutable = state.items.toMutableList()
            if (from !in mutable.indices || to !in mutable.indices) return@update state
            val moved = mutable.removeAt(from)
            mutable.add(to, moved)
            state.copy(items = mutable.mapIndexed { index, item -> item.copy(order = index) })
        }
    }

    fun removeItem(id: String) {
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.id == id }
                .mapIndexed { index, item -> item.copy(order = index) })
        }
    }

    fun updateItem(id: String, transform: (MediaItemEntity) -> MediaItemEntity) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) transform(it) else it })
        }
    }

    /** Splits a video clip's TRIM RANGE at `atSeconds` (relative to the
     * clip's own trimmed duration, not the absolute timeline) into two
     * consecutive items — same underlying idea as iOS Split in `Studio.md`.
     * No-op for images (nothing meaningful to split) or out-of-range cuts. */
    fun splitItem(id: String, atSeconds: Double) {
        _uiState.update { state ->
            val index = state.items.indexOfFirst { it.id == id }
            if (index == -1) return@update state
            val item = state.items[index]
            if (!item.isVideo || atSeconds <= 0.0 || atSeconds >= item.duration) return@update state

            val first = item.copy(duration = atSeconds)
            val second = item.copy(
                id = java.util.UUID.randomUUID().toString(),
                trimStart = item.trimStart + atSeconds,
                duration = item.duration - atSeconds
            )
            val mutable = state.items.toMutableList()
            mutable[index] = first
            mutable.add(index + 1, second)
            state.copy(items = mutable.mapIndexed { i, entity -> entity.copy(order = i) })
        }
    }

    fun setColorStyle(style: ColorStyle) {
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setExportResolution(resolution: ExportResolution) {
        _uiState.update { it.copy(exportResolution = resolution) }
    }

    fun setExportBitrate(bitrate: ExportBitrate) {
        _uiState.update { it.copy(exportBitrate = bitrate) }
    }

    fun addDebugCaption(text: String) {
        _uiState.update { state ->
            val caption = CaptionEntity(
                projectId = state.projectId,
                text = text,
                startTime = 0.0,
                endTime = 3.0,
                order = state.captions.size
            )
            state.copy(captions = state.captions + caption)
        }
    }

    fun removeCaption(id: String) {
        _uiState.update { state -> state.copy(captions = state.captions.filterNot { it.id == id }) }
    }

    /** Debug seed for PiP — reuses the same test video as
     * `debugSeedVideoAndMusic`, positioned bottom-trailing, active for the
     * first 2 seconds of the timeline. Not user-facing (no UI yet to pick
     * corner/timing — the export pipeline is what's being verified here). */
    fun addDebugPipOverlay(folderName: String) {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        val videoProjection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.RELATIVE_PATH)
        resolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%$folderName%"),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val overlay = OverlayItemEntity(
                    projectId = _uiState.value.projectId,
                    mediaUri = uri.toString(),
                    isVideo = true,
                    globalStartTime = 0.0,
                    duration = 2.0,
                    cornerRawValue = "bottomTrailing",
                    sizeScale = 0.35,
                    order = _uiState.value.overlays.size
                )
                _uiState.update { it.copy(overlays = it.overlays + overlay) }
            }
        }
    }

    fun removeOverlay(id: String) {
        _uiState.update { state -> state.copy(overlays = state.overlays.filterNot { it.id == id }) }
    }

    fun saveProject(title: String) {
        viewModelScope.launch {
            val state = _uiState.value
            db.projectDao().upsert(
                ProjectEntity(
                    id = state.projectId,
                    title = title,
                    musicUri = state.musicUri,
                    musicVolume = state.musicVolume,
                    colorStyleRaw = state.colorStyle.rawValue
                )
            )
            db.mediaItemDao().upsertAll(state.items)
            if (state.captions.isNotEmpty()) {
                db.captionDao().upsertAll(state.captions)
            }
            if (state.overlays.isNotEmpty()) {
                db.overlayItemDao().upsertAll(state.overlays)
            }
        }
    }

    fun startExport(outputFile: File) {
        val state = _uiState.value
        if (state.items.isEmpty()) return
        _uiState.update { it.copy(exportState = ExportState.Exporting) }
        activeTransformer = StudioExporter.export(
            context = getApplication(),
            items = state.items,
            captions = state.captions,
            overlays = state.overlays,
            colorStyle = state.colorStyle,
            outputWidth = state.exportResolution.width,
            outputHeight = state.exportResolution.height,
            bitrate = state.exportBitrate.bps,
            musicUri = state.musicUri,
            musicVolume = state.musicVolume,
            outputFile = outputFile,
            onCompleted = {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val galleryUri = runCatching { GallerySaver.saveVideo(getApplication(), outputFile) }.getOrNull()
                    _uiState.update {
                        it.copy(exportState = ExportState.Done(outputFile.absolutePath, galleryUri))
                    }
                }
            },
            onError = { message -> _uiState.update { it.copy(exportState = ExportState.Error(message)) } }
        )
    }

    override fun onCleared() {
        super.onCleared()
        activeTransformer?.cancel()
    }
}
