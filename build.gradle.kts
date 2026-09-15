plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    // Etap 4 (14.09.2026) — wymaga PRAWDZIWEGO google-services.json z
    // konsoli Firebase (console.firebase.google.com), którego appka nie
    // ma jeszcze na tym Macu (trzeba przejść przez logowanie Google w
    // przeglądarce — user zrobi to sam). Do tego czasu placeholder w
    // `app/google-services.json` (patrz komentarz w tym pliku) pozwala
    // reszcie kodu się skompilować.
    alias(libs.plugins.google.services) apply false
}
