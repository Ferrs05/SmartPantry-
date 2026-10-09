package id.smartpantry.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.smartpantry.app.presentation.PhotoCatalog
import id.smartpantry.app.presentation.PhotoMemory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoAssetsTest {
    @Test fun photosDecodeOfflineAndCacheStaysWithinBudget()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val catalog=PhotoCatalog.get(context)
        assertTrue(catalog.all().size>=59)
        assertEquals("ayam_mentega",catalog.recipe(1824)?.key)
        assertEquals("tempe_orek",catalog.recipe(9000)?.key)
        assertNull(catalog.recipe(1986)) // Chilli Tuna Puff must not receive a generic fish photograph.
        assertNull(catalog.recipe(Long.MAX_VALUE))
        PhotoMemory.clear()
        for(photo in catalog.all()) {
            assertTrue(photo.sourceUrl.startsWith("https://commons.wikimedia.org/"))
            assertTrue(photo.author.isNotBlank())
            val bitmap=PhotoMemory.load(context,photo,320)
            assertTrue(bitmap.width>0 && bitmap.height>0)
            assertTrue(maxOf(bitmap.width,bitmap.height)<=960)
            assertTrue(PhotoMemory.cachedBytes()<=8*1024*1024)
        }
        PhotoMemory.load(context,requireNotNull(catalog.photo("home")),960)
        assertTrue(PhotoMemory.cachedBytes()<=8*1024*1024)
        PhotoMemory.clear()
        assertEquals(0,PhotoMemory.cachedBytes())
    }
}
