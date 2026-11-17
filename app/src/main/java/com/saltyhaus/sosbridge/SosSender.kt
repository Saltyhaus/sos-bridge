package com.saltyhaus.sosbridge

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

/**
 * The entire job this app exists to do: read config, fire one HTTP request.
 * Shared by the real SOS receiver and MainActivity's "Send Test Alert" button so the
 * test path exercises exactly the same code as a real button press.
 *
 * Must be called off the main thread -- HttpURLConnection throws
 * NetworkOnMainThreadException otherwise, and the receiver is on the main thread when
 * onReceive() runs.
 */
object SosSender {

    data class Result(val success: Boolean, val detail: String)

    fun send(context: Context, triggeredBy: String): Result {
        val config = SosConfig.load(context)
            ?: return Result(false, "no valid config file -- see README, push config.properties first")

        Logger.log(context, "sending alert (triggered_by=$triggeredBy, radio=${config.radioLabel}) -> ${config.url}")

        return try {
            val connection = URL(config.url).openConnection() as HttpURLConnection
            connection.requestMethod = if (config.method == "POST") "POST" else "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000

            if (!config.headerName.isNullOrBlank() && config.headerValue != null) {
                connection.setRequestProperty(config.headerName, config.headerValue)
            }

            if (connection.requestMethod == "POST") {
                connection.doOutput = true
                val bodyBytes = (config.body ?: "").toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.outputStream.use { it.write(bodyBytes) }
            }

            val code = connection.responseCode
            connection.disconnect()

            if (code in 200..299) {
                Logger.log(context, "alert sent OK (HTTP $code)")
                Result(true, "HTTP $code")
            } else {
                Logger.log(context, "alert FAILED (HTTP $code)")
                Result(false, "HTTP $code")
            }
        } catch (e: Exception) {
            Logger.log(context, "alert FAILED (${e.javaClass.simpleName}: ${e.message})")
            Result(false, "${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
