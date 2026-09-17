package com.piotrmarkowski.pmemories.travel

import com.piotrmarkowski.pmemories.R

/**
 * Kod kraju -> R.drawable prawdziwej naklejki "World Travel Stickers" dla
 * Travel Passport (17.09.2026, pierwsza partia — Europa, 45 krajów).
 * Port stanu iOS `worldStickerAssetByCountryCode`, ale TYLKO wpisy, dla
 * których faktycznie mamy plik (iOS trzyma ~300 wpisów w słowniku mimo że
 * ~170 nie ma prawdziwego assetu — to była właśnie przyczyna dzisiejszego
 * bugfixu; tu na Androidzie budujemy słownik WYŁĄCZNIE z tego co naprawdę
 * istnieje, więc ten konkretny błąd nie może się tu w ogóle powtórzyć).
 * Kraje spoza tej listy spadają na fallback flaga+tekst w
 * `TravelPassportScreen.kt`, dokładnie jak na iOS.
 */
val worldStickerAssetByCountryCode: Map<String, Int> = mapOf(
    "DE" to R.drawable.sticker_germany,
    "GR" to R.drawable.sticker_greece,
    "HU" to R.drawable.sticker_hungary,
    "IS" to R.drawable.sticker_iceland,
    "IE" to R.drawable.sticker_ireland,
    "IT" to R.drawable.sticker_italy,
    "XK" to R.drawable.sticker_kosovo,
    "LV" to R.drawable.sticker_latvia,
    "LI" to R.drawable.sticker_liechtenstein,
    "LT" to R.drawable.sticker_lithuania,
    "LU" to R.drawable.sticker_luxembourg,
    "MT" to R.drawable.sticker_malta,
    "MD" to R.drawable.sticker_moldova,
    "MC" to R.drawable.sticker_monaco,
    "ME" to R.drawable.sticker_montenegro,
    "AL" to R.drawable.sticker_albania,
    "AD" to R.drawable.sticker_andorra,
    "AT" to R.drawable.sticker_austria,
    "BY" to R.drawable.sticker_belarus,
    "BE" to R.drawable.sticker_belgium,
    "BA" to R.drawable.sticker_bosnia_and_herzegovina,
    "BG" to R.drawable.sticker_bulgaria,
    "HR" to R.drawable.sticker_croatia,
    "CY" to R.drawable.sticker_cyprus,
    "CZ" to R.drawable.sticker_czechia,
    "DK" to R.drawable.sticker_denmark,
    "EE" to R.drawable.sticker_estonia,
    "FI" to R.drawable.sticker_finland,
    "FR" to R.drawable.sticker_france,
    "GE" to R.drawable.sticker_georgia,
    "NL" to R.drawable.sticker_netherlands,
    "MK" to R.drawable.sticker_north_macedonia,
    "NO" to R.drawable.sticker_norway,
    "PL" to R.drawable.sticker_poland,
    "PT" to R.drawable.sticker_portugal,
    "RO" to R.drawable.sticker_romania,
    "RU" to R.drawable.sticker_russia,
    "SM" to R.drawable.sticker_san_marino,
    "RS" to R.drawable.sticker_serbia,
    "SK" to R.drawable.sticker_slovakia,
    "SI" to R.drawable.sticker_slovenia,
    "ES" to R.drawable.sticker_spain,
    "SE" to R.drawable.sticker_sweden,
    "CH" to R.drawable.sticker_switzerland,
    "TR" to R.drawable.sticker_turkiye,
)
