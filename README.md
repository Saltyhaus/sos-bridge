# SOS Bridge

Minimal replacement for the MacroDroid macro on the TE320s (radio1's and radio2's
radios). One job: catch the firmware's `android.intent.action.SOS.longpress`
broadcast on a 2-second SOS hold, and fire the same HTTP alert to
`sos_alert_bridge.py` that MacroDroid's "HTTP Request" action used to send. No
licensing, no trial, no ads, no third-party app on the safety path at all.

Source is three small Kotlin files plus a manifest -- `SosReceiver.kt` (the
trigger), `SosSender.kt` (the HTTP call), `SosConfig.kt`/`Logger.kt` (config +
plain-text log). `MainActivity.kt` is just a status screen and a manual test
button; it's not required for the receiver to work, it's there for convenience.

Deliberately **zero** external dependencies -- no OkHttp, no AndroidX, nothing
beyond `java.net.HttpURLConnection` and plain framework widgets. That keeps the
build self-contained once the Android SDK itself is in place, and means the
source can go in your public Forgejo/GitHub repo later the same way dmr-relay
did, with no secret baked into it anywhere (see "Configuration" below).

I could not actually assemble the APK myself -- the sandbox this was written in
has no route to Google's SDK repository or Maven Central, only the language
package managers (npm/pip/etc). Everything here is complete and should build
cleanly on your own machine, where that restriction doesn't exist.

## Building it

**Easiest path: Android Studio.** Open this folder (`sos-bridge-app/`) as an
existing project. Studio will offer to download whatever SDK platform/build-tools
it needs automatically. Then Build > Build Bundle(s)/APK(s) > Build APK(s).

**Terminal path**, since that's clearly your normal mode:

```bash
# One-time SDK setup, if you don't already have an Android SDK on this machine.
# Grab the command-line tools zip from https://developer.android.com/studio#command-tools
# and unpack it somewhere, e.g. ~/Android/cmdline-tools/latest/

export ANDROID_HOME=~/Android
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Build. No gradle wrapper is checked in here (couldn't fetch the wrapper jar
# from this sandbox either) -- use your own system Gradle (8.4+) directly, or
# generate a wrapper once with: gradle wrapper --gradle-version 8.7
cd sos-bridge-app
gradle assembleDebug
```

Output lands at `app/build/outputs/apk/debug/app-debug.apk`. It's debug-signed
with Gradle's default debug keystore -- fine for sideloading onto your own
radios, same as everything else you've sideloaded on these TE320s.

## Installing on a TE320

```bash
adb -s <serial> install -r app-debug.apk

# Launch it once -- this clears Android's "stopped" flag, which is the actual
# thing that lets the receiver start catching broadcasts at all. Same bug class
# that bit news.androidtv.launchonboot and com.z3.dmr before.
adb -s <serial> shell monkey -p com.saltyhaus.sosbridge -c android.intent.category.LAUNCHER 1

# Keep it alive in the background, same treatment as the other persistent apps
# on this device:
adb -s <serial> shell dumpsys deviceidle whitelist +com.saltyhaus.sosbridge
adb -s <serial> shell cmd appops set com.saltyhaus.sosbridge RUN_IN_BACKGROUND allow
adb -s <serial> shell cmd appops set com.saltyhaus.sosbridge RUN_ANY_IN_BACKGROUND allow
```

Note what's *not* in that list: no `SYSTEM_ALERT_WINDOW` grant. That was only
needed for `news.androidtv.launchonboot` because it had to call `startActivity()`
from the background to launch Z3DMR. This receiver never starts an activity --
it only makes a network call -- so that particular Android 10 background-start
restriction doesn't apply here at all.

## Configuration (do this on-device, not in chat)

The URL, HTTP method, auth header, and body are read at runtime from a plain
text file on the device, **not** compiled into the app. That's deliberate: the
shared secret your `sos_alert_bridge.py` already expects never needs to be typed
into a chat window or committed anywhere.

On your own machine, create a file matching whatever MacroDroid's HTTP Request
action was already configured with for that specific radio (open the old
MacroDroid macro and copy the exact values across):

```
# config.properties -- one per radio, values must match what
# sos_alert_bridge.py on the Oracle VPS actually expects
url=https://alerts.example.com/sos-alert
method=POST
header_name=<the header name MacroDroid was sending>
header_value=<the real shared secret -- copy from the old MacroDroid macro>
body=radio=radio1
radio_label=radio1
```

Then push it -- **after** the first launch above, since the external-files
directory doesn't exist until the app has run once:

```bash
adb -s <serial> push config.properties \
    /storage/emulated/0/Android/data/com.saltyhaus.sosbridge/files/config.properties
```

radio2's radio gets its own `config.properties` with `radio_label=radio2` (and
whatever body/URL distinguishes the two on the server side) pushed the same way.

## Testing

1. Open the app -- the status screen shows whether `config.properties` loaded
   correctly and the target URL it'll hit.
2. Tap **Send Test Alert**. It runs the exact same code path as a real SOS
   hold. Watch for the "Sent OK" toast, or an error with the real HTTP
   status/exception if something's wrong.
3. Pull the log to double-check instead of trusting the toast alone, same
   habit as everywhere else in this build:
   ```bash
   adb -s <serial> pull /storage/emulated/0/Android/data/com.saltyhaus.sosbridge/files/sos_bridge.log
   ```
4. Do a real cold reboot + a real physical SOS long-press, confirm the alert
   reaches the dashboard exactly like the MacroDroid version did.

Once both radios confirm clean on a real reboot + real button test, MacroDroid
can be uninstalled from both -- it's no longer in the SOS path at all.

## License

GPL-3.0. See [LICENSE](LICENSE).
