package com.piotrmarkowski.pmemories.onboarding

import android.content.Context

/** Android analog of iOS `OnboardingStorage` (`UserDefaults` bool flag). */
object OnboardingStorage {
    private const val PREFS = "onboarding"
    private const val KEY = "hasCompletedOnboarding"

    fun hasCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}
