package com.piotrmarkowski.pmemories.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * 16.09.2026 — Android analog of iOS `AnalyticsLogger`, SAME event shape
 * (same names/params) so data is comparable across platforms — but a much
 * simpler implementation: iOS routed this through the CloudKit
 * `LeaderboardEntry` database because iOS has no built-in telemetry
 * backend. Android already has a real one the moment a real Firebase
 * project exists (as of today) — `Firebase Analytics`, built in, free,
 * zero extra plumbing. No custom "AppEvent" schema/record type needed the
 * way iOS required (`cktool import-schema` + manual "Deploy Schema
 * Changes to Production") — Firebase auto-creates event definitions on
 * first log call, in BOTH its dev and prod views, no separate deploy step.
 *
 * Same "two layers" split as iOS:
 * 1) REALLY WIRED today (Android's feature set is smaller than iOS's right
 *    now — Etap 2 Studio MVP has no premium-bound transitions, no 4K
 *    export tier, no AI Director yet, so those iOS call sites simply don't
 *    exist here to wire). What DOES exist and IS wired:
 *    `app_opened`, `export_started`, `export_completed`,
 *    `transport_mode_used`, `leaderboard_opened`, and ONE
 *    `premium_feature_tapped` (multi_transport — Travel Map already
 *    distinguishes Free/Premium there, same as iOS).
 * 2) DEFINED, not yet wired — `premium_feature_viewed`, `paywall_*`,
 *    `purchase_*`, `free_limit_reached`, `upgrade_button_tapped` — kept in
 *    the event vocabulary for the same reason as iOS: one stable schema,
 *    not two migrations, once Studio grows premium-gated features/a real
 *    paywall.
 */
object AnalyticsLogger {
    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    // Dziś zawsze "free" — appka nie ma jeszcze żadnego systemu
    // subskrypcji/zakupów, ten sam powód co pole `planTier` w iOS
    // `AnalyticsLogger`. Zostaje w każdym zdarzeniu już teraz, nie
    // dopisywane później, żeby nie migrować starych danych.
    private const val PLAN_TIER = "free"

    sealed class Event(val name: String, val params: Bundle) {
        data object AppOpened : Event("app_opened", Bundle())
        data object ExportStarted : Event("export_started", Bundle())
        class ExportCompleted(durationSeconds: Double, usedColorFilter: Boolean) : Event(
            "export_completed",
            Bundle().apply {
                putDouble("duration_seconds", durationSeconds)
                putInt("used_color_filter", if (usedColorFilter) 1 else 0)
            }
        )
        class TransportModeUsed(mode: String) : Event(
            "transport_mode_used",
            Bundle().apply { putString("mode", mode) }
        )
        data object LeaderboardOpened : Event("leaderboard_opened", Bundle())
        class PremiumFeatureViewed(feature: String, source: String) : Event(
            "premium_feature_viewed",
            Bundle().apply { putString("feature", feature); putString("source", source) }
        )
        class PremiumFeatureTapped(feature: String, source: String) : Event(
            "premium_feature_tapped",
            Bundle().apply { putString("feature", feature); putString("source", source) }
        )

        // Zdefiniowane, jeszcze NIEPODPIĘTE — patrz komentarz przy klasie.
        class PaywallOpened(source: String) : Event("paywall_opened", Bundle().apply { putString("source", source) })
        class PaywallClosed(source: String) : Event("paywall_closed", Bundle().apply { putString("source", source) })
        data object PurchaseStarted : Event("purchase_started", Bundle())
        data object PurchaseCompleted : Event("purchase_completed", Bundle())
        class PurchaseFailed(reason: String) : Event("purchase_failed", Bundle().apply { putString("reason", reason) })
        class UpgradeButtonTapped(source: String) : Event("upgrade_button_tapped", Bundle().apply { putString("source", source) })
        class FreeLimitReached(limit: String) : Event("free_limit_reached", Bundle().apply { putString("limit", limit) })
    }

    fun log(event: Event) {
        event.params.putString("plan_tier", PLAN_TIER)
        analytics.logEvent(event.name, event.params)
    }
}
