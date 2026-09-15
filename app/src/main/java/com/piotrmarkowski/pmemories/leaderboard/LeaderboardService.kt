package com.piotrmarkowski.pmemories.leaderboard

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.piotrmarkowski.pmemories.auth.AuthManager
import kotlinx.coroutines.tasks.await

/**
 * Firestore leaderboard — Android analog of iOS `LeaderboardService`
 * (CloudKit). Wyniki będą realnie liczone dopiero po Etapie 7 (Travel Map/
 * Explorer Score) — na razie infrastruktura, wołana z zerami/placeholderami
 * dopóki appka nie ma jeszcze danych podróży do policzenia.
 */
data class LeaderboardEntry(
    val userId: String = "",
    val displayName: String = "",
    val score: Double = 0.0,
    val km: Double = 0.0,
    val countries: Int = 0,
    val cities: Int = 0,
    val elevationM: Double = 0.0,
    val avatarFrame: String = "none"
)

enum class LeaderboardSort(val field: String) {
    SCORE("score"),
    COUNTRIES("countries")
}

object LeaderboardService {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private const val COLLECTION = "leaderboard"

    class NotSignedInException : Exception("Not signed in")

    /**
     * Upsert po stałym ID (Firebase UID) — jeden dokument na usera, ten sam
     * duch co `CKRecord.ID(recordName: userIdentifier)` na iOS.
     *
     * Transakcja zamiast osobnego fetch-then-mutate-then-save — przy
     * przeglądzie kodu iOS 13.09.2026 złapany REALNY bug w dokładnie takim
     * wzorcu (`submitCurrentScore` na iOS): dwa nakładające się wywołania
     * (np. `.task` + `.refreshable` w tym samym oknie) mogą się prześcignąć
     * i zgubić aktualizację. Firestore ma do tego wbudowany, atomiczny
     * prymityw (`runTransaction`) — budujemy to tutaj od razu poprawnie
     * zamiast powtarzać ten sam błąd i łatać go później.
     */
    suspend fun submitCurrentScore(
        score: Double, km: Double, countries: Int, cities: Int, elevationM: Double, avatarFrame: String
    ) {
        val userId = AuthManager.userIdentifier ?: throw NotSignedInException()
        val displayName = AuthManager.displayName.value ?: "Traveler"
        val docRef = db.collection(COLLECTION).document(userId)

        db.runTransaction { transaction ->
            val existing = transaction.get(docRef)
            val existingScore = existing.getDouble("score") ?: 0.0
            // Urządzenie z mniej kompletną lokalną historią (np. appka bez
            // pełnych danych podróży) nie powinno NIGDY obniżyć już
            // zapisanego wyniku — bierzemy zawsze wyższy z dwóch, ten sam
            // fix co na iOS (09.08.2026, realny bug: appka na drugim
            // urządzeniu nadpisała prawdziwy wynik zerem).
            if (existingScore > score) return@runTransaction null

            transaction.set(
                docRef,
                mapOf(
                    "displayName" to displayName,
                    "avatarFrame" to avatarFrame,
                    "score" to score,
                    "km" to km,
                    "countries" to countries,
                    "cities" to cities,
                    "elevationM" to elevationM,
                    "updatedAt" to Timestamp.now()
                )
            )
            null
        }.await()
    }

    suspend fun topEntries(sortBy: LeaderboardSort = LeaderboardSort.SCORE, limit: Int = 50): List<LeaderboardEntry> {
        val snapshot = db.collection(COLLECTION)
            .orderBy(sortBy.field, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snapshot.documents.map { doc ->
            LeaderboardEntry(
                userId = doc.id,
                displayName = doc.getString("displayName") ?: "Traveler",
                score = doc.getDouble("score") ?: 0.0,
                km = doc.getDouble("km") ?: 0.0,
                countries = (doc.getLong("countries") ?: 0L).toInt(),
                cities = (doc.getLong("cities") ?: 0L).toInt(),
                elevationM = doc.getDouble("elevationM") ?: 0.0,
                avatarFrame = doc.getString("avatarFrame") ?: "none"
            )
        }
    }
}
