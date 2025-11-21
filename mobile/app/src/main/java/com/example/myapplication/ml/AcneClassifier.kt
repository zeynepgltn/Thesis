package com.example.myapplication.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TensorFlow Lite Model Classifier
 * Akne tespiti yapar
 */
class AcneClassifier(context: Context) {

    private var interpreter: Interpreter? = null

    companion object {
        private const val TAG = "AcneClassifier"

        private const val MODEL_FILE = "acne_model_final.tflite"
        private const val INPUT_SIZE = 224
        private const val THRESHOLD = 0.5f
    }

    init {
        try {
            // Model dosyasını yükle
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, " Model başarıyla yüklendi")

        } catch (e: Exception) {
            Log.e(TAG, " Model yükleme hatası: ${e.message}")
        }
    }

    /**
     * Fotoğrafı analiz eder
     */
    fun classify(bitmap: Bitmap): ClassificationResult {
        // Görüntüyü 224x224 boyutuna getir
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // ByteBuffer'a dönüştür
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // Output buffer
        val outputBuffer = ByteBuffer.allocateDirect(4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Model çalıştır
        val startTime = System.currentTimeMillis()
        interpreter?.run(inputBuffer, outputBuffer)
        val inferenceTime = System.currentTimeMillis() - startTime

        // Sonucu oku (sigmoid çıktısı 0.0-1.0 arası)
        outputBuffer.rewind()
        val rawConfidence = outputBuffer.float

        Log.d(TAG, " RAW SIGMOID DEĞER: $rawConfidence")

        // Sigmoid > 0.5 = Akne VAR (1)
        // Sigmoid < 0.5 = Akne YOK (0)
        val prediction = if (rawConfidence > THRESHOLD) 1 else 0

        Log.d(TAG, " THRESHOLD: $THRESHOLD")
        Log.d(TAG, " PREDICTION: $prediction")

        // Confidence = tahmin ettiğimiz sınıfın olasılığı
        val finalConfidence = if (prediction == 1) {
            rawConfidence  // Akne VAR için sigmoid
        } else {
            1.0f - rawConfidence  // Akne YOK için (1 - sigmoid)
        }

        Log.d(TAG, " FINAL CONFIDENCE: $finalConfidence")
        Log.d(TAG, " Sonuç: Prediction=$prediction, Confidence=$finalConfidence, Süre=${inferenceTime}ms")

        return ClassificationResult(
            prediction = prediction,
            confidence = finalConfidence,
            inferenceTimeMs = inferenceTime
        )
    }

    /**
     * Bitmap'i ByteBuffer'a çevirir (normalizasyon ile)
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // MobileNetV2 preprocess_input kullanıyor: [-1, +1] aralığı
        for (pixel in pixels) {
            // RGB kanallarını çıkar [0-255]
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            // [-1, +1] aralığına dönüştür (MobileNetV2 standart)
            byteBuffer.putFloat((r / 127.5f) - 1.0f)
            byteBuffer.putFloat((g / 127.5f) - 1.0f)
            byteBuffer.putFloat((b / 127.5f) - 1.0f)
        }

        Log.d(TAG, " Normalizasyon: MobileNetV2 [-1,+1] aralığı")

        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}

/**
 * Sınıflandırma sonucu
 */
data class ClassificationResult(
    val prediction: Int,        // 0: Akne Yok, 1: Akne Var
    val confidence: Float,      // 0.0 - 1.0
    val inferenceTimeMs: Long   // Süre (ms)
) {
    fun getPredictionText(): String = when (prediction) {
        0 -> "Akne Tespit Edilmedi "
        1 -> "Akne Tespit Edildi ⚠"
        else -> "Bilinmiyor"
    }

    fun getConfidencePercentage(): Int = (confidence * 100).toInt()
}