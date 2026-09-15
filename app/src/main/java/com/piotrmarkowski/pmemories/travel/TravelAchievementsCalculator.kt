package com.piotrmarkowski.pmemories.travel

import com.piotrmarkowski.pmemories.data.StopEntity
import com.piotrmarkowski.pmemories.data.TripWithStops
import java.util.Locale

/**
 * 1:1 port of iOS `ExplorerScore`/`TravelAchievementsCalculator.explorerScore`
 * (`TravelAchievements.swift`) — same seven weighted components (countries
 * ×20, cities ×5, transport modes ×25, distance÷100, hiking km ×1,
 * elevation÷100, peak÷100), same "diversity over raw distance" philosophy,
 * same deterministic (non-AI) formula.
 */
data class ScoreComponent(val id: String, val points: Double)

data class ExplorerScore(val components: List<ScoreComponent>, val totalKm: Double) {
    val total: Double get() = components.sumOf { it.points }

    companion object {
        val tierNames = listOf("Newcomer", "Traveller", "Explorer", "Globetrotter", "Legend")
        val tierFloors = listOf(0.0, 50.0, 150.0, 350.0, 700.0)
    }

    val tierName: String
        get() {
            val index = tierFloors.indexOfLast { total >= it }.coerceAtLeast(0)
            return tierNames[index]
        }
}

object TravelAchievementsCalculator {

    /** UK-only region grouping — mirrors iOS `ukPassportRegion`. Apple/Android
     * geocoders both return the plain English constituent-nation name as the
     * first-level admin area for UK addresses, so a simple string match works
     * on both platforms. */
    fun countryGroupingCode(countryCode: String?, administrativeArea: String?): String? {
        if (countryCode == null) return null
        if (countryCode == "GB") {
            return when (administrativeArea?.lowercase(Locale.ROOT)) {
                "england" -> "GB-ENG"
                "scotland" -> "GB-SCT"
                "wales" -> "GB-WLS"
                "northern ireland" -> "GB-NIR"
                else -> "GB"
            }
        }
        return countryCode
    }

    fun explorerScore(trips: List<TripWithStops>): ExplorerScore {
        val allStops = trips.flatMap { it.stops }
        val stopsWithRealLeg = allStops.filter { it.order > 0 && it.legDistanceKm > 0 }

        val countryCount = allStops.mapNotNull { countryGroupingCode(it.countryCode, it.administrativeArea) }.toSet().size.toDouble()
        val cityCount = allStops.map { it.cityName.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.toSet().size.toDouble()
        val distinctTransportModes = stopsWithRealLeg.map { it.transportRawValue }.toSet().size.toDouble()
        val totalKm = allStops.sumOf { it.legDistanceKm }
        val hikingKm = stopsWithRealLeg.filter { it.transportRawValue == TransportMode.HIKING.rawValue }.sumOf { it.legDistanceKm }
        val elevationGain = allStops.mapNotNull { it.elevationGainMeters }.sum()
        val highestPeak = allStops.mapNotNull { it.highestElevationMeters }.maxOrNull() ?: 0.0

        return ExplorerScore(
            components = listOf(
                ScoreComponent("countries", countryCount * 20),
                ScoreComponent("cities", cityCount * 5),
                ScoreComponent("transportModes", distinctTransportModes * 25),
                ScoreComponent("distance", Math.round(totalKm / 100).toDouble()),
                ScoreComponent("hiking", hikingKm),
                ScoreComponent("elevation", Math.round(elevationGain / 100).toDouble()),
                ScoreComponent("peak", Math.round(highestPeak / 100).toDouble())
            ),
            totalKm = totalKm
        )
    }

    /** Real (not points-derived) helper values for display — same reasoning
     * as iOS `ExplorerScore.rawCountries`/`rawCities`: don't reverse rounded
     * points, read the real counts straight from the trips. */
    fun rawCountries(trips: List<TripWithStops>): Int =
        trips.flatMap { it.stops }.mapNotNull { countryGroupingCode(it.countryCode, it.administrativeArea) }.toSet().size

    fun rawCities(trips: List<TripWithStops>): Int =
        trips.flatMap { it.stops }.map { it.cityName.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.toSet().size

    /** Flight count — same `order > 0 && legDistanceKm > 0` filter as iOS
     * (excludes the synthetic order-0 origin stop, which always carries the
     * default transport mode despite having no real leg before it — the
     * 56-vs-28 bug fixed on iOS 13.09.2026, avoided here from day one). */
    fun flightCount(trips: List<TripWithStops>): Int =
        trips.flatMap { it.stops }.count { it.order > 0 && it.legDistanceKm > 0 && it.transportRawValue == TransportMode.PLANE.rawValue }

    fun rawElevationGainMeters(trips: List<TripWithStops>): Double =
        trips.flatMap { it.stops }.mapNotNull { it.elevationGainMeters }.sum()

    fun highestPeak(trips: List<TripWithStops>): Pair<String, Int>? {
        val peakStop = trips.flatMap { it.stops }
            .filter { it.transportRawValue == TransportMode.HIKING.rawValue && it.highestElevationMeters != null }
            .maxByOrNull { it.highestElevationMeters ?: 0.0 }
            ?: return null
        return peakStop.cityName to Math.round(peakStop.highestElevationMeters ?: 0.0).toInt()
    }
}
