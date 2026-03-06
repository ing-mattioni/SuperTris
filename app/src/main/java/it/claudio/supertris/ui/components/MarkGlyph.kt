package it.claudio.supertris.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import it.claudio.supertris.core.SuperTrisRules

@Composable
fun MarkGlyph(
    mark: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }

    LaunchedEffect(mark) {
        if (mark == SuperTrisRules.EMPTY) {
            anim.snapTo(0f)
        } else {
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            )
        }
    }

    Canvas(modifier = modifier) {
        val p = anim.value
        if (mark == SuperTrisRules.EMPTY) return@Canvas

        val strokeWidth = size.minDimension * 0.14f
        val pad = strokeWidth * 0.9f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        when (mark) {
            SuperTrisRules.X -> {
                val s1 = Offset(pad, pad)
                val e1 = Offset(size.width - pad, size.height - pad)
                val s2 = Offset(size.width - pad, pad)
                val e2 = Offset(pad, size.height - pad)

                val t1 = (p * 2f).coerceIn(0f, 1f)
                val t2 = ((p - 0.5f) * 2f).coerceIn(0f, 1f)

                drawLine(
                    color = color,
                    start = s1,
                    end = lerp(s1, e1, t1),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = s2,
                    end = lerp(s2, e2, t2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            SuperTrisRules.O -> {
                val sweep = 360f * p
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - 2 * pad, size.height - 2 * pad),
                    style = stroke,
                )
            }
        }
    }
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset {
    return Offset(
        x = a.x + (b.x - a.x) * t,
        y = a.y + (b.y - a.y) * t,
    )
}