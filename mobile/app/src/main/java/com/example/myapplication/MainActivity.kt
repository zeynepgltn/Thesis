package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.data.local.AcneDatabase
import com.example.myapplication.data.model.AcneAnalysis
import com.example.myapplication.ml.AcneClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.camera.capturePhoto
import com.example.myapplication.camera.getCameraProvider
import com.example.myapplication.ui.ProgressScreen
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.myapplication.data.model.SkinCareAdvice
import com.example.myapplication.ui.theme.AcneDetectionTheme

class MainActivity : ComponentActivity() {

    private lateinit var classifier: AcneClassifier
    private lateinit var database: AcneDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Classifier'ı başlat
        classifier = AcneClassifier(this)

        // Database'i başlat
        database = AcneDatabase.getDatabase(this)

        setContent {
            AcneDetectionTheme {  // ← MaterialTheme yerine AcneDetectionTheme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AcneDetectionApp(classifier, database)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcneDetectionApp(classifier: AcneClassifier, database: AcneDatabase) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(" Akne Tespit Sistemi") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    label = { Text("Analiz") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { }
                )
                NavigationBarItem(
                    label = { Text("İlerleme") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { }
                )
                NavigationBarItem(
                    label = { Text("Geçmiş") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> AnalysisScreen(classifier, database)
                1 -> ProgressScreen(database)
                2 -> HistoryScreen(database)
            }
        }
    }
}

@Composable
fun AnalysisScreen(classifier: AcneClassifier, database: AcneDatabase) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var analysisResult by remember { mutableStateOf<AcneAnalysis?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Kamera izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, it)
                    ) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                selectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                analysisResult = null
                showCamera = false
            } catch (e: Exception) {
                Toast.makeText(context, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Kamera ekranı
    if (showCamera && hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Kamera önizlemesi
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    scope.launch {
                        val provider = ctx.getCameraProvider()
                        cameraProvider = provider

                        val previewUseCase = Preview.Builder().build()
                        preview = previewUseCase
                        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)

                        val imageCaptureUseCase = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        imageCapture = imageCaptureUseCase

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                previewUseCase,
                                imageCaptureUseCase
                            )
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
                        }
                    }

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Üst kısım - Kapat butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        showCamera = false
                        cameraProvider?.unbindAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("Kapat", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Alt kısım - Fotoğraf çek butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val bitmap = capturePhoto(
                                    context,
                                    cameraProvider!!,
                                    imageCapture!!
                                )
                                selectedImage = bitmap
                                showCamera = false
                                cameraProvider?.unbindAll()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Fotoğraf çekilemedi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Text("ÇEK", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    } else {
        // Normal analiz ekranı
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),  // ← BU SATIRI EKLE
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Başlık
            Text(
                text = "Akne Analizi",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Fotoğraf gösterme alanı
            selectedImage?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(bottom = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Seçilen fotoğraf",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kamera butonu
                ElevatedButton(
                    onClick = {
                        if (hasCameraPermission) {
                            showCamera = true
                            selectedImage = null
                            analysisResult = null
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Kamera", style = MaterialTheme.typography.titleMedium)
                }

                // Galeri butonu
                ElevatedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Galeri", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Analiz butonu
            Button(
                onClick = {
                    selectedImage?.let { bitmap ->
                        isAnalyzing = true
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.Default) {
                                    classifier.classify(bitmap)
                                }

                                val imagePath = withContext(Dispatchers.IO) {
                                    saveImageToInternalStorage(context, bitmap)
                                }

                                val analysis = AcneAnalysis(
                                    imagePath = imagePath,
                                    prediction = result.prediction,
                                    confidence = result.confidence
                                )

                                val id = withContext(Dispatchers.IO) {
                                    database.acneAnalysisDao().insertAnalysis(analysis)
                                }

                                analysisResult = analysis.copy(id = id)

                                Toast.makeText(context, "Analiz kaydedildi!", Toast.LENGTH_SHORT).show()

                            } catch (e: Exception) {
                                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            isAnalyzing = false
                        }
                    }
                },
                enabled = selectedImage != null && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Analiz Ediliyor...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Analiz Et", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Sonuç kartı
            analysisResult?.let { result ->
                Spacer(modifier = Modifier.height(24.dp))

                // Sonuç Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.prediction == 1)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = result.getPredictionText(),
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Güven: %${result.getConfidencePercentage()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bakım Önerileri Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Bakım Önerileri",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(12.dp))

                        // Önerileri listele
                        val advice = if (result.prediction == 1) {
                            SkinCareAdvice.acneDetectedAdvice
                        } else {
                            SkinCareAdvice.noAcneAdvice
                        }

                        advice.take(5).forEachIndexed { index, tip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genel İpuçları Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Genel Cilt Bakım İpuçları",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(12.dp))

                        // İpuçlarını çiftler halinde göster
                        SkinCareAdvice.generalTips.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pair[0],
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = pair.getOrNull(1) ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            if (pair != SkinCareAdvice.generalTips.chunked(2).last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HistoryScreen(database: AcneDatabase) {
    val analyses by database.acneAnalysisDao().getAllAnalyses()
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = " Analiz Geçmişi (${analyses.size})",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (analyses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Henüz analiz yapılmadı")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(analyses) { analysis ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = analysis.getPredictionText(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Güven: %${analysis.getConfidencePercentage()}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = analysis.getFormattedDate(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            database.acneAnalysisDao().deleteAnalysis(analysis)
                                            // Fotoğrafı da sil
                                            File(analysis.imagePath).delete()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Yardımcı fonksiyon
private fun saveImageToInternalStorage(context: android.content.Context, bitmap: Bitmap): String {
    val directory = File(context.filesDir, "acne_images")
    if (!directory.exists()) {
        directory.mkdirs()
    }

    val fileName = "acne_${System.currentTimeMillis()}.jpg"
    val file = File(directory, fileName)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    return file.absolutePath
}

