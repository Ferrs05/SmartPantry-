package id.smartpantry.app

import android.app.Application
import id.smartpantry.app.presentation.PhotoMemory
import id.smartpantry.data.*
import id.smartpantry.domain.*

class SmartPantryApplication : Application() {
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) PhotoMemory.clear()
    }
    @Suppress("DEPRECATION")
    override fun onLowMemory() { super.onLowMemory(); PhotoMemory.clear() }
    val database by lazy { RecipeDatabase.open(this) }
    val recipes: RecipeRepository by lazy { LocalRecipeRepository(database.dao()) }
    val detector: IngredientDetector by lazy { LiteRtIngredientDetector(this) }
}
