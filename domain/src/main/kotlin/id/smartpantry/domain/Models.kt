package id.smartpantry.domain

object Ingredients {
    val names = listOf("Ayam", "Bawang Merah", "Bawang Putih", "Cabai", "Tomat", "Telur",
        "Tahu", "Tempe", "Ikan", "Jagung", "Wortel", "Bayam", "Daun Bawang",
        "Kacang Panjang", "Kangkung", "Kol", "Terong", "Kentang")
}
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun iou(other: Box): Float {
        val intersection = (minOf(right, other.right) - maxOf(left, other.left)).coerceAtLeast(0f) *
            (minOf(bottom, other.bottom) - maxOf(top, other.top)).coerceAtLeast(0f)
        val union = (right-left).coerceAtLeast(0f)*(bottom-top).coerceAtLeast(0f) +
            (other.right-other.left).coerceAtLeast(0f)*(other.bottom-other.top).coerceAtLeast(0f) - intersection
        return if (union > 0) intersection/union else 0f
    }
}
data class Detection(val classId: Int, val confidence: Float, val box: Box)
data class RgbImage(val width: Int, val height: Int, val pixels: IntArray) {
    init { require(width > 0 && height > 0 && pixels.size == width * height) }
}
data class DetectionResult(val detections: List<Detection>, val inferenceMs: Long)
interface IngredientDetector : AutoCloseable {
    suspend fun detect(image: RgbImage): DetectionResult
}
data class Recipe(val id: Long, val title: String, val ingredientLines: List<String>,
    val steps: List<String>, val category: String, val loves: Int, val sourceUrl: String,
    val ingredientIds: Set<Int>, val uncheckedLines: List<String>)
interface RecipeRepository {
    suspend fun findCandidates(ingredientIds: Set<Int>): List<Recipe>
    suspend fun getRecipe(id: Long): Recipe?
    suspend fun count(): Int
}
data class RecipeMatch(val recipe: Recipe, val matchedIds: Set<Int>, val missingIds: Set<Int>) {
    val coverage: Float get() = matchedIds.size.toFloat() / recipe.ingredientIds.size
}
class RecommendRecipes(private val repository: RecipeRepository) {
    suspend operator fun invoke(ingredients: Set<Int>): List<RecipeMatch> {
        if (ingredients.isEmpty()) return emptyList()
        return repository.findCandidates(ingredients).filter { it.ingredientIds.isNotEmpty() }
            .map { RecipeMatch(it, it.ingredientIds intersect ingredients, it.ingredientIds - ingredients) }
            .filter { it.matchedIds.isNotEmpty() }
            .sortedWith(compareByDescending<RecipeMatch> { it.coverage }
                .thenByDescending { it.matchedIds.size }.thenByDescending { it.recipe.loves }
                .thenBy { it.recipe.id })
    }
}
