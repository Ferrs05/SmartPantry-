package id.smartpantry.app.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject

internal data class DishPhoto(val key: String, val asset: String, val dishName: String,
    val author: String, val license: String, val licenseUrl: String, val sourceUrl: String, val changes: String)

/** Offline index. Unknown recipes have no photo; there is deliberately no category fallback. */
internal class PhotoCatalog(context: Context) {
    private val sources=JSONObject(context.assets.open("photo_sources.json").bufferedReader().use { it.readText() })
    private val index=JSONObject(context.assets.open("photo_index.json").bufferedReader().use { it.readText() })
    private val records by lazy {
        sources.keys().asSequence().associateWith { key ->
            val it=sources.getJSONObject(key)
            DishPhoto(key,it.getString("asset"),it.getString("dishName"),it.getString("author"),
                it.getString("license"),it.getString("licenseUrl"),it.getString("sourceUrl"),it.getString("changes"))
        }
    }
    fun photo(key: String): DishPhoto? = records[key]
    fun recipe(id: Long): DishPhoto? = photo(index.getJSONObject("recipes").optString(id.toString()))
    fun category(name: String): DishPhoto? = photo(index.getJSONObject("categories").optString(name.lowercase()))
    fun all(): List<DishPhoto> = records.values.toList()
    companion object {
        @Volatile private var instance: PhotoCatalog?=null
        fun get(context: Context): PhotoCatalog = instance ?: synchronized(this) {
            instance ?: PhotoCatalog(context.applicationContext).also { instance=it }
        }
    }
}

/** Pixel memory is bounded separately from the much smaller compressed APK assets. */
internal object PhotoMemory {
    private const val BUDGET=8*1024*1024
    private val cache=object: LruCache<String,Bitmap>(BUDGET) {
        override fun sizeOf(key: String,value: Bitmap)=value.allocationByteCount
    }
    private val decoding=Semaphore(2)
    fun clear() { cache.evictAll() } // Do not recycle: visible Compose images may still own a bitmap.
    fun cachedBytes(): Int=cache.size()
    suspend fun load(context: Context,photo: DishPhoto,target: Int): Bitmap = withContext(Dispatchers.IO) {
        val key="${photo.asset}:$target"
        cache.get(key) ?: decoding.withPermit {
            cache.get(key) ?: run {
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                context.assets.open(photo.asset).use { BitmapFactory.decodeStream(it,null,bounds) }
                var sample=1
                while(maxOf(bounds.outWidth,bounds.outHeight)/(sample*2)>=target) sample*=2
                val options=BitmapFactory.Options().apply { inSampleSize=sample; inPreferredConfig=Bitmap.Config.ARGB_8888 }
                val bitmap=context.assets.open(photo.asset).use { requireNotNull(BitmapFactory.decodeStream(it,null,options)) }
                cache.put(key,bitmap)
                bitmap
            }
        }
    }
}

@Composable internal fun DishPhotoImage(photo: DishPhoto,modifier: Modifier=Modifier,target: Int=320) {
    val context=LocalContext.current.applicationContext
    val bitmap by key(photo.asset,target) {
        // A reused list slot must never retain another recipe's image while decoding.
        produceState<Bitmap?>(null) { value=PhotoMemory.load(context,photo,target) }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        bitmap?.let { Image(it.asImageBitmap(),"Foto ${photo.dishName}",Modifier.matchParentSize(),contentScale=ContentScale.Crop) }
    }
}
