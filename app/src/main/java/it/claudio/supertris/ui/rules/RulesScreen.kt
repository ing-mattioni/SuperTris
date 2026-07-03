package it.claudio.supertris.ui.rules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.ui.components.SuperBackground

@Composable
fun RulesScreen(onBack: () -> Unit) {
    SuperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.menu_come_si_gioca),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(18.dp))

            RuleCard(
                title = stringResource(id = R.string.rules_tabellone_titolo),
                body = stringResource(id = R.string.rules_tabellone_testo),
            )
            Spacer(modifier = Modifier.height(12.dp))

            RuleCard(
                title = stringResource(id = R.string.rules_obiettivo_titolo),
                body = stringResource(id = R.string.rules_obiettivo_testo),
            )
            Spacer(modifier = Modifier.height(12.dp))

            RuleCard(
                title = stringResource(id = R.string.rules_regola_titolo),
                body = stringResource(id = R.string.rules_regola_testo),
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                ForcedGridDiagram()
            }
            Spacer(modifier = Modifier.height(12.dp))

            RuleCard(
                title = stringResource(id = R.string.rules_libera_titolo),
                body = stringResource(id = R.string.rules_libera_testo),
            )
            Spacer(modifier = Modifier.height(12.dp))

            RuleCard(
                title = stringResource(id = R.string.rules_strategia_titolo),
                body = stringResource(id = R.string.rules_strategia_testo),
            )
            Spacer(modifier = Modifier.height(12.dp))

            RuleCard(
                title = stringResource(id = R.string.rules_modalita_titolo),
                body = stringResource(id = R.string.rules_modalita_testo),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) { Text(stringResource(id = R.string.azione_indietro)) }
        }
    }
}

@Composable
private fun RuleCard(
    title: String,
    body: String,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
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
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
        )
        extraContent()
    }
}

/**
 * Esempio visivo della regola d'oro: giochi nella cella in alto a destra
 * di una micro-griglia e l'avversario viene mandato nella micro-griglia
 * in alto a destra del tabellone.
 */
@Composable
private fun ForcedGridDiagram() {
    val cyan = MaterialTheme.colorScheme.tertiary
    val lime = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniGrid(markCell = 2, markColor = cyan, highlightCell = null, highlightColor = lime)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.rules_diagramma_tu),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "→",
            style = MaterialTheme.typography.headlineMedium,
            color = lime,
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniGrid(markCell = null, markColor = cyan, highlightCell = 2, highlightColor = lime)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.rules_diagramma_avversario),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MiniGrid(
    markCell: Int?,
    markColor: Color,
    highlightCell: Int?,
    highlightColor: Color,
) {
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)

    Canvas(modifier = Modifier.size(96.dp)) {
        val cell = size.width / 3f

        if (highlightCell != null) {
            val col = highlightCell % 3
            val row = highlightCell / 3
            drawRoundRect(
                color = highlightColor.copy(alpha = 0.22f),
                topLeft = Offset(col * cell + 2.dp.toPx(), row * cell + 2.dp.toPx()),
                size = Size(cell - 4.dp.toPx(), cell - 4.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
            drawRoundRect(
                color = highlightColor,
                topLeft = Offset(col * cell + 2.dp.toPx(), row * cell + 2.dp.toPx()),
                size = Size(cell - 4.dp.toPx(), cell - 4.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        val stroke = 1.5.dp.toPx()
        for (i in 1..2) {
            drawLine(
                color = lineColor,
                start = Offset(i * cell, 0f),
                end = Offset(i * cell, size.height),
                strokeWidth = stroke,
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, i * cell),
                end = Offset(size.width, i * cell),
                strokeWidth = stroke,
            )
        }

        if (markCell != null) {
            val col = markCell % 3
            val row = markCell / 3
            val pad = cell * 0.28f
            val x0 = col * cell + pad
            val y0 = row * cell + pad
            val x1 = (col + 1) * cell - pad
            val y1 = (row + 1) * cell - pad
            val markStroke = 3.dp.toPx()
            drawLine(markColor, Offset(x0, y0), Offset(x1, y1), markStroke, StrokeCap.Round)
            drawLine(markColor, Offset(x1, y0), Offset(x0, y1), markStroke, StrokeCap.Round)
        }
    }
}
