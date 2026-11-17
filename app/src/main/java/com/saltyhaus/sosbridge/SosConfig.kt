package com.saltyhaus.sosbridge

import android.content.Context
import java.io.File

/**
 * Everything this app needs to know -- URL, method, auth header, body -- lives in a
 * plain key=value file on the device's own external-files dir, NOT in this source
 * code. That means:
 *
 *  - the shared-secret auth header value never has to be typed into chat or committed
 *    to a repo if you ever publish this app's source the way you did dmr-relay
 *  - each radio (radio1's, radio2's) just gets its own config file pushed via adb,
 *    same as each had its own separate MacroDroid macro before
 *
 * Path on the device: /storage/emulated/0/Android/data/com.saltyhaus.sosbridge/files/config.properties
 * (that's what getExternalFilesDir(null) resolves to on a single-user Android 10
 * device -- no storage permission needed for the app's own external dir).
 *
 * File format, one key=value per line, '#' starts a comment, blank lines ignored:
 *
 *   url=https://alerts.example.com/sos-alert
 *   method=POST
 *   header_name=X-Example-Secret
 *   header_value=REPLACE_ME
 *   body=radio=radio1
 *   radio_label=radio1
 */
data class SosConfig(
    val url: String,
    val method: String,
    val headerName: String?,
    val headerValue: String?,
    val body: String?,
    val radioLabel: String
) {
    companion object {
        private const val FILE_NAME = "config.properties"

        fun configFile(context: Context): File =
            File(context.getExternalFilesDir(null), FILE_NAME)

        fun logFile(context: Context): File =
            File(context.getExternalFilesDir(null), "sos_bridge.log")

        /** Returns null (and logs why) if the config file is missing or incomplete. */
        fun load(context: Context): SosConfig? {
            val file = configFile(context)
            if (!file.exists()) {
                Logger.log(context, "ERROR: config file not found at ${file.absolutePath}")
                return null
            }

            val map = mutableMapOf<String, String>()
            file.readLines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val idx = line.indexOf('=')
                if (idx <= 0) return@forEach
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                map[key] = value
            }

            val url = map["url"]
            if (url.isNullOrBlank()) {
                Logger.log(context, "ERROR: config missing required 'url' key")
                return null
            }

            return SosConfig(
                url = url,
                method = (map["method"] ?: "GET").uppercase(),
                headerName = map["header_name"],
                headerValue = map["header_value"],
                body = map["body"],
                radioLabel = map["radio_label"] ?: "unknown"
            )
        }
    }
}
