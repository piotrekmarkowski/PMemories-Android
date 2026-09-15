package com.piotrmarkowski.pmemories

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.piotrmarkowski.pmemories.studio.TempFileCleanup

/**
 * Android analog of iOS `PMemoriesAppApp` — the one place that runs once
 * for the whole process, same level `TempFileCleanup.purgeStaleTemporaryFiles()`
 * hooks into there (`init()` for cold start, `scenePhase == .background`
 * for backgrounding). `ProcessLifecycleOwner` is the process-wide analog of
 * `scenePhase` — a plain `Activity.onStop()` would also fire on rotation/
 * multi-window, which isn't "the app went to background" the way iOS means
 * it.
 */
class PMemoriesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TempFileCleanup.purgeStaleFiles(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                TempFileCleanup.purgeStaleFiles(this@PMemoriesApplication)
            }
        })
    }
}
