package com.saltyhaus.sosbridge

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiny rolling log file, same spirit as the "confirmed via each service's own real
 * connection-success log lines, not just systemctl status" habit on the server side --
 * logcat alone is awkward to pull from a radio with no touchscreen, so every send
 * attempt and result also lands in a plain text file you can `adb pull` directly:
 *
 *   adb pull /storage/emulated/0/Android/data/com.saltyhaus.sosbridge/files/sos_bridge.log
 */
object Logger {
    private const val TAG = "SosBridge"
    private const val MAX_BYTES = 200_000L // trim well before this gets unwieldy

    @Synchronized
    fun log(context: Context, message: String) {
        Log.i(TAG, message)
        try {
            val file = SosConfig.logFile(context)
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            file.appendText("[$stamp] $message\n")
            if (file.length() > MAX_BYTES) {
                val tail = file.readText().takeLast((MAX_BYTES / 2).toInt())
                file.writeText(tail)
            }
        } catch (e: Exception) {
            Log.w(TAG, "could not write log file: ${e.message}")
        }
    }
}
