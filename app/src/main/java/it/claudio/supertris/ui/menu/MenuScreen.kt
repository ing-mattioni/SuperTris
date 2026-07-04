package it.claudio.supertris.ui.menu

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.ui.components.BrandLockup
import it.claudio.supertris.ui.components.SuperBackground
import it.claudio.supertris.ui.vm.MenuViewModel

@Composable
fun MenuScreen(
    vm: MenuViewModel,
    onNuovaPartita: () -> Unit,
    onContinua: () -> Unit,
    onGiocaIn2: () -> Unit,
    onComeSiGioca: () -> Unit,
    onStatistiche: () -> Unit,
    onEsci: () -> Unit,
) {
    val hasSavedGame by vm.hasInProgressGame.collectAsState()
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(id = R.string.privacy_policy_url)

    val alphaContinue by animateFloatAsState(
        targetValue = if (hasSavedGame) 1f else 0.45f,
        animationSpec = tween(durationMillis = 220),
        label = "continueAlpha",
    )

    SuperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLockup(showTagline = true)
            Spacer(modifier = Modifier.height(30.dp))

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
                    .padding(18.dp),
            ) {
                BigMenuButton(
                    text = stringResource(id = R.string.menu_nuova_partita),
                    onClick = onNuovaPartita,
                )
                Spacer(modifier = Modifier.height(14.dp))
                BigMenuButton(
                    text = stringResource(id = R.string.menu_continua),
                    enabled = hasSavedGame,
                    alpha = alphaContinue,
                    onClick = onContinua,
                )
                Spacer(modifier = Modifier.height(14.dp))
                BigMenuButton(
                    text = stringResource(id = R.string.menu_gioca_in_2),
                    onClick = onGiocaIn2,
                )
                Spacer(modifier = Modifier.height(14.dp))
                BigMenuButton(
                    text = stringResource(id = R.string.menu_come_si_gioca),
                    onClick = onComeSiGioca,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(14.dp))
                BigMenuButton(
                    text = stringResource(id = R.string.menu_statistiche),
                    onClick = onStatistiche,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(14.dp))
                BigMenuButton(
                    text = stringResource(id = R.string.menu_esci),
                    onClick = {
                        activity?.moveTaskToBack(true)
                        onEsci()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = { uriHandler.openUri(privacyPolicyUrl) },
                ) {
                    Text(
                        text = stringResource(id = R.string.menu_privacy_policy),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BigMenuButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    alpha: Float = 1f,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .alpha(alpha),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
