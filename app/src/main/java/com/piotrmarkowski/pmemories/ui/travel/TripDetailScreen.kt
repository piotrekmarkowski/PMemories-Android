package com.piotrmarkowski.pmemories.ui.travel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.piotrmarkowski.pmemories.data.AppDatabase
import com.piotrmarkowski.pmemories.data.TripWithStops

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(tripId: String, onEdit: () -> Unit) {
    val context = LocalContext.current
    var trip by remember { mutableStateOf<TripWithStops?>(null) }

    LaunchedEffect(tripId) {
        trip = AppDatabase.get(context).tripDao().getWithStops(tripId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.trip?.title ?: "") },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val current = trip
            if (current == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                TripRouteMap(trip = current, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
