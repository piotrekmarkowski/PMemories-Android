package com.piotrmarkowski.pmemories.travel

/**
 * 1:1 port of iOS `TransportMode` (`TravelMap.swift`) — same six modes, same
 * raw values (`name.lowercase()` matches Swift's `rawValue` exactly for all
 * six cases), so a trip saved on one platform reads correctly on the other
 * once cross-platform sync exists.
 */
enum class TransportMode(val rawValue: String, val emoji: String) {
    PLANE("plane", "✈️"),
    TRAIN("train", "🚆"),
    CAR("car", "🚗"),
    BOAT("boat", "⛴️"),
    CRUISE("cruise", "🛳️"),
    HIKING("hiking", "🥾");

    companion object {
        fun fromRaw(raw: String?): TransportMode = entries.firstOrNull { it.rawValue == raw } ?: PLANE
    }
}
