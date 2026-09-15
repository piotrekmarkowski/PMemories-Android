package com.piotrmarkowski.pmemories.ui.nav

/**
 * Android analog of iOS `MainTab` (`HomeView.swift`). `Travel` added
 * 15.09.2026 (Etap 7) — `TripPlanning` (iOS's separate "before the trip"
 * segment inside the same screen) not ported yet, left for a follow-up;
 * Travel here covers "during/after" (Trips list + World Globe) first.
 * `Android.md`'s original Etap 1 wording ("Templates" as a tab) is stale —
 * Templates moved under Profile on iOS on 11.08, not part of this app yet
 * either (no Profile screen/button built).
 */
enum class MainTab(val route: String) {
    Home("home"),
    Studio("studio"),
    Travel("travel"),
    Library("library")
}
