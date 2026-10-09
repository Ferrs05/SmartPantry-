package id.smartpantry.app.presentation

import android.graphics.Bitmap
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable fun CameraScreen(onPhoto: (Bitmap)->Unit,onError: (String)->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val previewView=remember { PreviewView(context) }
    val capture=remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val executor=remember { Executors.newSingleThreadExecutor() }
    val active=remember { AtomicBoolean(true) }
    val latestPhoto by rememberUpdatedState(onPhoto)
    val latestError by rememberUpdatedState(onError)
    var ready by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        active.set(true)
        var disposed=false
        var cameraProvider: ProcessCameraProvider?=null
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(!disposed) try {
                val provider=future.get()
                cameraProvider=provider
                val preview=Preview.Builder().build().also { it.surfaceProvider=previewView.surfaceProvider }
                provider.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture)
                ready=true
            } catch(e: Exception) { latestError("Kamera tidak bisa dibuka. Pilih foto dari galeri. ${e.message ?: ""}") }
        },ContextCompat.getMainExecutor(context))
        onDispose { active.set(false); disposed=true; cameraProvider?.unbind(capture); cameraProvider?.unbindAll(); executor.shutdown() }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory={previewView},modifier=Modifier.fillMaxSize())
        Button(onClick={
            capturing=true
            capture.targetRotation=previewView.display?.rotation ?: Surface.ROTATION_0
            capture.takePicture(executor,object: ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val photo=PhotoLoader.orientCamera(image.toBitmap(),image.imageInfo.rotationDegrees)
                        ContextCompat.getMainExecutor(context).execute {
                            if(active.get()) latestPhoto(photo) else photo.recycle()
                        }
                    } catch(e: Exception) {
                        ContextCompat.getMainExecutor(context).execute { if(active.get()) { capturing=false;latestError("Foto tidak dapat diproses: ${e.message}") } }
                    } finally { image.close() }
                }
                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute { if(active.get()) { capturing=false;latestError("Pengambilan foto gagal: ${exception.message}") } }
                }
            })
        },enabled=ready && !capturing,modifier=Modifier.align(Alignment.BottomCenter).padding(24.dp).heightIn(min=56.dp)) {
            Text(if(capturing) "Mengambil foto…" else "Ambil foto")
        }
    }
}
