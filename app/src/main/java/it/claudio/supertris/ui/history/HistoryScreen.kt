package it.claudio.supertris.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.SuperTrisRules
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.data.HistoryEntry
import it.claudio.supertris.data.localWon
import it.claudio.supertris.ui.components.SuperBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    repo: GameRepository,
    onOpenReplay: (index: Int) -> Unit,
    onBack: () -> Unit,
) {
    val history by repo.historyFlow.collectAsState(initial = emptyList())

    SuperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.history_titolo),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (history.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.history_vuota),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            } else {
                history.forEachIndexed { index, entry ->
                    HistoryCard(entry = entry, onClick = { onOpenReplay(index) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) { Text(stringResource(id = R.string.azione_indietro)) }
        }
    }
}

@Composable
internal fun historyModeLabel(entry: HistoryEntry): String = when (entry.gameMode) {
    GameMode.VS_AI -> stringResource(
        id = R.string.history_vs_ai,
        when (entry.difficulty) {
            Difficulty.MEDIO -> stringResource(id = R.string.difficolta_medio)
            Difficulty.DIFFICILE -> stringResource(id = R.string.difficolta_difficile)
            else -> stringResource(id = R.string.difficolta_facile)
        },
    )
    GameMode.PASS_AND_PLAY -> stringResource(id = R.string.stats_pass_play)
    GameMode.NEARBY -> entry.opponentName
        ?.let { stringResource(id = R.string.online_vs, it) }
        ?: stringResource(id = R.string.stats_nearby)
    GameMode.ONLINE -> entry.opponentName
        ?.let { stringResource(id = R.string.online_vs, it) }
        ?: stringResource(id = R.string.stats_online)
}

@Composable
internal fun historyOutcomeLabel(entry: HistoryEntry): Pair<String, androidx.compose.ui.graphics.Color> {
    val lime = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.secondary
    val cyan = MaterialTheme.colorScheme.tertiary
    val neutral = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    return when {
        entry.winnerMark == SuperTrisRules.EMPTY ->
            stringResource(id = R.string.fine_partita_pareggio) to neutral
        entry.gameMode == GameMode.PASS_AND_PLAY ->
            if (entry.winnerMark == SuperTrisRules.X) {
                stringResource(id = R.string.history_vince_x) to cyan
            } else {
                stringResource(id = R.string.history_vince_o) to amber
            }
        entry.localWon == true -> stringResource(id = R.string.history_vittoria) to lime
        else -> stringResource(id = R.string.history_sconfitta) to amber
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }
    val (outcomeText, outcomeColor) = historyOutcomeLabel(entry)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = historyModeLabel(entry),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = outcomeText,
                style = MaterialTheme.typography.bodyMedium,
                color = outcomeColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatter.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}
