package com.prelude.iptv.ui

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.app.UiModeManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import kotlinx.coroutines.delay


/* ---- ίδια χρώματα με το υπόλοιπο app ---- */
private val ShBg      = IptvColors.Background
private val ShBar     = IptvColors.BackgroundRaised
private val ShElev    = IptvColors.Surface
private val ShLine    = IptvColors.DividerStrong
private val ShAccent  = IptvColors.Primary
private val ShAccent2 = IptvColors.Primary
private val ShHi      = IptvColors.TextPrimary
private val ShMid     = IptvColors.TextSecondary
private val ShLo      = IptvColors.TextTertiary


@Composable
fun Modifier.tvFocus(
    shape: Shape = RoundedCornerShape(12.dp),
    tint: Boolean = true,
    scale: Boolean = true
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val focusScale by androidx.compose.animation.core.animateFloatAsState(
        if (focused && scale) motionScale(Motion.TvFocusScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "focusScale"
    )
    val borderColor by animateColorAsState(
        if (focused) Color.White else Color.Transparent,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "focusBorder"
    )
    val fillColor by animateColorAsState(
        if (focused && tint) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "focusFill"
    )
    return this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .graphicsLayer {
            scaleX = focusScale
            scaleY = focusScale
        }
        .background(fillColor, shape)
        .border(width = 2.dp, color = borderColor, shape = shape)
}

/**
 * OK / παρατεταμένο OK από τηλεχειριστήριο.
 *
 * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: το `combinedClickable(onLongClick = …)` ΔΕΝ πυροδοτείται από το
 * D-pad — είναι φτιαγμένο για αφή. Στην τηλεόραση πρέπει να μετρήσουμε μόνοι μας
 * τη διάρκεια KeyDown -> KeyUp. Χωρίς αυτό, το «παρατεταμένο OK» απλά δεν κάνει
 * τίποτα, και το λάθος είναι αόρατο στον κώδικα (φαίνεται σωστό).
 *
 * Αποφασίζουμε στο ΣΗΚΩΜΑ του πλήκτρου, όχι στο πάτημα: αν ανοίξουμε διάλογο ενώ
 * το OK είναι ακόμη πατημένο, το KeyUp παραδίδεται ΣΤΟΝ ΔΙΑΛΟΓΟ και ενεργοποιεί
 * το πρώτο του κουμπί.
 *
 * Το event καταναλώνεται, οπότε δεν διπλοπυροδοτείται τυχόν `clickable` από κάτω.
 */
@Composable
fun Modifier.tvConfirm(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    var pressStartMs by remember { mutableStateOf(0L) }
    return this.onPreviewKeyEvent { event ->
        val isConfirm = event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
            event.key == androidx.compose.ui.input.key.Key.Enter ||
            event.key == androidx.compose.ui.input.key.Key.NumPadEnter
        if (!isConfirm) return@onPreviewKeyEvent false
        when (event.type) {
            androidx.compose.ui.input.key.KeyEventType.KeyDown -> {
                if (pressStartMs == 0L) pressStartMs = android.os.SystemClock.uptimeMillis()
                true
            }
            androidx.compose.ui.input.key.KeyEventType.KeyUp -> {
                val held = if (pressStartMs == 0L) 0L
                else android.os.SystemClock.uptimeMillis() - pressStartMs
                pressStartMs = 0L
                val longEnough = held >= android.view.ViewConfiguration.getLongPressTimeout().toLong()
                if (onLongClick != null && longEnough) onLongClick() else onClick()
                true
            }
            else -> false
        }
    }
}

/**
 * Δίνει ΑΡΧΙΚΟ focus μόλις εμφανιστεί μια οθόνη.
 *
 * Χωρίς αυτό, σε τηλεόραση: όταν ανοίγει νέα οθόνη, το στοιχείο που είχε το focus
 * φεύγει από το composition, το focus χάνεται και το τηλεχειριστήριο δεν κάνει
 * ΤΙΠΟΤΑ — δεν υπάρχει σημείο εκκίνησης για να μετακινηθεί.
 *
 * Χρήση:  val fr = rememberInitialFocus()  ...  Modifier.focusRequester(fr)
 * (πρέπει να μπει σε στοιχείο που είναι focusable, π.χ. έχει .clickable{})
 */
@Composable
fun rememberInitialFocus(enabled: Boolean = true, key: Any? = Unit): FocusRequester {
    // A new destination/key gets a clean requester instead of retaining a node
    // that may already have left the composition.
    val fr = remember(key) { FocusRequester() }
    LaunchedEffect(enabled, key, fr) {
        if (enabled) fr.requestFocusWithRetry()
    }
    return fr
}

/**
 * Requests focus after lazy layout/navigation transitions without assuming the
 * target node exists on the first frame. Useful after a programmatic scroll or
 * when closing an overlay where a one-shot request is otherwise lost.
 */
suspend fun FocusRequester.requestFocusWithRetry(
    attempts: Int = INITIAL_FOCUS_ATTEMPTS,
    retryDelayMs: Long = INITIAL_FOCUS_RETRY_MS,
): Boolean {
    val safeAttempts = attempts.coerceAtLeast(1)
    repeat(safeAttempts) { attempt ->
        withFrameNanos { }
        if (runCatching { requestFocus() }.isSuccess) return true
        if (attempt < safeAttempts - 1) delay(retryDelayMs.coerceAtLeast(0L))
    }
    return false
}

private const val INITIAL_FOCUS_ATTEMPTS = 6
private const val INITIAL_FOCUS_RETRY_MS = 32L

/**
 * Κουμπί εικονιδίου με ορατό focus. Τα σκέτα IconButton είναι μεν focusable,
 * αλλά δεν δείχνουν τίποτα — στην τηλεόραση μοιάζουν «νεκρά».
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = ShHi,
    onClick: () -> Unit
) {
    Box(
        modifier.size(44.dp).clip(CircleShape).tvFocus(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Text action with an explicit TV-visible focus ring for dialogs. */
@Composable
fun TvDialogTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = ShMid,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.tvFocus(RoundedCornerShape(8.dp), tint = false),
    ) {
        Text(label, color = if (enabled) color else color.copy(alpha = 0.45f))
    }
}

/* ================================================ πληκτρολόγιο σε TV ====== */

/** true σε Android TV / μποξάκι (ή γενικά σε συσκευή χωρίς οθόνη αφής). */
@Composable
fun isTvDevice(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        val pm = ctx.packageManager
        val ui = ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val hasTouch = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val uiModeTv = ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        !hasTouch && (hasLeanback || uiModeTv)
    }
}

/**
 * Πληκτρολόγηση σε τηλεόραση: ανοίγει ΜΟΝΟ όταν το ζητήσει ο χρήστης (OK).
 * Έτσι το D-pad περνάει από τα πεδία χωρίς να πετάγεται το πληκτρολόγιο.
 */
