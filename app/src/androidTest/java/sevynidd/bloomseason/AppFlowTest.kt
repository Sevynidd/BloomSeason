package sevynidd.bloomseason

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Rule
import org.junit.Test

/** Kernabläufe der Oberfläche auf einem Gerät oder Emulator. */
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun openTab(name: String) {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithContentDescription(name).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription(name).performClick()
    }

    @Test
    fun tabsShowTheirScreens() {
        openTab("Statistik")
        rule.onNodeWithText("Zykluslängen").assertIsDisplayed()
        openTab("Einstellungen")
        rule.onNodeWithText("Backup-Ordner").performScrollTo().assertIsDisplayed()
        openTab("Kalender")
        rule.onNodeWithText("Tag bearbeiten").assertIsDisplayed()
    }

    @Test
    fun swipingChangesTheMonth() {
        openTab("Kalender")
        fun title(month: YearMonth) = "${month.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${month.year}"
        val now = YearMonth.now()
        rule.onNodeWithText(title(now)).assertIsDisplayed()
        rule.onNodeWithText("Tag bearbeiten").performTouchInput { swipeLeft() }
        rule.onNodeWithText(title(now.plusMonths(1))).assertIsDisplayed()
        rule.onNodeWithText("Tag bearbeiten").performTouchInput { swipeRight() }
        rule.onNodeWithText(title(now)).assertIsDisplayed()
    }

    @Test
    fun backupDialogAsksForFolderFirst() {
        openTab("Einstellungen")
        rule.onNodeWithText("Backup erstellen").performScrollTo().performClick()
        // Ohne Ordner erscheint kein Passwortdialog, sondern ein Hinweis.
        rule.onNodeWithText("Lege zuerst einen Backup-Ordner fest.").assertIsDisplayed()
    }
}

