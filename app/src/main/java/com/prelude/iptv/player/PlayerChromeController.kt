package com.prelude.iptv.player

/**
 * Owns player chrome visibility, auto-hide, transient status and TV focus recovery.
 *
 * The controller intentionally knows nothing about Android Views or Handlers. The Activity
 * supplies a tiny host/scheduler adapter, which keeps this behavior deterministic and unit-testable.
 */
class PlayerChromeController(
    private val scheduler: Scheduler,
    private val host: Host,
    private val autoHideDelayMs: (isTv: Boolean, userSeeking: Boolean) -> Long,
    private val focusRetryDelayMs: Long = DEFAULT_FOCUS_RETRY_DELAY_MS,
    private val maxFocusRetries: Int = DEFAULT_MAX_FOCUS_RETRIES,
) {
    interface Scheduler {
        fun schedule(delayMs: Long, task: () -> Unit): ScheduledTask
    }

    fun interface ScheduledTask {
        fun cancel()
    }

    interface Host {
        fun setOverlayVisible(visible: Boolean)
        fun setSubtitleLift(liftDp: Int)
        fun canRestoreTvFocus(): Boolean
        fun hasValidFocus(): Boolean
        fun requestPreferredFocus(): Boolean
        fun setStatus(message: String?)
    }

    var isVisible: Boolean = true
        private set

    private var autoHideTask: ScheduledTask? = null
    private var focusTask: ScheduledTask? = null
    private var statusTask: ScheduledTask? = null
    private var disposed = false
    private var lastTv = false
    private var lastUserSeeking = false

    fun show(
        isTv: Boolean,
        isFullscreen: Boolean,
        isLive: Boolean,
        userSeeking: Boolean,
    ) {
        if (disposed) return
        // Μόλις εμφανίστηκε (ήταν κρυφό); Τότε το focus πρέπει να πάει ΡΗΤΑ στο
        // play — αλλιώς το σύστημα το δίνει στο «Πίσω» και πατιέται κατά λάθος.
        val wasHidden = !isVisible
        lastTv = isTv
        lastUserSeeking = userSeeking
        isVisible = true
        host.setOverlayVisible(true)
        host.setSubtitleLift(subtitleLiftDp(isFullscreen, isLive))
        if (isTv) requestFocus(force = wasHidden) else cancelFocus()
        armHide(isTv, userSeeking)
    }

    fun hide() {
        if (disposed) return
        cancelAutoHide()
        cancelFocus()
        isVisible = false
        host.setOverlayVisible(false)
        host.setSubtitleLift(HIDDEN_SUBTITLE_LIFT_DP)
    }

    fun toggle(
        isTv: Boolean,
        isFullscreen: Boolean,
        isLive: Boolean,
        userSeeking: Boolean,
    ) {
        if (isVisible) hide() else show(isTv, isFullscreen, isLive, userSeeking)
    }

    /** Rearms chrome timeout after a click, seek or focus change. */
    fun armHide(isTv: Boolean = lastTv, userSeeking: Boolean = lastUserSeeking) {
        if (disposed || !isVisible) return
        lastTv = isTv
        lastUserSeeking = userSeeking
        cancelAutoHide()
        val delay = autoHideDelayMs(isTv, userSeeking).coerceAtLeast(0L)
        autoHideTask = scheduler.schedule(delay) { hide() }
    }

    fun requestFocus(force: Boolean = false) {
        if (disposed || !lastTv || !isVisible) return
        cancelFocus()
        scheduleFocusAttempt(attempt = 0, delayMs = 0L, force = force)
    }

    /**
     * Κρατά το overlay ορατό ακυρώνοντας το εκκρεμές auto-hide — π.χ. όσο ο
     * χρήστης σέρνει τη μπάρα ή όσο ένα φύλλο επιλογών είναι ανοιχτό. Το επόμενο
     * [show]/[armHide] ξαναρυθμίζει κανονικά το timeout.
     */
    fun keepVisible() {
        if (disposed) return
        cancelAutoHide()
    }

    fun showStatus(message: String, durationMs: Long) {
        if (disposed || message.isBlank()) return
        statusTask?.cancel()
        host.setStatus(message)
        statusTask = scheduler.schedule(durationMs.coerceAtLeast(0L)) {
            statusTask = null
            host.setStatus(null)
        }
    }

    fun hideStatus() {
        statusTask?.cancel()
        statusTask = null
        if (!disposed) host.setStatus(null)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        cancelAutoHide()
        cancelFocus()
        statusTask?.cancel()
        statusTask = null
    }

    /**
     * [force] = το overlay μόλις εμφανίστηκε: διεκδικούμε ΡΗΤΑ το προτιμώμενο
     * στοιχείο (play), αγνοώντας το τυχόν focus που έδωσε μόνο του το σύστημα —
     * που ήταν το «Πίσω» (πρώτο focusable στην ιεραρχία) και οδηγούσε σε κατά
     * λάθος έξοδο. Χωρίς [force], αν υπάρχει ήδη έγκυρο focus το σεβόμαστε,
     * ώστε να μην «κλέβεται» ενώ ο χρήστης περιηγείται στα κουμπιά.
     */
    private fun scheduleFocusAttempt(attempt: Int, delayMs: Long, force: Boolean = false) {
        focusTask = scheduler.schedule(delayMs) {
            focusTask = null
            if (disposed || !lastTv || !isVisible || !host.canRestoreTvFocus()) return@schedule
            if (!force && host.hasValidFocus()) return@schedule
            if (host.requestPreferredFocus()) return@schedule
            if (attempt < maxFocusRetries) {
                scheduleFocusAttempt(attempt + 1, focusRetryDelayMs, force)
            }
        }
    }

    private fun cancelAutoHide() {
        autoHideTask?.cancel()
        autoHideTask = null
    }

    private fun cancelFocus() {
        focusTask?.cancel()
        focusTask = null
    }

    companion object {
        const val HIDDEN_SUBTITLE_LIFT_DP = 10
        private const val DEFAULT_FOCUS_RETRY_DELAY_MS = 32L
        private const val DEFAULT_MAX_FOCUS_RETRIES = 6

        fun subtitleLiftDp(isFullscreen: Boolean, isLive: Boolean): Int = when {
            isFullscreen && isLive -> 88
            isFullscreen -> 112
            isLive -> 14
            else -> 46
        }
    }
}
