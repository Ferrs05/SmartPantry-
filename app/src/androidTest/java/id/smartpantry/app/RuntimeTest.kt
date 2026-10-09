package id.smartpantry.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.smartpantry.data.*
import id.smartpantry.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun recipeDatabaseOpensAndRanksRealRecipes()= runBlocking {
        val db=RecipeDatabase.open(context)
        try {
            val repo=LocalRecipeRepository(db.dao())
            assertEquals(9272,repo.count())
            val matches=RecommendRecipes(repo)(setOf(0,1,2,3))
            assertTrue(matches.isNotEmpty())
            assertTrue(matches.first().coverage>0)
            val recipe=repo.getRecipe(matches.first().recipe.id)!!
            assertTrue(recipe.steps.isNotEmpty())
            assertTrue(recipe.sourceUrl.startsWith("https://cookpad.com/"))
            assertEquals(recipe.ingredientIds.size,recipe.ingredientIds.toSet().size)
        } finally { db.close() }
    }
    @Test fun bundledV2RunsOnCpuAndReturnsValidBounds()= runBlocking {
        val detector=LiteRtIngredientDetector(context)
        try {
            val result=detector.detect(RgbImage(320,240,IntArray(320*240) { 0xff000000.toInt() }))
            assertTrue(result.inferenceMs>=0)
            result.detections.forEach {
                assertTrue(it.classId in 0..17)
                assertTrue(it.confidence in .25f..1f)
                assertTrue(it.box.left>=0 && it.box.right<=320 && it.box.top>=0 && it.box.bottom<=240)
            }
        } finally { detector.close() }
    }
    @Test fun appHasNoInternetPermission() {
        val info=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_PERMISSIONS)
        assertFalse(info.requestedPermissions.orEmpty().contains("android.permission.INTERNET"))
    }
}
