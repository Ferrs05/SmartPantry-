package id.smartpantry.data

import android.content.Context
import androidx.room.*
import id.smartpantry.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Entity(tableName = "bahan")
data class IngredientEntity(@PrimaryKey val id: Int, val nama: String)
@Entity(tableName = "resep")
data class RecipeEntity(@PrimaryKey val id: Long, val title: String, val ingredientLines: String,
    val steps: String, val category: String, val loves: Int, val sourceUrl: String, val uncheckedLines: String)
@Entity(tableName = "resep_bahan", primaryKeys = ["resepId", "bahanId"],
    foreignKeys = [ForeignKey(entity=RecipeEntity::class,parentColumns=["id"],childColumns=["resepId"],onDelete=ForeignKey.CASCADE),
        ForeignKey(entity=IngredientEntity::class,parentColumns=["id"],childColumns=["bahanId"],onDelete=ForeignKey.CASCADE)],
    indices = [Index("bahanId")])
data class RecipeIngredient(val resepId: Long, val bahanId: Int)
data class RecipeWithIngredients(@Embedded val recipe: RecipeEntity,
    @Relation(parentColumn="id",entityColumn="resepId",entity=RecipeIngredient::class)
    val links: List<RecipeIngredient>)
@Dao interface RecipeDao {
    @Transaction @Query("SELECT * FROM resep WHERE id IN (SELECT resepId FROM resep_bahan WHERE bahanId IN (:ids))")
    suspend fun candidates(ids: List<Int>): List<RecipeWithIngredients>
    @Transaction @Query("SELECT * FROM resep WHERE id=:id")
    suspend fun get(id: Long): RecipeWithIngredients?
    @Query("SELECT COUNT(*) FROM resep") suspend fun count(): Int
}
@Database(entities=[RecipeEntity::class,IngredientEntity::class,RecipeIngredient::class],version=1,exportSchema=true)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun dao(): RecipeDao
    companion object {
        fun open(context: Context) = Room.databaseBuilder(context,RecipeDatabase::class.java,"resep.db")
            .createFromAsset("resep.db").build()
    }
}
class LocalRecipeRepository(private val dao: RecipeDao): RecipeRepository {
    override suspend fun findCandidates(ingredientIds: Set<Int>) = withContext(Dispatchers.IO) {
        if (ingredientIds.isEmpty()) emptyList() else dao.candidates(ingredientIds.toList()).map { it.toDomain() }
    }
    override suspend fun getRecipe(id: Long) = dao.get(id)?.toDomain()
    override suspend fun count() = dao.count()
    private fun RecipeWithIngredients.toDomain() = Recipe(recipe.id,recipe.title,
        recipe.ingredientLines.strings(),recipe.steps.strings(),recipe.category,recipe.loves,
        recipe.sourceUrl,links.map { it.bahanId }.toSet(),recipe.uncheckedLines.strings())
    private fun String.strings(): List<String> = JSONArray(this).let { a -> List(a.length()) { a.getString(it) } }
}
