package it.claudio.supertris.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import it.claudio.supertris.ui.theme.AccentO
import it.claudio.supertris.ui.theme.AccentPrimary
import it.claudio.supertris.ui.theme.AccentX
import it.claudio.supertris.ui.theme.DarkSurface
import it.claudio.supertris.ui.theme.NearBlack

@Composable
fun SuperBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bg = Brush.verticalGradient(
        colors = listOf(NearBlack, DarkSurface),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val step = w / 7f

            drawCircle(
                color = AccentX.copy(alpha = 0.11f),
                radius = w * 0.62f,
                center = Offset(w * 0.12f, h * 0.18f),
            )
            drawCircle(
                color = AccentO.copy(alpha = 0.08f),
                radius = w * 0.68f,
                center = Offset(w * 0.94f, h * 0.24f),
            )
            drawCircle(
                color = AccentPrimary.copy(alpha = 0.07f),
                radius = w * 0.98f,
                center = Offset(w * 0.54f, h * 1.08f),
            )

            for (index in -2..10) {
                val x = index * step
                drawLine(
                    color = Color.White.copy(alpha = 0.025f),
                    start = Offset(x, 0f),
                    end = Offset(x - h * 0.18f, h),
                    strokeWidth = 2f,
                )
            }

            drawCircle(
                color = AccentPrimary.copy(alpha = 0.09f),
                radius = w * 0.12f,
                center = Offset(w * 0.82f, h * 0.78f),
                style = Stroke(width = w * 0.012f),
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, NearBlack.copy(alpha = 0.62f)),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.92f,
                ),
            )
        }

        content()
    }
}