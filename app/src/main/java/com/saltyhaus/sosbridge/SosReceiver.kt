package com.saltyhaus.sosbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Replaces the MacroDroid macro: one trigger (this receiver, filtered in the manifest
 * to android.intent.action.SOS.longpress), one action (SosSender.send below).
 *
 * Confirmed from logcat during the original Hot-key Menu investigation: SOS.longpress
 * is a "non-protected" system broadcast, firing from uid android/system -- any app can
 * register a receiver for it with no special permission, which is exactly what makes
 * this approach work without root or vendor cooperation.
 *
 * onReceive() runs on the main thread and the system expects it back quickly, so this
 * uses goAsync() to get a bit more time, does the actual network call on a background
 * thread (required anyway -- HttpURLConnection on the main thread throws), and always
 * calls finish() exactly once, with a hard backstop timer so a hung connection can't
 * leave the PendingResult dangling indefinitely.
 */
class SosReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SOS.longpress") return

        Logger.log(context, "SOS.longpress received")

        val pendingResult = goAsync()
        val executor = Executors.newSingleThreadExecutor()
        val finished = java.util.concurrent.atomic.AtomicBoolean(false)

        fun finishOnce() {
            if (finished.compareAndSet(false, true)) {
                try {
                    pendingResult.finish()
                } catch (_: IllegalStateException) {
                    // already finished -- fine
                }
            }
        }

        // Backstop: if the network call somehow hangs past this, finish anyway so the
        // receiver doesn't sit around looking stuck to the system.
        Handler(Looper.getMainLooper()).postDelayed({ finishOnce() }, 15_000)

        executor.execute {
            try {
                SosSender.send(context.applicationContext, triggeredBy = "sos_longpress_broadcast")
            } finally {
                finishOnce()
                executor.shutdown()
            }
        }
    }
}
