package com.piotrmarkowski.pmemories.studio

import android.content.Context
import java.io.File

/**
 * Safety net against littering app storage — Android analog of iOS
 * `TempFileCleanup` (`FileManager.default.temporaryDirectory`, purged at
 * cold start + backgrounding after a 253-file/11GB report there).
 *
 * Android has its OWN version of that exact bug, found while building this:
 * every Studio export writes a new timestamped file to
 * `getExternalFilesDir("exports")` (`exportOutputFile` in `StudioScreen.kt`)
 * and `GallerySaver` copies it into the public Gallery/MediaStore — but
 * never deletes the private original. Each export permanently doubles its
 * own size in app-private storage forever, the same shape of leak as the
 * iOS report this feature was built for there.
 *
 * Threshold-based sweep (delete anything older than `maxAgeMillis`), not an
 * immediate delete right after a successful gallery copy — same reasoning
 * as iOS: one simple, generic mechanism that any future temp-writing code
 * path is automatically covered by, instead of hand-tracking each writer's
 * lifecycle individually.
 */
object TempFileCleanup {
    /** Same 1h default as iOS — long enough that no legitimate in-flight
     * export/session could still need a file this old. */
    private const val DEFAULT_MAX_AGE_MILLIS = 60 * 60 * 1000L

    fun purgeStaleFiles(context: Context, maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        // `getExternalFilesDir("exports")` — the leaking export duplicates
        // described above. `cacheDir` — general hygiene for anything else
        // written there (e.g. `StudioExporter`'s `pip_filler.png`), mirrors
        // iOS sweeping the whole temp directory, not just one known writer.
        purgeDirectory(context.getExternalFilesDir("exports"), cutoff)
        purgeDirectory(context.cacheDir, cutoff)
    }

    private fun purgeDirectory(dir: File?, cutoffMillis: Long) {
        val files = dir?.listFiles() ?: return
        for (file in files) {
            if (file.isFile && file.lastModified() < cutoffMillis) {
                file.delete()
            }
        }
    }
}
