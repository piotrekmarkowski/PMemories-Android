package com.piotrmarkowski.pmemories.ui.nav

/**
 * Android analog of iOS `MainTab` (`HomeView.swift`). Deliberately only the
 * three tabs that don't depend on travel data: `.travel` and `.tripPlanning`
 * wait for Etap 7 (Travel Map), same as everything else travel-related.
 * `Android.md`'s original Etap 1 wording ("Templates" as a tab) is stale —
 * Templates moved under Profile on iOS on 11.08, and Profile/Ranking is
 * Etap 4 here, so it isn't part of this skeleton either.
 */
enum class MainTab(val route: String) {
    Home("home"),
    Studio("studio"),
    Library("library")
}
