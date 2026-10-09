package id.smartpantry.domain

import kotlin.math.roundToInt

data class Letterbox(val originalWidth: Int, val originalHeight: Int, val size: Int = 512) {
    val scale = minOf(size.toFloat()/originalWidth, size.toFloat()/originalHeight)
    val resizedWidth = (originalWidth*scale).roundToInt()
    val resizedHeight = (originalHeight*scale).roundToInt()
    val padLeft = (size-resizedWidth)/2
    val padTop = (size-resizedHeight)/2
    fun restore(cx: Float, cy: Float, w: Float, h: Float): Box = Box(
        ((cx-w/2-padLeft)/scale).coerceIn(0f, originalWidth.toFloat()),
        ((cy-h/2-padTop)/scale).coerceIn(0f, originalHeight.toFloat()),
        ((cx+w/2-padLeft)/scale).coerceIn(0f, originalWidth.toFloat()),
        ((cy+h/2-padTop)/scale).coerceIn(0f, originalHeight.toFloat()))
}
class YoloDecoder(private val confidence: Float = .25f, private val nmsIou: Float = .7f,
    private val maxDetections: Int = 300) {
    // v2 LiteRT export uses xywh in input pixels; no extra objectness channel.
    fun decode(output: FloatArray, anchors: Int, transform: Letterbox): List<Detection> {
        require(output.size == (4+Ingredients.names.size)*anchors)
        val candidates = ArrayList<Detection>()
        for (i in 0 until anchors) {
            var classId = 0
            var score = output[4*anchors+i]
            for (c in 1 until Ingredients.names.size) {
                val next = output[(4+c)*anchors+i]
                if (next > score) { score = next; classId = c }
            }
            if (!score.isFinite() || score < confidence) continue
            val xywh = FloatArray(4) { output[it*anchors+i] }
            if (xywh.any { !it.isFinite() } || xywh[2] <= 0 || xywh[3] <= 0) continue
            val box = transform.restore(xywh[0], xywh[1], xywh[2], xywh[3])
            if (box.right > box.left && box.bottom > box.top) candidates += Detection(classId,score,box)
        }
        val kept = ArrayList<Detection>()
        for (d in candidates.sortedByDescending { it.confidence }) {
            if (kept.none { it.classId == d.classId && it.box.iou(d.box) > nmsIou }) kept += d
            if (kept.size >= maxDetections) break
        }
        return kept
    }
}
