package it.claudio.supertris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentO,
    tertiary = AccentX,
    background = NearBlack,
    surface = DarkSurface,
    surfaceVariant = ElevatedSurface,
    outline = SurfaceLine,
    onBackground = SoftText,
    onSurface = SoftText,
)

@Composable
fun SuperTrisTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}