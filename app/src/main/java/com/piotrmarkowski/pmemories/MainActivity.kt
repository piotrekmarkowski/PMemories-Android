package com.piotrmarkowski.pmemories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.piotrmarkowski.pmemories.onboarding.OnboardingScreen
import com.piotrmarkowski.pmemories.onboarding.OnboardingStorage
import com.piotrmarkowski.pmemories.ui.nav.PMemoriesNavHost
import com.piotrmarkowski.pmemories.ui.theme.PMemoriesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PMemoriesTheme {
                val context = LocalContext.current
                // Etap 4 (14.09.2026) — analog iOS `HomeView.isShowingOnboarding`
                // (`!OnboardingStorage.hasCompleted`). Logowanie Google od razu
                // tutaj, NIE dopiero w zakładce Ranking — ten sam fix co na iOS
                // (09.08.2026, appka witała każdego usera imieniem developera,
                // bo logowanie było ukryte głęboko).
                var showOnboarding by remember { mutableStateOf(!OnboardingStorage.hasCompleted(context)) }
                if (showOnboarding) {
                    OnboardingScreen(onFinished = { showOnboarding = false })
                } else {
                    PMemoriesNavHost()
                }
            }
        }
    }
}
