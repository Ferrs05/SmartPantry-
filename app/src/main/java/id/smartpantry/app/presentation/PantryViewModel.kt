package id.smartpantry.app.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.smartpantry.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PantryState(val photo: Bitmap? = null, val detections: List<Detection> = emptyList(),
    val selected: Set<Int> = emptySet(), val busy: Boolean = false, val error: String? = null,
    val inferenceMs: Long? = null, val totalMs: Long? = null,
    val matches: List<RecipeMatch> = emptyList(), val detail: Recipe? = null)

class PantryViewModel(private val detector: IngredientDetector, private val repository: RecipeRepository): ViewModel() {
    private val mutable=MutableStateFlow(PantryState())
    val state=mutable.asStateFlow()
    private val homeRecipes=MutableStateFlow<List<Recipe>>(emptyList())
    val featured=homeRecipes.asStateFlow()
    init {
        viewModelScope.launch {
            try { homeRecipes.value=listOf(1824L,9000L).mapNotNull { repository.getRecipe(it) } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.update { it.copy(error="Resep belum bisa dibuka. Coba kembali ke beranda.") } }
        }
    }
    private var currentJob: Job?=null
    private var generation=0

    fun processPhoto(load: suspend () -> Bitmap) {
        currentJob?.cancel()
        val request=++generation
        mutable.value=PantryState(busy=true)
        currentJob=viewModelScope.launch {
            try {
                val start=System.nanoTime()
                val bitmap=withContext(Dispatchers.IO) { load() }
                if (request != generation) return@launch
                mutable.update { it.copy(photo=bitmap) }
                val pixels=withContext(Dispatchers.Default) {
                    IntArray(bitmap.width*bitmap.height).also { bitmap.getPixels(it,0,bitmap.width,0,0,bitmap.width,bitmap.height) }
                }
                val result=detector.detect(RgbImage(bitmap.width,bitmap.height,pixels))
                if (request == generation) mutable.update { it.copy(busy=false,detections=result.detections,
                    selected=result.detections.map { d -> d.classId }.toSet(),inferenceMs=result.inferenceMs,
                    totalMs=(System.nanoTime()-start)/1_000_000) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if(request==generation) mutable.update { it.copy(busy=false,error="Foto belum bisa diproses: ${e.message ?: "coba foto lain"}") } }
        }
    }
    fun toggleIngredient(id: Int) {
        require(id in Ingredients.names.indices)
        generation++
        currentJob?.cancel()
        mutable.update { it.copy(selected=if(id in it.selected) it.selected-id else it.selected+id,matches=emptyList(),busy=false) }
    }
    fun findRecipes() {
        currentJob?.cancel()
        val request=++generation
        val selected=mutable.value.selected
        mutable.update { it.copy(busy=true,error=null) }
        currentJob=viewModelScope.launch {
            try { val matches=RecommendRecipes(repository)(selected); if(request==generation) mutable.update { it.copy(busy=false,matches=matches) } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(request==generation) mutable.update { it.copy(busy=false,error="Resep belum bisa dibuka: ${e.message}") } }
        }
    }
    fun openRecipe(id: Long) {
        val request=++generation
        mutable.update { it.copy(detail=null,busy=true,error=null) }
        currentJob?.cancel()
        currentJob=viewModelScope.launch {
            try { val recipe=repository.getRecipe(id); if(request==generation) mutable.update { it.copy(detail=recipe,busy=false,
                error=if(recipe==null) "Resep tidak ditemukan" else null) } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(request==generation) mutable.update { it.copy(busy=false,error="Resep belum bisa dibuka: ${e.message}") } }
        }
    }
    fun error(message: String) { mutable.update { it.copy(error=message) } }
    fun reset() { generation++; currentJob?.cancel(); mutable.value=PantryState() }
}
