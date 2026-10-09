package id.smartpantry.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.*

class DomainTest {
    private fun <T> runImmediate(block: suspend ()->T): T {
        var result: Result<T>?=null
        block.startCoroutine(object: Continuation<T> {
            override val context=EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result=value }
        })
        return checkNotNull(result).getOrThrow()
    }
    private fun recipe(id: Long,ids: Set<Int>,loves: Int=0)=Recipe(id,"Resep $id",listOf("bahan"),
        listOf("langkah"),"ayam",loves,"https://cookpad.com/id/resep/$id",ids,listOf("garam"))
    private fun repository(vararg values: Recipe)=object: RecipeRepository {
        override suspend fun findCandidates(ingredientIds: Set<Int>)=values.toList()
        override suspend fun getRecipe(id: Long)=values.firstOrNull { it.id==id }
        override suspend fun count()=values.size
    }
    @Test fun classesKeepModelOrder() { assertEquals(18,Ingredients.names.size);assertEquals("Ayam",Ingredients.names[0]);assertEquals("Kentang",Ingredients.names[17]) }
    @Test fun identicalBoxesHaveIouOne() { assertEquals(1f,Box(0f,0f,10f,10f).iou(Box(0f,0f,10f,10f)),.0001f) }
    @Test fun disjointBoxesHaveIouZero() { assertEquals(0f,Box(0f,0f,10f,10f).iou(Box(20f,20f,30f,30f)),0f) }
    @Test fun partialOverlapUsesUnion() { assertEquals(25f/175,Box(0f,0f,10f,10f).iou(Box(5f,5f,15f,15f)),.0001f) }
    @Test fun zeroAreaBoxIsSafe() { assertEquals(0f,Box(0f,0f,0f,0f).iou(Box(0f,0f,0f,0f)),0f) }
    @Test fun landscapeLetterboxRestoresOriginal() {
        val lb=Letterbox(1024,512)
        assertEquals(128,lb.padTop)
        assertEquals(Box(0f,0f,1024f,512f),lb.restore(256f,256f,512f,256f))
    }
    @Test fun portraitLetterboxRestoresOriginal() {
        val lb=Letterbox(512,1024)
        assertEquals(128,lb.padLeft)
        assertEquals(Box(0f,0f,512f,1024f),lb.restore(256f,256f,256f,512f))
    }
    @Test fun boxesAreClampedToImage() { assertEquals(Box(0f,0f,512f,512f),Letterbox(512,512).restore(256f,256f,1000f,1000f)) }
    private fun output(vararg classes: Int): FloatArray {
        val n=classes.size
        return FloatArray(22*n).also { a ->
            classes.forEachIndexed { i,c -> a[i]=256f;a[n+i]=256f;a[2*n+i]=100f;a[3*n+i]=100f;a[(4+c)*n+i]=.9f-i*.1f }
        }
    }
    @Test fun sameClassOverlapsAreSuppressed() { assertEquals(1,YoloDecoder().decode(output(0,0),2,Letterbox(512,512)).size) }
    @Test fun differentClassesAreNotSuppressed() { assertEquals(2,YoloDecoder().decode(output(0,1),2,Letterbox(512,512)).size) }
    @Test fun belowThresholdRemoved() { val a=output(0);a[4]=.24f;assertTrue(YoloDecoder().decode(a,1,Letterbox(512,512)).isEmpty()) }
    @Test fun nonFiniteCoordinatesRemoved() { val a=output(0);a[0]=Float.NaN;assertTrue(YoloDecoder().decode(a,1,Letterbox(512,512)).isEmpty()) }
    @Test fun outputIsChannelMajorPixelCoordinates() { val d=YoloDecoder().decode(output(17),1,Letterbox(512,512)).single(); assertEquals(17,d.classId);assertEquals(Box(206f,206f,306f,306f),d.box) }
    @Test fun emptyIngredientsReturnNoRecipes() { assertTrue(runImmediate { RecommendRecipes(repository(recipe(1,setOf(0))))(emptySet()) }.isEmpty()) }
    @Test fun coverageIsRecipeIngredientsDenominator() { val match=runImmediate { RecommendRecipes(repository(recipe(1,setOf(0,1,2))))(setOf(0,1,9)) }.single();assertEquals(2f/3,match.coverage,.0001f);assertEquals(setOf(2),match.missingIds) }
    @Test fun rankByCoverageThenMatchesThenLoves() {
        val values=runImmediate { RecommendRecipes(repository(recipe(1,setOf(0,1,2)),recipe(2,setOf(0)),
            recipe(3,setOf(0,1),5),recipe(4,setOf(0,1),9)))(setOf(0,1)) }
        assertEquals(listOf(4L,3L,2L,1L),values.map { it.recipe.id })
    }
    @Test fun excludeRecipesWithoutOverlapOrSupportedIngredients() {
        assertTrue(runImmediate { RecommendRecipes(repository(recipe(1,emptySet()),recipe(2,setOf(3))))(setOf(0)) }.isEmpty())
    }
}
