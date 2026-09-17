package com.piotrmarkowski.pmemories.travel

/** ISO country code (2 letters) -> flag emoji, via Unicode regional
 * indicator symbols (standard trick, same result as iOS `flagEmoji`).
 * Codes other than 2 letters (e.g. synthetic "GB-ENG") get a safe 🌍
 * fallback instead of a garbled/wrong flag. */
fun flagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"
    val base = 0x1F1E6 - 'A'.code
    return countryCode.uppercase().map { base + it.code }
        .joinToString("") { String(Character.toChars(it)) }
}
