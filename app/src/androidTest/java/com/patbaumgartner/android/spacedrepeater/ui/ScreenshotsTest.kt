package com.patbaumgartner.android.spacedrepeater.ui

import android.os.Environment
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.data.local.deck.DeckDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Screenshot instrumented test for docs / README.
 *
 * Each test method navigates to one screen and captures a PNG via
 * UiDevice.takeScreenshot (uiautomator). Screenshots land at:
 *   /sdcard/Android/data/com.patbaumgartner.android.spacedrepeater/files/Pictures/SpacedRepeater/
 * (the app's external files dir — writable without storage permissions)
 *
 * The test seeds a **synthetic** deck (no private names or faces) directly into
 * the app database so every screen has content to render and the screenshots
 * are reproducible without the private sample deck.
 *
 * Run on a connected device or emulator:
 *
 *   # Build and install the debug APK
 *   ./gradlew assembleDebug installDebug
 *
 *   # Run screenshot captures
 *   ./gradlew connectedDebugAndroidTest \
 *     --tests "com.patbaumgartner.android.spacedrepeater.ui.ScreenshotsTest"
 *
 *   # Pull screenshots to the repo
 *   ./scripts/pull-screenshots.sh
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

    /**
     * The instrumentation app's own external files directory. Writable without
     * any permission on all supported API levels (scoped-storage safe), unlike
     * the shared Pictures directory.
     */
    private val outDir: File by lazy {
        File(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "SpacedRepeater",
        ).also { it.mkdirs() }
    }

    /** Resets the database, seeds a synthetic deck (no private data), and
     * launches the app so every screen has content to render. Runs before
     * each test. */
    @Before
    fun seedSyntheticDeck() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(
            context,
            SpacedRepeaterDatabase::class.java,
            "spaced_repeater.db",
        ).build()
        try {
            db.clearAllTables()
            val deckDao = db.deckDao()
            val cardDao = db.cardDao()
            val stateDao = db.cardStateDao()
            val now = System.currentTimeMillis()
            val deckId = deckDao.insert(
                DeckEntity(
                    name = "Fixture Deck (Synthetic)",
                    description = "Synthetic deck used for screenshot captures; no private data.",
                    createdAt = now,
                ),
            )
            val cardId = cardDao.insertAll(
                listOf(
                    CardEntity(
                        deckId = deckId,
                        front = "Wie heisst diese Person?\n\n![Portraet](media/card-001.png)",
                        back = "# Answer\n\nSynthetic Name\n\nGruppe: Test / CI.Fixture",
                        createdAt = now,
                    ),
                ),
            ).first()
            stateDao.insertAll(
                listOf(
                    CardStateEntity(cardId = cardId, dueDate = now, phase = "NEW"),
                ),
            )
        } finally {
            db.close()
        }
        launchApp()
    }

    /** Launches MainActivity explicitly — running via `am instrument` alone does
     * not bring any activity to the foreground. Waits generously: on TCG
     * emulators (no KVM) the first frames take minutes. */
    private fun launchApp() {
        device.executeShellCommand(
            "am start -W -n com.patbaumgartner.android.spacedrepeater/.ui.MainActivity",
        )
        device.waitForIdle(30_000)
    }

    /**
     * Captures the current screen to [fileName] inside [outDir].
     *
     * Uses [UiDevice.takeScreenshot] — the instrumentation shell context is
     * allowed to capture the display, while the plain `screencap` binary is
     * not reachable from an app process on enforced (user) builds.
     */
    private fun capture(fileName: String) {
        val output = File(outDir, fileName)
        val success = device.takeScreenshot(output, 1.0f, 100)
        assertTrue("screenshot failed for $fileName", success)
        assertTrue("screenshot not created: ${output.absolutePath}", output.exists())
    }

    // ------------------------------------------------------------------
    // Single JUnit methods — each runnable in isolation via
    //   adb shell am instrument -w .../androidx.test.runner.AndroidJUnitRunner \
    //     -e class ...#methodName
    //
    // Navigation uses UiSelector (text / content-description) so it works on
    // any display resolution. Waits are generous because TCG emulators
    // (no KVM) render slowly.
    // ------------------------------------------------------------------

    /** Taps the first visible node matching [selector], waiting up to [timeoutMs]. */
    private fun tap(selector: BySelector, timeoutMs: Long = 60_000) {
        val obj = device.wait(Until.findObject(selector), timeoutMs)
            ?: error("no node matched $selector within ${timeoutMs}ms")
        obj.click()
        device.waitForIdle(5_000)
    }

    @Test
    fun homeScreen() {
        // Home is the launcher destination — the activity opens here.
        device.waitForIdle(10_000)
        capture("01_home.png")
    }

    @Test
    fun importScreen() {
        // Navigate: Home → Import FAB (content description "Import deck").
        tap(By.desc("Import deck"))
        capture("02_import.png")
    }

    @Test
    fun statsScreen() {
        // Navigate: Home → stats icon (content description "Progress").
        tap(By.desc("Progress"))
        capture("04_stats.png")
    }

    @Test
    fun settingsScreen() {
        // Navigate: Home → settings icon (content description "Settings").
        tap(By.desc("Settings"))
        capture("05_settings.png")
    }

    @Test
    fun studyScreen() {
        // Navigate: Home → first deck card (the seeded synthetic deck).
        tap(By.text("Fixture Deck (Synthetic)"))
        capture("03_study.png")
    }

    @Test
    fun studySession_answerRevealed() {
        // Navigate: Home → deck → "Reveal answer" button.
        tap(By.text("Fixture Deck (Synthetic)"))
        tap(By.text("Reveal answer"))
        capture("06_study_answer.png")
    }
}
