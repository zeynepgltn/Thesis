package com.example.myapplication.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Kamera önizlemesi için Composable
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Bitmap) -> Unit,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Fotoğraf çekme fonksiyonu
 */
suspend fun capturePhoto(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    imageCapture: ImageCapture
): Bitmap = suspendCoroutine { continuation ->
    val executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                continuation.resume(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                // Hata durumunda boş bitmap döndür
                continuation.resume(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
            }
        }
    )
}

/**
 * ImageProxy'yi Bitmap'e çevirir
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    // JPEG'den Bitmap oluştur
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Rotasyonu düzelt
    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

/**
 * CameraProvider başlatma
 */
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(this))
    }
}