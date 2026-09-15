package com.piotrmarkowski.pmemories.travel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.piotrmarkowski.pmemories.data.StopEntity

/**
 * 15.09.2026, Etap 7 — DELIBERATELY SIMPLIFIED v1 route visualization: an
 * equirectangular (flat lat/lon → x/y, not a real map projection) line
 * drawing with city labels, NOT real satellite map tiles or a 3D camera
 * flight path. iOS's `TravelMapVideoRenderer` reached satellite-globe
 * rendering with a real camera only after days of iteration (SceneKit
 * prototype tried and abandoned first, per iOS `HISTORIA.md`) — this is
 * an honest, working stepping stone in that same direction, not a
 * corner-cut hidden as the finished thing. Upgrade path: swap this
 * renderer's canvas drawing for a `GoogleMap` snapshot sequence
 * (`GoogleMap.snapshot()`) once real map tiles are wanted per-frame,
 * without touching `BitmapSequenceVideoRenderer` or the calling code.
 */
object RouteFrameRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val PADDING_FRACTION = 0.15

    /** Renders one frame at `progress` (0f = only the first stop visible,
     * 1f = the whole route drawn) — call in a loop to build an animation. */
    fun renderFrame(stops: List<StopEntity>, progress: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background — brand gradient, same blue→indigo→purple family as
        // iOS `Palette.heroGradient` (#4F8CFF → #6C63FF → #A855F7).
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                intArrayOf(Color.parseColor("#4F8CFF"), Color.parseColor("#6C63FF"), Color.parseColor("#A855F7")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        if (stops.isEmpty()) return bitmap

        val minLat = stops.minOf { it.latitude }
        val maxLat = stops.maxOf { it.latitude }
        val minLon = stops.minOf { it.longitude }
        val maxLon = stops.maxOf { it.longitude }
        val latSpan = (maxLat - minLat).coerceAtLeast(0.5)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.5)

        val drawableWidth = WIDTH * (1 - 2 * PADDING_FRACTION)
        val drawableHeight = HEIGHT * (1 - 2 * PADDING_FRACTION) * 0.7 // leave room for stat text at bottom
        val originX = WIDTH * PADDING_FRACTION
        val originY = HEIGHT * PADDING_FRACTION

        fun project(lat: Double, lon: Double): Pair<Float, Float> {
            val x = originX + ((lon - minLon) / lonSpan) * drawableWidth
            val y = originY + (1 - (lat - minLat) / latSpan) * drawableHeight
            return x.toFloat() to y.toFloat()
        }

        val points = stops.map { project(it.latitude, it.longitude) }
        val visibleCount = (1 + progress * (points.size - 1)).coerceAtMost(points.size.toFloat())

        val linePaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        for (i in 0 until points.size - 1) {
            if (i + 1 > visibleCount) break
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        val dotPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        points.forEachIndexed { index, (x, y) ->
            if (index + 1 <= visibleCount) {
                canvas.drawCircle(x, y, 12f, dotPaint)
                canvas.drawText(stops[index].cityName, x, y - 24f, labelPaint)
            }
        }

        val statPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val visibleStops = stops.take(visibleCount.toInt().coerceAtLeast(1))
        val distanceSoFar = visibleStops.sumOf { it.legDistanceKm }
        canvas.drawText(
            "${visibleStops.size}/${stops.size} stops · ${distanceSoFar.toInt()} km",
            WIDTH / 2f,
            HEIGHT * 0.92f,
            statPaint
        )

        return bitmap
    }
}
