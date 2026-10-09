package id.smartpantry.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import id.smartpantry.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class LiteRtIngredientDetector(private val context: Context) : IngredientDetector {
    private val mutex = Mutex()
    private var interpreter: Interpreter? = null
    private var closed = false
    private val config = JSONObject(context.assets.open("android_config.json").bufferedReader().use { it.readText() })
    private val post = config.getJSONObject("postprocessing")
    private val decoder = YoloDecoder(post.getDouble("confidence_threshold").toFloat(),
        post.getDouble("iou_threshold").toFloat(),post.getInt("max_detections"))

    private fun runtime(): Interpreter {
        check(!closed) { "Detector sudah ditutup" }
        return interpreter ?: context.assets.openFd("best.tflite").use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                val mapped=channel.map(FileChannel.MapMode.READ_ONLY,fd.startOffset,fd.declaredLength)
                Interpreter(mapped, Interpreter.Options().setNumThreads(2)).also {
                    require(it.getInputTensor(0).shape().contentEquals(intArrayOf(1,3,512,512))) { "Input model harus NCHW [1,3,512,512]" }
                    require(it.getInputTensor(0).dataType()==DataType.FLOAT32)
                    require(it.getOutputTensor(0).shape().contentEquals(intArrayOf(1,22,5376)))
                    require(it.getOutputTensor(0).dataType()==DataType.FLOAT32)
                    val names=config.getJSONArray("class_names")
                    require(List(names.length()) { n -> names.getString(n) } == Ingredients.names)
                    interpreter=it
                }
            }
        }
    }
    override suspend fun detect(image: RgbImage): DetectionResult = withContext(Dispatchers.Default) {
        mutex.withLock {
            synchronized(this@LiteRtIngredientDetector) {
            val runtime=runtime()
            val transform=Letterbox(image.width,image.height)
            val source=Bitmap.createBitmap(image.pixels,image.width,image.height,Bitmap.Config.ARGB_8888)
            val target=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
            val pixels=IntArray(512*512)
            try {
                val canvas=Canvas(target)
                canvas.drawColor(Color.rgb(114,114,114))
                canvas.drawBitmap(source,null,Rect(transform.padLeft,transform.padTop,
                    transform.padLeft+transform.resizedWidth,transform.padTop+transform.resizedHeight),
                    Paint(Paint.FILTER_BITMAP_FLAG))
                target.getPixels(pixels,0,512,0,0,512,512)
            } finally { source.recycle(); target.recycle() }
            val input=ByteBuffer.allocateDirect(pixels.size*3*4).order(ByteOrder.nativeOrder())
            for (shift in intArrayOf(16,8,0)) for (pixel in pixels) input.putFloat(((pixel shr shift) and 255)/255f)
            input.rewind()
            val output=ByteBuffer.allocateDirect(22*5376*4).order(ByteOrder.nativeOrder())
            val start=System.nanoTime()
            runtime.run(input,output)
            val inferenceMs=(System.nanoTime()-start)/1_000_000
            output.rewind()
            val values=FloatArray(22*5376)
            output.asFloatBuffer().get(values)
            require(values.all { it.isFinite() }) { "Output model tidak valid" }
            DetectionResult(decoder.decode(values,5376,transform),inferenceMs)
            }
        }
    }
    override fun close() { synchronized(this) { closed=true; interpreter?.close(); interpreter=null } }
}
