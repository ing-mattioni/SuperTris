package it.claudio.supertris.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.claudio.supertris.core.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    private fun decodeGameStateOrNull(raw: String?): GameState? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(GameState.serializer(), raw) }.getOrNull()
    }
}