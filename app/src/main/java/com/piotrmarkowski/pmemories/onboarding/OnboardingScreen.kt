package com.piotrmarkowski.pmemories.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piotrmarkowski.pmemories.R
import com.piotrmarkowski.pmemories.auth.AuthManager
import kotlinx.coroutines.launch

/**
 * Etap 4 — Android analog of iOS `OnboardingView`'s `signInPage`. Sign-in
 * up front in onboarding, NOT buried inside the Ranking tab — iOS bug
 * (09.08.2026): hiding sign-in deep in a tab meant the app greeted every
 * user with the developer's own name, because most people never opened
 * that screen. One screen here rather than iOS's full page carousel — no
 * Travel Map/other features to introduce yet on Android (Etap 7).
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSignedIn by AuthManager.isSignedIn.collectAsState()
    val displayName by AuthManager.displayName.collectAsState()
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSignedIn) Icons.Filled.CheckCircle else Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.height(64.dp)
        )
        Spacer(Modifier.height(20.dp))

        if (isSignedIn) {
            Text(stringResource(R.string.you_re_all_set), style = MaterialTheme.typography.headlineSmall)
            displayName?.let {
                Spacer(Modifier.height(8.dp))
                Text("${stringResource(R.string.signed_in_as)} $it", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text(stringResource(R.string.personalize_pmemories), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.sign_in_description),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(32.dp))

        // `stringResource` wymaga kontekstu @Composable — złapane od razu
        // (15.09.2026), bo `onFailure` niżej działa już POZA kompozycją
        // (`scope.launch`). Wartość łapiemy tutaj, w ciele Composable.
        val signInFailedMessage = stringResource(R.string.sign_in_failed)

        if (!isSignedIn) {
            Button(
                onClick = {
                    isSigningIn = true
                    errorMessage = null
                    scope.launch {
                        AuthManager.signIn(context)
                            .onFailure { errorMessage = it.message ?: signInFailedMessage }
                        isSigningIn = false
                    }
                },
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(R.string.sign_in_with_google))
                }
            }
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Button(
                onClick = {
                    OnboardingStorage.markCompleted(context)
                    onFinished()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.get_started))
            }
        }
    }
}
