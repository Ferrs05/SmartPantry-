package id.smartpantry.app.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object PhotoLoader {
    fun load(context: Context, uri: Uri): Bitmap {
        val resolver=context.contentResolver
        require(resolver.getType(uri) in setOf("image/jpeg","image/png")) { "Pilih gambar JPEG atau PNG" }
        // Bounded read prevents loading an arbitrarily large attachment into memory.
        val bytes=resolver.openInputStream(uri)?.use { stream ->
            val result=ByteArrayOutputStream()
            val chunk=ByteArray(8192)
            while(result.size()<=20*1024*1024) {
                val count=stream.read(chunk,0,minOf(chunk.size,20*1024*1024+1-result.size()))
                if(count<0) break
                result.write(chunk,0,count)
            }
            result.toByteArray()
        }
            ?: error("Foto tidak dapat dibuka")
        require(bytes.size<=20*1024*1024) { "Ukuran foto maksimal 20 MB" }
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        require(bounds.outWidth>0 && bounds.outHeight>0) { "File gambar rusak" }
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1600) sample*=2
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,
            BitmapFactory.Options().apply { inSampleSize=sample; inPreferredConfig=Bitmap.Config.ARGB_8888 })
            ?: error("Foto tidak dapat dibaca")
        val exif=ExifInterface(ByteArrayInputStream(bytes))
        val matrix=Matrix().apply {
            if(exif.isFlipped) postScale(-1f,1f)
            postRotate(exif.rotationDegrees.toFloat())
        }
        if(matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true).also { if(it!==bitmap) bitmap.recycle() }
    }
    fun orientCamera(bitmap: Bitmap, degrees: Int): Bitmap {
        val rotated=if(degrees==0) bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,
            Matrix().apply { postRotate(degrees.toFloat()) },true).also { if(it!==bitmap) bitmap.recycle() }
        val max=maxOf(rotated.width,rotated.height)
        return if(max<=1600) rotated else Bitmap.createScaledBitmap(rotated,
            (rotated.width*1600f/max).toInt(),(rotated.height*1600f/max).toInt(),true)
            .also { if(it!==rotated) rotated.recycle() }
    }
}
