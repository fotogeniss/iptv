package com.prelude.iptv.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prelude.iptv.ui.IptvColors

/**
 * Η «γραμμή-ορίζοντας» του Horizon design (prototypes/redesign_horizon.html).
 *
 * Είναι η κόκκινη φωτεινή γραμμή του logo και ο ΜΟΝΟΣ δείκτης για ό,τι είναι
 * επιλεγμένο, παίζει ή έχει focus: ενεργή καρτέλα, focus στην TV, πρόοδος,
 * κανάλι που παίζει. Κόκκινο στις άκρες, λευκό στο κέντρο, με απαλή λάμψη.
 */
object Horizon {
    val Line = 2.dp
    val Glow = 9.dp
}

/** Το gradient της γραμμής για οριζόντιο τμήμα από [startX] έως [endX]. */
fun horizonBrush(startX: Float, endX: Float, alpha: Float = 1f): Brush =
    Brush.horizontalGradient(
        0f to Color.Transparent,
        0.2f to IptvColors.Primary.copy(alpha = alpha),
        0.5f to Color.White.copy(alpha = alpha),
        0.8f to IptvColors.Primary.copy(alpha = alpha),
        1f to Color.Transparent,
        startX = startX,
        endX = endX,
    )

/** Ζωγραφίζει τη γραμμή με τη λάμψη της, κεντραρισμένη στο [centerY]. */
fun DrawScope.drawHorizon(startX: Float, endX: Float, centerY: Float, alpha: Float = 1f) {
    if (alpha <= 0f || endX <= startX) return
    // Λάμψη: φαρδιά, διάφανη γραμμή στο ίδιο σημείο.
    drawLine(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to IptvColors.Primary.copy(alpha = 0.45f * alpha),
            1f to Color.Transparent,
            startX = startX,
            endX = endX,
        ),
        start = Offset(startX, centerY),
        end = Offset(endX, centerY),
        strokeWidth = Horizon.Glow.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        brush = horizonBrush(startX, endX, alpha),
        start = Offset(startX, centerY),
        end = Offset(endX, centerY),
        strokeWidth = Horizon.Line.toPx(),
        cap = StrokeCap.Round,
    )
}

/**
 * Γραμμή-ορίζοντας κάτω από το στοιχείο. Ζωγραφίζεται ΕΞΩ από τα όρια του
 * στοιχείου ([gap] κάτω από την κάτω ακμή), οπότε δεν αλλάζει το layout ούτε
 * μετακινεί τίποτα. [alpha] 0 = κρυφή· δώσε animated τιμή για ομαλή εμφάνιση.
 */
fun Modifier.horizonBelow(
    alpha: Float,
    widthFraction: Float = 0.84f,
    gap: Dp = 8.dp,
): Modifier = drawWithContent {
    drawContent()
    if (alpha > 0f) {
        val w = size.width * widthFraction
        val x0 = (size.width - w) / 2f
        drawHorizon(x0, x0 + w, size.height + gap.toPx(), alpha)
    }
}

/** Γραμμή-ορίζοντας ΠΑΝΩ από το στοιχείο (π.χ. ενεργή καρτέλα στην κάτω μπάρα). */
fun Modifier.horizonAbove(
    alpha: Float,
    width: Dp = 34.dp,
    inset: Dp = 0.dp,
): Modifier = drawWithContent {
    drawContent()
    if (alpha > 0f) {
        val w = width.toPx().coerceAtMost(size.width)
        val x0 = (size.width - w) / 2f
        drawHorizon(x0, x0 + w, inset.toPx(), alpha)
    }
}

/** Αυτόνομη γραμμή-ορίζοντας, π.χ. κάτω από το λογότυπο. */
@Composable
fun HorizonLine(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(Horizon.Line)) {
        drawHorizon(0f, size.width, size.height / 2f)
    }
}
