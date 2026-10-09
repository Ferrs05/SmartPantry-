package id.smartpantry.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.FileInputStream

class CameraFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun emulatorCameraCaptureReachesDetectionResults() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} android.permission.CAMERA")
            .use { fd -> FileInputStream(fd.fileDescriptor).use { it.readBytes() } }
        compose.onNodeWithTag("cameraButton").performClick()
        compose.waitUntil(30000) {
            compose.onAllNodes(hasText("Ambil foto")).fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.Disabled)==null
            }
        }
        compose.onNodeWithText("Ambil foto").performClick()
        compose.waitUntil(60000) {
            compose.onAllNodes(hasTestTag("scanComplete")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Bahan kamu").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("errorMessage")).assertCountEquals(0)
    }
}
