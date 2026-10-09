package id.smartpantry.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

class UiFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrument=InstrumentationRegistry.getInstrumentation()
        instrument.waitForIdleSync()
        Thread.sleep(1500) // Allow emulator compositor to present the frame before taking evidence.
        val bitmap=instrument.uiAutomation.takeScreenshot()
        File(instrument.targetContext.getExternalFilesDir(null),"$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
        bitmap.recycle()
    }
    @Test fun manualIngredientsEnableRecipeSearch() {
        screenshot("real-photo-home")
        compose.onNodeWithTag("manualButton").performScrollTo().performClick()
        compose.onNodeWithTag("searchRecipes").assertIsNotEnabled()
        compose.onNodeWithTag("addIngredients").performScrollTo().performClick()
        compose.onNodeWithTag("ingredient-0").performClick()
        compose.onNodeWithText("Selesai").performClick()
        compose.onNodeWithTag("searchRecipes").assertIsEnabled().performClick()
        compose.waitUntil(30000) {
            compose.onAllNodes(hasText("bahan utama",substring=true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Pilihan resep").assertIsDisplayed()
        screenshot("real-photo-recipes")
        compose.onNodeWithTag("category-Ikan").performClick()
        compose.onNodeWithTag("recipeFilters").performClick()
        compose.onNodeWithTag("completeFilter").performClick()
        screenshot("real-photo-filter")
        compose.onNodeWithText("Tampilkan resep").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("zzzz-no-recipe")
        compose.onNodeWithText("Belum ada resep yang cocok. Coba ubah filter atau bahan pilihan.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("recipeFilters").performScrollTo().performClick()
        compose.onNodeWithText("Hapus filter").performClick()
        compose.onAllNodes(hasText("bahan utama",substring=true)).onFirst().performClick()
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("detailReady")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("real-photo-detail")
        compose.onNodeWithTag("detailList").performScrollToIndex(3)
        compose.onNodeWithText("Bahan lengkap").performScrollTo().assertIsDisplayed()
    }
}
