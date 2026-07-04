package it.claudio.supertris.ui.stats

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.data.GameStats
import it.claudio.supertris.data.ModeStats
import it.claudio.supertris.ui.components.SuperBackground

@Composable
fun StatsScreen(
    repo: GameRepository,
    onHistory: () -> Unit,
    onBack: () -> Unit,
) {
    val stats by repo.statsFlow.collectAsState(initial = GameStats())

    SuperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.stats_titolo),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (stats.totalPlayed == 0) {
                StatsPanel {
                    Text(
                        text = stringResource(id = R.string.stats_nessuna),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    )
                }
            } else {
                StatsPanel {
                    SectionTitle(stringResource(id = R.string.stats_vs_ai))
                    AiRow(stringResource(id = R.string.difficolta_facile), stats.aiFacile)
                    AiRow(stringResource(id = R.string.difficolta_medio), stats.aiMedio)
                    AiRow(stringResource(id = R.string.difficolta_difficile), stats.aiDifficile)
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (stats.passAndPlay.played > 0) {
                    StatsPanel {
                        SectionTitle(stringResource(id = R.string.stats_pass_play))
                        StatLine(stringResource(id = R.string.stats_vittorie_x), stats.passAndPlay.xWins)
                        StatLine(stringResource(id = R.string.stats_vittorie_o), stats.passAndPlay.oWins)
                        StatLine(stringResource(id = R.string.stats_pareggi), stats.passAndPlay.draws)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (stats.nearby.played > 0) {
                    StatsPanel {
                        SectionTitle(stringResource(id = R.string.stats_nearby))
                        WldLine(stats.nearby)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (stats.online.played > 0) {
                    StatsPanel {
                        SectionTitle(stringResource(id = R.string.stats_online))
                        WldLine(stats.online)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onHistory,
            ) { Text(stringResource(id = R.string.stats_cronologia)) }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) { Text(stringResource(id = R.string.azione_indietro)) }
        }
    }
}

@Composable
private fun StatsPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                shape = RoundedCornerShape(24.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun AiRow(label: String, stats: ModeStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
        Text(
            text = stringResource(id = R.string.stats_wld, stats.wins, stats.losses, stats.draws),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun WldLine(stats: ModeStats) {
    StatLine(stringResource(id = R.string.stats_vittorie), stats.wins)
    StatLine(stringResource(id = R.string.stats_sconfitte), stats.losses)
    StatLine(stringResource(id = R.string.stats_pareggi), stats.draws)
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
