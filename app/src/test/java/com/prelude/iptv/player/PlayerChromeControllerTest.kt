package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerChromeControllerTest {
    @Test
    fun showRendersExpectedLiftAndAutoHides() {
        val scheduler = TestScheduler()
        val host = TestHost()
        val controller = controller(scheduler, host)

        controller.show(isTv = false, isFullscreen = true, isLive = true, userSeeking = false)

        assertTrue(controller.isVisible)
        assertEquals(true, host.overlayVisible)
        assertEquals(88, host.subtitleLift)
        assertEquals(listOf(2_000L), scheduler.activeDelays())

        scheduler.runNext()
        assertFalse(controller.isVisible)
        assertEquals(false, host.overlayVisible)
        assertEquals(10, host.subtitleLift)
    }

    @Test
    fun rearmCancelsPreviousAutoHide() {
        val scheduler = TestScheduler()
        val host = TestHost()
        val controller = controller(scheduler, host)

        controller.show(false, false, false, false)
        val first = scheduler.tasks.single()
        controller.armHide(false, true)

        assertTrue(first.cancelled)
        assertEquals(listOf(5_000L), scheduler.activeDelays())
    }

    @Test
    fun tvFocusRetriesAreBoundedAndStopAfterSuccess() {
        val scheduler = TestScheduler()
        val host = TestHost(focusSuccessOnAttempt = 3)
        val controller = controller(scheduler, host, maxFocusRetries = 6)

        controller.show(isTv = true, isFullscreen = true, isLive = true, userSeeking = false)
        repeat(3) { scheduler.runNext() }

        assertEquals(3, host.focusRequests)
        assertTrue(controller.isVisible)
    }


    @Test
    fun focusRetriesStopAtConfiguredBound() {
        val scheduler = TestScheduler()
        val host = TestHost()
        val controller = controller(scheduler, host, maxFocusRetries = 2)

        controller.show(isTv = true, isFullscreen = true, isLive = true, userSeeking = false)
        repeat(3) { scheduler.runNext() }

        assertEquals(3, host.focusRequests)
        assertTrue(scheduler.activeDelays().all { it >= 1_000L })
    }

    @Test
    fun subtitleLiftMatrixPreservesLegacyLayout() {
        assertEquals(88, PlayerChromeController.subtitleLiftDp(isFullscreen = true, isLive = true))
        assertEquals(112, PlayerChromeController.subtitleLiftDp(isFullscreen = true, isLive = false))
        assertEquals(14, PlayerChromeController.subtitleLiftDp(isFullscreen = false, isLive = true))
        assertEquals(46, PlayerChromeController.subtitleLiftDp(isFullscreen = false, isLive = false))
    }

    @Test
    fun validExistingFocusSkipsFocusRequest() {
        val scheduler = TestScheduler()
        val host = TestHost(hasValidFocus = true)
        val controller = controller(scheduler, host)

        controller.show(isTv = true, isFullscreen = true, isLive = false, userSeeking = false)
        scheduler.runNext() // immediate focus attempt

        assertEquals(0, host.focusRequests)
    }

    @Test
    fun hideCancelsPendingFocusAndAutoHide() {
        val scheduler = TestScheduler()
        val host = TestHost()
        val controller = controller(scheduler, host)

        controller.show(isTv = true, isFullscreen = true, isLive = true, userSeeking = false)
        controller.hide()

        assertTrue(scheduler.tasks.all { it.cancelled })
        assertFalse(controller.isVisible)
    }

    @Test
    fun replacingStatusCancelsOldTimer() {
        val scheduler = TestScheduler()
        val host = TestHost()
        val controller = controller(scheduler, host)

        controller.showStatus("first", 1000)
        val firstTimer = scheduler.tasks.single()
        controller.showStatus("second", 3000)

        assertTrue(firstTimer.cancelled)
        assertEquals("second", host.currentStatus)
        scheduler.runNext()
        assertEquals(null, host.currentStatus)
    }

    @Test
    fun disposePreventsLateCallbacks() {
        val scheduler = TestScheduler()
        val host = TestHost()
        val controller = controller(scheduler, host)

        controller.show(true, true, true, false)
        controller.showStatus("message", 1000)
        controller.dispose()
        scheduler.runUntilIdle()

        assertTrue(scheduler.tasks.all { it.cancelled })
        assertEquals("message", host.currentStatus)
    }

    private fun controller(
        scheduler: TestScheduler,
        host: TestHost,
        maxFocusRetries: Int = 6,
    ) = PlayerChromeController(
        scheduler = scheduler,
        host = host,
        autoHideDelayMs = { _, seeking -> if (seeking) 5_000L else 2_000L },
        focusRetryDelayMs = 32L,
        maxFocusRetries = maxFocusRetries,
    )

    private class TestHost(
        private val focusSuccessOnAttempt: Int = Int.MAX_VALUE,
        var hasValidFocus: Boolean = false,
    ) : PlayerChromeController.Host {
        var overlayVisible: Boolean? = null
        var subtitleLift: Int? = null
        var currentStatus: String? = null
        var focusRequests = 0

        override fun setOverlayVisible(visible: Boolean) { overlayVisible = visible }
        override fun setSubtitleLift(liftDp: Int) { subtitleLift = liftDp }
        override fun canRestoreTvFocus(): Boolean = true
        override fun hasValidFocus(): Boolean = hasValidFocus
        override fun requestPreferredFocus(): Boolean {
            focusRequests++
            return focusRequests >= focusSuccessOnAttempt
        }
        override fun setStatus(message: String?) { currentStatus = message }
    }

    private class TestScheduler : PlayerChromeController.Scheduler {
        val tasks = mutableListOf<Task>()
        private var nowMs = 0L

        override fun schedule(delayMs: Long, task: () -> Unit): PlayerChromeController.ScheduledTask =
            Task(delayMs, nowMs + delayMs, task).also(tasks::add)

        fun activeDelays(): List<Long> = tasks.filterNot { it.cancelled || it.ran }.map { it.dueAtMs - nowMs }

        fun runNext() {
            val task = tasks.filterNot { it.cancelled || it.ran }.minByOrNull { it.dueAtMs } ?: return
            nowMs = task.dueAtMs
            task.ran = true
            task.block()
        }

        fun runUntilIdle(limit: Int = 100) {
            repeat(limit) {
                if (tasks.none { !it.cancelled && !it.ran }) return
                runNext()
            }
        }

        class Task(val delayMs: Long, val dueAtMs: Long, val block: () -> Unit) : PlayerChromeController.ScheduledTask {
            var cancelled = false
            var ran = false
            override fun cancel() { cancelled = true }
        }
    }
}
