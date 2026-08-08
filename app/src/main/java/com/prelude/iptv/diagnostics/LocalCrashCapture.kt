package com.prelude.iptv.diagnostics

import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

internal object LocalCrashCapture {
    private val installed = AtomicBoolean(false)

    fun install(store: LocalDiagnosticStore) {
        if (!installed.compareAndSet(false, true)) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                store.save(DiagnosticRedactor.fromThrowable(throwable, System.currentTimeMillis()))
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }
}
