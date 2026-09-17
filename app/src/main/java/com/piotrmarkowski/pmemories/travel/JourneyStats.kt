package com.piotrmarkowski.pmemories.travel

import com.piotrmarkowski.pmemories.data.TripWithStops

/**
 * Statystyki CAŁEGO archiwum podróży dla Plakatu "My Travel Journey" — port
 * 1:1 logiki z iOS `JourneyStats` (`TravelJourneyPosterView.swift`). Reużywa
 * te same funkcje co `TravelAchievementsCalculator`/Explorer Score, więc
 * liczba krajów tu ZAWSZE zgadza się z resztą appki (ten sam duch co iOS).
 */
data class JourneyStats(
    val countryCount: Int,
    val tripCount: Int,
    val totalKm: Int,
    val flightCount: Int,
    /** `null` gdy user nie ma ani jednej Wędrówki z policzoną wysokością —
     * ekran świadomie NIE pokazuje wtedy piątej kolumny (uczciwy stan pusty
     * zamiast zmyślonych danych), dokładnie jak na iOS. */
    val highestPeak: Pair<String, Int>?,
) {
    companion object {
        fun from(trips: List<TripWithStops>): JourneyStats {
            val allStops = trips.flatMap { it.stops }
            return JourneyStats(
                countryCount = TravelAchievementsCalculator.rawCountries(trips),
                tripCount = trips.size,
                totalKm = Math.round(allStops.sumOf { it.legDistanceKm }).toInt(),
                flightCount = TravelAchievementsCalculator.flightCount(trips),
                highestPeak = TravelAchievementsCalculator.highestPeak(trips),
            )
        }
    }
}
