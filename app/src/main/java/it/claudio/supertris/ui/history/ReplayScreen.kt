package it.claudio.supertris.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.core.SuperTrisRules
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.data.rebuildReplayState
import it.claudio.supertris.ui.components.SuperBackground
import it.claudio.supertris.ui.game.SuperBoard

@Composable
fun ReplayScreen(
    repo: GameRepository,
    entryIndex: Int,
    onBack: () -> Unit,
) {
    val history by repo.historyFlow.collectAsState(initial = null)
    val entries = history

    SuperBackground {
        if (entries == null) return@SuperBackground // caricamento lampo da DataStore

        val entry = entries.getOrNull(entryIndex)
        if (entry == null) {
            // Voce sparita (cronologia ripulita): torna indietro con garbo.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.history_vuota),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onBack) { Text(stringResource(id = R.string.azione_indietro)) }
            }
            return@SuperBackground
        }

        val total = entry.moves.size
        var position by rememberSaveable { mutableStateOf(total) }
        val state = remember(entry, position) { rebuildReplayState(entry, position) }
        val (outcomeText, outcomeColor) = historyOutcomeLabel(entry)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            // Intestazione: chi, esito.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = historyModeLabel(entry),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = outcomeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = outcomeColor,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SuperBoard(
                enabled = false,
                forcedMicro = if (position < total) state.forcedMicro else -1,
                cells = state.cells,
                microStatus = state.microStatus,
                celebrationToken = 0,
                resolvedMicro = -1,
                resolvedMark = SuperTrisRules.EMPTY,
                onTap = { _, _ -> },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.replay_mossa, position, total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = position > 0,
                    onClick = { position = 0 },
                ) { Text("⏮") }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = position > 0,
                    onClick = { position-- },
                ) { Text("◀") }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = position < total,
                    onClick = { position++ },
                ) { Text("▶") }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = position < total,
                    onClick = { position = total },
                ) { Text("⏭") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) { Text(stringResource(id = R.string.azione_indietro)) }
        }
    }
}
