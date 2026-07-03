package it.claudio.supertris.ui.twoplayers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.ui.components.BrandLockup
import it.claudio.supertris.ui.components.SuperBackground

@Composable
fun TwoPlayersScreen(
    onBack: () -> Unit,
    onPassAndPlay: () -> Unit,
    onNearby: () -> Unit,
    onOnline: () -> Unit,
    onlineEnabled: Boolean,
) {
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
                text = stringResource(id = R.string.menu_gioca_in_2),
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
                ModeCard(
                    title = stringResource(id = R.string.two_players_stesso_telefono),
                    subtitle = stringResource(id = R.string.two_players_stesso_telefono_hint),
                    onClick = onPassAndPlay,
                )
                Spacer(modifier = Modifier.height(10.dp))
                ModeCard(
                    title = stringResource(id = R.string.two_players_due_telefoni),
                    subtitle = stringResource(id = R.string.two_players_due_telefoni_hint),
                    onClick = onNearby,
                )
                Spacer(modifier = Modifier.height(10.dp))
                ModeCard(
                    title = stringResource(id = R.string.two_players_online),
                    subtitle = if (onlineEnabled) {
                        stringResource(id = R.string.two_players_online_hint)
                    } else {
                        stringResource(id = R.string.two_players_online_disabled_hint)
                    },
                    onClick = { if (onlineEnabled) onOnline() },
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBack,
                ) { Text(stringResource(id = R.string.azione_indietro)) }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
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
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
    }
}
