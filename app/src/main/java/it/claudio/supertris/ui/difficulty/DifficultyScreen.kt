package it.claudio.supertris.ui.difficulty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.ui.components.BrandLockup
import it.claudio.supertris.ui.components.SuperBackground

@Composable
fun DifficultyScreen(
    onBack: () -> Unit,
    onDifficultyChosen: (Difficulty) -> Unit,
) {
    val (selected, setSelected) = remember { mutableStateOf<Difficulty?>(null) }

    SuperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLockup(showTagline = false)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(id = R.string.titolo_scegli_difficolta),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(16.dp),
            ) {
                DifficultyCard(
                    title = stringResource(id = R.string.difficolta_facile),
                    selected = selected == Difficulty.FACILE,
                    onClick = { setSelected(Difficulty.FACILE) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                DifficultyCard(
                    title = stringResource(id = R.string.difficolta_medio),
                    selected = selected == Difficulty.MEDIO,
                    onClick = { setSelected(Difficulty.MEDIO) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                DifficultyCard(
                    title = stringResource(id = R.string.difficolta_difficile),
                    selected = selected == Difficulty.DIFFICILE,
                    onClick = { setSelected(Difficulty.DIFFICILE) },
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onBack,
                    ) { Text(stringResource(id = R.string.azione_indietro)) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = selected != null,
                        onClick = { onDifficultyChosen(selected!!) },
                    ) { Text(stringResource(id = R.string.azione_avvia)) }
                }
            }

            AnimatedVisibility(
                visible = selected == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    modifier = Modifier.padding(top = 14.dp),
                    text = stringResource(id = R.string.hint_seleziona_difficolta),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)

    val border = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    val subtitleRes = if (selected) R.string.stato_selezionata else R.string.hint_tocca_per_selezionare

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(18.dp))
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
    }
}