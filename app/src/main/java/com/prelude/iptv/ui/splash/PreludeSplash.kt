package com.prelude.iptv.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors

private val BRAND_RED = Color(0xFFE50914)

/**
 * Η εισαγωγή που βλέπει ο χρήστης ενώ ανοίγει η εφαρμογή.
 *
 * ΓΙΑΤΙ ΕΔΩ ΚΑΙ ΟΧΙ ΣΤΟ StartupActivity: εκείνο είναι ξεχωριστή οθόνη που κλείνει
 * ΠΡΙΝ ξεκινήσει η φόρτωση — δεν έχει τρόπο να ξέρει πόσο μένει, και γι' αυτό το
 * παλιό intro ήταν βίντεο σταθερού χρόνου με χρονόμετρο ασφαλείας. Ως επίστρωση
 * μέσα στην εφαρμογή, βλέπει την ίδια πρόοδο με τον κατάλογο και φεύγει όταν
 * εκείνος είναι έτοιμος.
 *
 * ΙΔΙΑ ΣΕ ΚΙΝΗΤΟ ΚΑΙ ΤΗΛΕΟΡΑΣΗ: είναι Compose χωρίς κουμπιά και χωρίς focus.
 * Δεν υπάρχει τίποτα να πατηθεί, άρα τίποτα να συμπεριφερθεί διαφορετικά· μόνο
 * τα μεγέθη κλιμακώνονται.
 */
@Composable
fun PreludeSplash(
    /** 0..1 πραγματική πρόοδος, ή null όταν το στάδιο δεν τη γνωρίζει. */
    realProgress: Float?,
    /** Τι κάνει τώρα («Λήψη από την πηγή…»). Κενό = τίποτα να πούμε. */
    stage: String,
    finished: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ---- πρόοδος ----
    var shown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        // Ένα καρέ τη φορά. Το withFrameNanos δένει την κίνηση στην οθόνη αντί σε
        // ένα delay που «τυχαίνει» να είναι κοντά στα 60Hz — σε 120Hz πάνελ το
        // δεύτερο θα έτρεχε στον μισό ρυθμό.
        while (true) {
            withFrameNanos { }
            shown = SplashProgressPolicy.next(shown, realProgress, finished)
        }
    }

    // ---- πότε φεύγει ----
    //
    // ΕΝΑ effect, καμία απόφαση εδώ. Το [SplashProgressPolicy.remainingMs] ξέρει
    // και το κάτω όριο (να προλάβει να ειπωθεί) και το πάνω (να μην κρατά όμηρο
    // την εκκίνηση ένας αργός πάροχος). Η οθόνη περιμένει όσο της πει και ξαναρωτά.
    //
    // Πριν υπήρχαν ΔΥΟ effects με δικά τους χρονόμετρα, και το policy δεν ήξερε
    // για το δεύτερο — το σχόλιό του έλεγε ρητά «δεν υπάρχει μέγιστος χρόνος».
    val start = remember { System.currentTimeMillis() }
    LaunchedEffect(finished) {
        while (true) {
            val wait = SplashProgressPolicy.remainingMs(
                finished = finished,
                visibleMs = System.currentTimeMillis() - start
            )
            if (wait <= 0L) break
            kotlinx.coroutines.delay(wait)
        }
        onDismiss()
    }

    val transition = rememberInfiniteTransition(label = "splash")

    // ---- λάμψη φόντου ----
    val glow by transition.animateFloat(
        initialValue = .9f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    // ---- σύνθεση του «+» ----
    // Ένα μόνο Animatable για τα τέσσερα κομμάτια: κινούνται μαζί, και τέσσερα
    // ξεχωριστά θα ήταν τέσσερις ευκαιρίες να ξεσυγχρονιστούν.
    val assemble = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        assemble.animateTo(1f, tween(1_800, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
    }

    // ---- παλμός, μόνο αφού συναρμολογηθεί ----
    val assembled = assemble.value >= 1f
    val beat = remember { Animatable(1f) }
    LaunchedEffect(assembled) {
        if (!assembled) return@LaunchedEffect
        while (true) {
            beat.animateTo(1.35f, tween(170, easing = FastOutSlowInEasing))
            beat.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
            beat.animateTo(1.2f, tween(170, easing = FastOutSlowInEasing))
            beat.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
            kotlinx.coroutines.delay(720)
        }
    }

    val logoAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        logoAlpha.animateTo(1f, tween(1_200, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
    }

    Box(
        modifier.fillMaxSize().background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().scale(glow)) {
            val radius = size.minDimension * .45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BRAND_RED.copy(alpha = .30f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer {
                alpha = logoAlpha.value
                val s = .85f + .15f * logoAlpha.value
                scaleX = s; scaleY = s
            }
        ) {
            Text(
                "prelude",
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            )
            Spacer(Modifier.width(12.dp))
            AssemblingPlus(
                progress = assemble.value,
                beat = if (assembled) beat.value else 1f
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (stage.isNotBlank()) {
                Text(
                    stage,
                    color = IptvColors.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Box(
                Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = .10f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(shown.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(BRAND_RED, Color(0xFFFF4D4D)))
                        )
                )
            }
        }
    }
}

/**
 * Τα τέσσερα κομμάτια που έρχονται από μακριά και γίνονται «+».
 *
 * Ζωγραφίζεται με [Canvas] και όχι με τέσσερα Box: η θέση κάθε κομματιού είναι
 * υπολογισμός πάνω στην ίδια τιμή προόδου, και σε Canvas αυτό διαβάζεται σαν μία
 * πρόταση αντί για τέσσερα modifier chains που πρέπει να συμφωνούν μεταξύ τους.
 */
@Composable
private fun AssemblingPlus(progress: Float, beat: Float) {
    Canvas(
        Modifier
            .size(50.dp)
            .graphicsLayer { scaleX = beat; scaleY = beat }
    ) {
        val p = progress.coerceIn(0f, 1f)
        val remaining = 1f - p
        val unit = size.minDimension / 50f

        PIECES.forEach { piece ->
            val w = piece.w * unit
            val h = piece.h * unit
            withTransform({
                translate(
                    piece.x * unit + piece.fromX * unit * remaining,
                    piece.y * unit + piece.fromY * unit * remaining
                )
                rotate(piece.spin * remaining, pivot = Offset(w / 2f, h / 2f))
            }) {
                drawRoundRect(
                    color = BRAND_RED.copy(alpha = p),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(3f * unit, 3f * unit)
                )
            }
        }
    }
}

/**
 * Τα τέσσερα κομμάτια, σε μονάδες πλέγματος 50×50.
 *
 * Οι αποστάσεις εκκίνησης είναι μεγάλες επίτηδες: μια σύνθεση που ξεκινά κοντά
 * μοιάζει με τρέμουλο, όχι με συναρμολόγηση.
 */
private data class SplashPiece(
    val w: Float, val h: Float,
    val x: Float, val y: Float,
    val fromX: Float, val fromY: Float,
    val spin: Float,
)

private val PIECES = listOf(
    SplashPiece(12f, 26f, 19f, 0f, -80f, -100f, -90f),   // πάνω
    SplashPiece(12f, 26f, 19f, 24f, 80f, 100f, 90f),     // κάτω
    SplashPiece(26f, 12f, 0f, 19f, -100f, 80f, -120f),   // αριστερά
    SplashPiece(26f, 12f, 24f, 19f, 100f, -80f, 120f),   // δεξιά
)
