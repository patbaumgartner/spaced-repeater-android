package com.patbaumgartner.android.spacedrepeater.ui

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * Screenshot instrumented test for docs / README.
 *
 * Each test method navigates to one screen and captures a PNG via
 * `adb shell screencap`. Screenshots land at:
 *   /sdcard/Pictures/SpacedRepeater/
 *
 * Run on a connected device or emulator:
 *
 *   # Build and install the debug APK
 *   ./gradlew assembleDebug installDebug
 *
 *   # Run screenshot captures
 *   ./gradlew connectedDebugAndroidTest \
 *     -PtestScreenshot=true \
 *     --tests "com.patbaumgartner.android.spacedrepeater.ui.ScreenshotsTest"
 *
 *   # Pull screenshots to the repo
 *   adb pull /sdcard/Pictures/SpacedRepeater/ docs/screenshots/
 *
 * Or run a single screen in isolation:
 *   adb shell am instrument -w ... \
 *     -e class com.patbaumgartner.android.spacedrepeater.ui.ScreenshotsTest#homeScreen
 *
 * Screenshots capture the full screen including system bars so they reflect the
 * real learner experience.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private val outDir: File by lazy {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "SpacedRepeater",
        ).also { it.mkdirs() }
    }

    /**
     * Captures the current screen to [fileName] inside [outDir].
     * Uses `screencap` via Runtime so no app permissions are required.
     */
    private fun capture(fileName: String) {
        val output = File(outDir, fileName)
        val process = Runtime.getRuntime().exec(arrayOf("screencap", "-p", output.absolutePath))
        val exitCode = try { process.waitFor() } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        assertTrue("screencap failed for $fileName (exit $exitCode)", exitCode == 0)
        assertTrue("screenshot not created: ${output.absolutePath}", output.exists())
    }

    // ------------------------------------------------------------------
    // Single JUnit methods — each runnable in isolation via
    //   adb shell am instrument -w ... -e class ...#methodName
    //
    // Tap coordinates are approximate for a 1080×2280 Pixel-5 display.
    // Adjust if your emulator or device has a different resolution.
    // ------------------------------------------------------------------

    @Test
    fun homeScreen() {
        // Home is the launcher destination — the activity opens here.
        device.waitForIdle(2_000)
        capture("01_home.png")
    }

    @Test
    fun importScreen() {
        // Navigate: Home → Import FAB (bottom-right, ~1050×1810 at 1080×2280).
        device.executeShellCommand("input tap 1050 1810")
        device.waitForIdle(1_500)
        capture("02_import.png")
    }

    @Test
    fun statsScreen() {
        // Navigate: Home → stats icon (top-right of app bar, ~980×140).
        device.executeShellCommand("input tap 980 140")
        device.waitForIdle(1_500)
        capture("04_stats.png")
    }

    @Test
    fun settingsScreen() {
        // Navigate: Home → settings icon (top-right, left of stats, ~890×140).
        device.executeShellCommand("input tap 890 140")
        device.waitForIdle(1_500)
        capture("05_settings.png")
    }

    @Test
    fun studyScreen() {
        // Navigate: Home → first deck card (centre-x, ~y=300 to clear header).
        device.executeShellCommand("input tap 540 300")
        device.waitForIdle(2_000)
        capture("03_study.png")
    }

    @Test
    fun studySession_answerRevealed() {
        // From study screen, tap the card to reveal the answer.
        device.executeShellCommand("input tap 540 600")
        device.waitForIdle(1_000)
        capture("06_study_answer.png")
    }
}
