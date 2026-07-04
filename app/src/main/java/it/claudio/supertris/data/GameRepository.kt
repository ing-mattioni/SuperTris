package it.claudio.supertris.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.claudio.supertris.core.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "supertris")

class GameRepository(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val keyGameState: Preferences.Key<String> = stringPreferencesKey("game_state_json")
    private val keyNickname: Preferences.Key<String> = stringPreferencesKey("nickname")
    private val keyNotifiedMoves: Preferences.Key<String> = stringPreferencesKey("online_notified_moves_json")
    private val keyStats: Preferences.Key<String> = stringPreferencesKey("game_stats_json")
    private val keyHistory: Preferences.Key<String> = stringPreferencesKey("game_history_json")
    private val keyRecordedResults: Preferences.Key<String> = stringPreferencesKey("online_recorded_results_json")

    val gameStateFlow: Flow<GameState?> = context.dataStore.data.map { prefs ->
        decodeGameStateOrNull(prefs[keyGameState])
    }

    // "Continua" deve essere disabilitato se non c'e' una partita in corso.
    val hasInProgressGameFlow: Flow<Boolean> = gameStateFlow.map { st ->
        st != null && !st.isGameOver()
    }

    suspend fun saveGame(state: GameState) {
        val encoded = json.encodeToString(GameState.serializer(), state)
        context.dataStore.edit { prefs ->
            prefs[keyGameState] = encoded
        }
    }

    suspend fun clearSavedGame() {
        context.dataStore.edit { prefs ->
            prefs.remove(keyGameState)
        }
    }

    // ---- Multiplayer online (v1.3) ----

    val nicknameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[keyNickname]?.takeIf { it.isNotBlank() }
    }

    suspend fun saveNickname(nickname: String) {
        context.dataStore.edit { prefs ->
            prefs[keyNickname] = nickname.trim()
        }
    }

    // Mappa codice stanza -> numero mosse gia' notificate: evita che il worker
    // delle notifiche ripeta "e' il tuo turno" per la stessa mossa.
    suspend fun readNotifiedMoves(): Map<String, Int> {
        val raw = context.dataStore.data.map { it[keyNotifiedMoves] }.first()
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString(
                MapSerializer(String.serializer(), Int.serializer()),
                raw,
            )
        }.getOrDefault(emptyMap())
    }

    suspend fun saveNotifiedMoves(map: Map<String, Int>) {
        val encoded = json.encodeToString(
            MapSerializer(String.serializer(), Int.serializer()),
            map,
        )
        context.dataStore.edit { prefs ->
            prefs[keyNotifiedMoves] = encoded
        }
    }

    // ---- Statistiche e cronologia (v1.5) ----

    val statsFlow: Flow<GameStats> = context.dataStore.data.map { prefs ->
        decodeStats(prefs[keyStats])
    }

    val historyFlow: Flow<List<HistoryEntry>> = context.dataStore.data.map { prefs ->
        decodeHistory(prefs[keyHistory])
    }

    /** Registra una partita conclusa: aggiorna statistiche e cronologia in un colpo solo. */
    suspend fun recordFinishedGame(entry: HistoryEntry) {
        context.dataStore.edit { prefs ->
            val stats = decodeStats(prefs[keyStats]).applying(entry)
            prefs[keyStats] = json.encodeToString(GameStats.serializer(), stats)

            val history = (listOf(entry) + decodeHistory(prefs[keyHistory])).take(HISTORY_LIMIT)
            prefs[keyHistory] = json.encodeToString(ListSerializer(HistoryEntry.serializer()), history)
        }
    }

    /**
     * Marca l'esito di una stanza online come registrato (chiave "codice#round").
     * Ritorna true solo la prima volta: evita doppi conteggi tra sessioni.
     */
    suspend fun markOnlineResultRecorded(key: String): Boolean {
        var isNew = false
        context.dataStore.edit { prefs ->
            val current = decodeStringList(prefs[keyRecordedResults])
            if (!current.contains(key)) {
                isNew = true
                prefs[keyRecordedResults] = json.encodeToString(
                    ListSerializer(String.serializer()),
                    (listOf(key) + current).take(RECORDED_LIMIT),
                )
            }
        }
        return isNew
    }

    private fun decodeStats(raw: String?): GameStats {
        if (raw.isNullOrBlank()) return GameStats()
        return runCatching { json.decodeFromString(GameStats.serializer(), raw) }
            .getOrDefault(GameStats())
    }

    private fun decodeHistory(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(HistoryEntry.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val HISTORY_LIMIT = 50
        const val RECORDED_LIMIT = 100
    }

    private fun decodeGameStateOrNull(raw: String?): GameState? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(GameState.serializer(), raw) }.getOrNull()
    }
}