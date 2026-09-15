package com.piotrmarkowski.pmemories.travel

import android.content.Context
import android.net.Uri
import com.piotrmarkowski.pmemories.data.TripWithStops
import java.io.File

/**
 * Renders a trip's route as a short animated video — Android analog of iOS
 * `TravelMapVideoRenderer`'s job (animated camera along the route), but a
 * DELIBERATELY simplified v1 renderer underneath — see `RouteFrameRenderer`
 * for what's simplified and why, and its upgrade path to real map tiles.
 */
object RouteVideoRenderer {
    private const val FRAME_COUNT = 60 // ~4s at frameDurationSeconds below
    private const val FRAME_DURATION_SECONDS = 0.067 // ~15fps animation cadence

    suspend fun render(context: Context, trip: TripWithStops): Uri {
        val stops = trip.orderedStops
        require(stops.isNotEmpty()) { "Trip has no stops" }

        val frames = (0 until FRAME_COUNT).map { i ->
            val progress = i.toFloat() / (FRAME_COUNT - 1).coerceAtLeast(1)
            RouteFrameRenderer.renderFrame(stops, progress)
        }

        val outputFile = File(context.cacheDir, "route_${trip.trip.id}_${System.currentTimeMillis()}.mp4")
        return BitmapSequenceVideoRenderer.render(context, frames, FRAME_DURATION_SECONDS, outputFile)
    }
}
