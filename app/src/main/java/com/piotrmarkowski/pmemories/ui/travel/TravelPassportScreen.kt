package com.piotrmarkowski.pmemories.ui.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piotrmarkowski.pmemories.data.TripWithStops
import com.piotrmarkowski.pmemories.travel.PassportCountry
import com.piotrmarkowski.pmemories.travel.TravelAchievementsCalculator
import com.piotrmarkowski.pmemories.travel.flagEmoji
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Travel Passport" — port 1:1 iOS `TravelPassportView.swift`, tym samym
 * FAKTYCZNYM stylem co iOS dziś realnie pokazuje (nie tym co `sticker_*`
 * miał kiedyś pokazywać — ten zestaw jest dziś pusty/wyłączony na obu
 * platformach, patrz `AssetIntegrityTests`/dzisiejszy bugfix). Flaga emoji +
 * nazwa kraju + data pierwszej wizyty, przerywana ramka, lekki DETERMINISTYCZNY
 * obrót per pieczątka (z hashu kodu kraju, nie losowy — żeby nie migotało przy
 * przewijaniu). Świadomie bez linku do profilu/udostępniania (iOS ma to przez
 * `ExplorerProfileService`, backend którego Android jeszcze nie ma — osobny
 * follow-up).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPassportScreen(trips: List<TripWithStops>, onBack: () -> Unit) {
    val countries = TravelAchievementsCalculator.passportCountries(trips)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Travel Passport") }) }
    ) { padding ->
        if (countries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛂", fontSize = 40.sp)
                Text("Your passport is empty", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Your first stamp will appear once you save a trip with a country",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    text = "${countries.size} ${if (countries.size == 1) "stamp" else "stamps"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(countries) { country -> PassportStampCard(country) }
                }
            }
        }
    }
}

private val stampPalette = listOf(
    Color(0xFF2F6FED), Color(0xFF2FA84F), Color(0xFFE0862F), Color(0xFF8F3FE0),
    Color(0xFF2FA6A6), Color(0xFF8A5A2F), Color(0xFFD84F8F),
)

@Composable
private fun PassportStampCard(country: PassportCountry) {
    val rotationDegrees = remember(country.countryCode) {
        val hash = country.countryCode.sumOf { it.code }
        (hash % 11) - 5
    }
    val color = remember(country.countryCode) {
        stampPalette[Math.floorMod(country.countryCode.hashCode(), stampPalette.size)]
    }
    val dateText = remember(country.firstVisitMillis) {
        country.firstVisitMillis?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: ""
    }

    // 17.09.2026 — prawdziwa naklejka gdy mamy plik (dziś: 45 krajów
    // Europy), inaczej flaga+tekst niżej. Android buduje
    // worldStickerAssetByCountryCode WYŁĄCZNIE z realnie istniejących
    // plików (nie ma 300-wpisowego słownika z dziurami jak iOS miał) —
    // więc tu wystarczy zwykłe sprawdzenie obecności klucza, bez
    // dodatkowej weryfikacji istnienia zasobu (patrz komentarz w pliku).
    val stickerDrawable = com.piotrmarkowski.pmemories.travel.worldStickerAssetByCountryCode[country.countryCode]
    if (stickerDrawable != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .rotate(rotationDegrees.toFloat())
                .padding(6.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = stickerDrawable),
                contentDescription = country.countryName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            if (dateText.isNotEmpty()) {
                Text(dateText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2F6FED))
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(rotationDegrees.toFloat())
            .border(2.dp, color, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(flagEmoji(country.countryCode), fontSize = 34.sp)
            Text(
                country.countryName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (dateText.isNotEmpty()) {
                Text(dateText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2F6FED))
            }
        }
    }
}
