package com.saltyhaus.sosbridge

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Not the point of this app -- the receiver is. This exists so there's (a) a launcher
 * icon to tap once after install, which is what clears Android's "stopped" flag and
 * lets the receiver actually start receiving broadcasts, and (b) a one-tap way to
 * fire a real test alert without needing to physically hold the SOS button, which is
 * handy for first setup and for checking the box is still reachable from the shack.
 */
class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val testButton = findViewById<Button>(R.id.testButton)

        refreshStatus()

        testButton.setOnClickListener {
            statusText.text = getString(R.string.sending)
            executor.execute {
                val result = SosSender.send(applicationContext, triggeredBy = "manual_test_button")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (result.success) "Sent OK (${result.detail})" else "Failed: ${result.detail}",
                        Toast.LENGTH_LONG
                    ).show()
                    refreshStatus()
                }
            }
        }
    }

    private fun refreshStatus() {
        val configFile = SosConfig.configFile(this)
        val config = SosConfig.load(this)
        val configLine = when {
            config != null -> "Config OK -- radio=\"${config.radioLabel}\", target=${config.url}"
            configFile.exists() -> "Config file exists but is missing 'url' -- check it"
            else -> "No config file yet.\nExpected at:\n${configFile.absolutePath}"
        }

        val logFile = SosConfig.logFile(this)
        val recentLog = if (logFile.exists()) {
            logFile.readLines().takeLast(8).joinToString("\n")
        } else {
            "(no log yet)"
        }

        statusText.text = "$configLine\n\nRecent activity:\n$recentLog"
    }
}
