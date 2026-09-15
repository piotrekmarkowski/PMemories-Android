package com.piotrmarkowski.pmemories.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.piotrmarkowski.pmemories.R
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piotrmarkowski.pmemories.auth.AuthManager
import com.piotrmarkowski.pmemories.data.ProjectWithDetails
import java.util.Calendar

/**
 * Home/Dashboard tab, Etap 3 — Android analog of iOS `HomeView`'s
 * `continueEditingCard`. "Recent Memories" (the other half of the original
 * `Android.md` Etap 3 line item) turned out NOT to be Travel-independent
 * after all — on iOS it reads from `SavedTrip` (flags, day count, stop
 * count — see `HomeView.recentMemoriesSection`/`RecentMemoryTile`), not
 * from `SavedProject`. That data doesn't exist on Android until Etap 7
 * (Travel Map). Moved there in `Android.md` rather than faked here with a
 * different, non-equivalent data source — see `Android.md` Etap 7.
 *
 * The travel-stats part of the iOS Dashboard ("Twoja podróż" etc.) stays
 * out entirely for the same reason, unchanged from the original plan.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier, viewModel: DashboardViewModel = viewModel()) {
    val continuable by viewModel.continuableProject.collectAsState()
    val displayName by AuthManager.displayName.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(greeting(displayName), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (continuable != null) {
            ContinueEditingCard(continuable!!)
        } else {
            Text(
                "Your most recent unfinished project will show up here once you start one.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Etap 4 (14.09.2026) — Android analog of iOS `HomeView.greeting` (variant
 * 0 only; iOS also rotates two other, name-independent variants — not
 * mirrored here since this is specifically the "greet by name" checklist
 * item, not the whole rotation). `displayName` is already just the first
 * word (see `AuthManager.firstWordOf`) — no name at all when not signed
 * in, never a guessed/hardcoded one (iOS bug, 08.08.2026: "Good afternoon,
 * Piotr" shown on the tester's OWN phone because the developer's name was
 * hardcoded).
 */
private fun greeting(displayName: String?): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (displayName != null) "$timeGreeting, $displayName" else "$timeGreeting!"
}

/**
 * Deliberately NOT tappable yet — on iOS this opens the project back into
 * the editor, but `StudioViewModel` on Android can only create a brand new
 * project so far (no load-by-id). Shown as a status card until that
 * capability exists, same reasoning as the Library screen's rows.
 */
@Composable
private fun ContinueEditingCard(details: ProjectWithDetails) {
    val relativeTime = remember(details.project.updatedAt) {
        DateUtils.getRelativeTimeSpanString(
            details.project.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(R.string.continue_editing), style = MaterialTheme.typography.labelSmall)
            Text(
                details.project.title.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${details.items.size} clips • $relativeTime",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
