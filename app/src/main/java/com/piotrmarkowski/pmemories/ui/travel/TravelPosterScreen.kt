package com.piotrmarkowski.pmemories.ui.travel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piotrmarkowski.pmemories.data.TripWithStops
import com.piotrmarkowski.pmemories.travel.JourneyStats
import com.piotrmarkowski.pmemories.travel.flagEmoji
import com.piotrmarkowski.pmemories.travel.journeyStampAssetByCountryCode

/**
 * "My Travel Journey" — Etap 1 (17.09.2026), świadomie OKROJONY zakres:
 * statystyki + rząd pieczątek krajów, port 1:1 logiki i stylu z iOS
 * (`TravelJourneyPosterView.swift`, `statsPlaque`/`stampsRow`). ŚWIADOMIE
 * bez mapy (`MKMapSnapshotter` → odpowiednik Google Maps snapshot, osobny
 * etap), bez prawdziwych zdjęć-polaroidów, bez tekstury papieru/ziarna —
 * to była na iOS wielodniowa, wieloetapowa robota (patrz komentarze w
 * pliku źródłowym — "dwudziesta runda" poprawek samej mapy), tu dowozimy
 * rdzeń najpierw, resztę etapami tak jak iOS przeszedł.
 *
 * "Najwyższy szczyt" pokazywany BEZ blokady Premium — sprawdzone: iOS sam
 * dziś (17.09.2026) tego NIE egzekwuje w kodzie mimo udokumentowanej
 * decyzji produktowej (`Pricing.md`, 15.09.2026) — żaden system
 * subskrypcji/IAP jeszcze nie istnieje na żadnej platformie. Android jest
 * lustrem FAKTYCZNEGO stanu iOS, nie stanu docelowego (ta sama zasada co
 * przy ramkach avatara).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPosterScreen(trips: List<TripWithStops>, onBack: () -> Unit) {
    val stats = JourneyStats.from(trips)
    val countries = trips.flatMap { it.stops }
        .distinctBy { it.countryCode ?: it.country }
        .filter { it.countryCode != null }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Travel Journey") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF4EFE3)) // ciepły kremowy papier — tymczasowe tło zanim dojdzie prawdziwa tekstura (Etap 2)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "${stats.countryCount} Countries • Countless Memories",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            JourneyStatsPlaque(stats)
            JourneyStampsRow(countryCodes = countries.mapNotNull { it.countryCode })
        }
    }
}

@Composable
private fun JourneyStatsPlaque(stats: JourneyStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF2DE).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFF29241C).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatColumn("🌍", "${stats.countryCount}", "Countries")
        PlainDivider()
        StatColumn("🧭", "${stats.tripCount}", "Trips")
        PlainDivider()
        StatColumn("📍", "${stats.totalKm}", "KM Travelled")
        PlainDivider()
        StatColumn("✈️", "${stats.flightCount}", "Flights")
        stats.highestPeak?.let { (name, meters) ->
            PlainDivider()
            StatColumn("⛰️", "${meters}m", name)
        }
    }
}

@Composable
private fun StatColumn(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29241C))
        Text(label, fontSize = 10.sp, color = Color(0xFF29241C).copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlainDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color(0xFF29241C).copy(alpha = 0.2f))
    )
}

/** Maks. 12 pieczątek + kafelek "+N MORE" — Etap 1 świadomie bez
 * `TravelRarityScore` (który wybierał na iOS "najrzadsze" kraje do
 * pokazania) — tu proste pierwsze 12, doprecyzowanie priorytetu wyboru to
 * osobny, mniejszy follow-up. */
@Composable
private fun JourneyStampsRow(countryCodes: List<String>) {
    val visible = countryCodes.take(12)
    val hiddenCount = (countryCodes.size - visible.size).coerceAtLeast(0)

    LazyVerticalGrid(
        columns = GridCells.Fixed(minOf(maxOf(visible.size + if (hiddenCount > 0) 1 else 0, 1), 6)),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(visible) { code ->
            StampCard(code = code)
        }
        if (hiddenCount > 0) {
            item { MoreStampsCard(count = hiddenCount) }
        }
    }
}

@Composable
private fun StampCard(code: String) {
    val drawableId = journeyStampAssetByCountryCode[code]
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = code,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(85.dp)
        )
    } else {
        // Fallback dla krajów spoza 214-elementowego zestawu (drobne
        // terytoria zamorskie) — flaga z emoji regionalnych wskaźników,
        // ten sam algorytm co iOS `flagEmoji`.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .border(1.5.dp, Color(0xFF29241C).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Text(flagEmoji(code), fontSize = 28.sp)
        }
    }
}

@Composable
private fun MoreStampsCard(count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(Color(0xFFFAF2DE).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .border(1.5.dp, Color(0xFF29241C).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text("+$count", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF29241C).copy(alpha = 0.75f))
        Text("MORE", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF29241C).copy(alpha = 0.75f))
    }
}
