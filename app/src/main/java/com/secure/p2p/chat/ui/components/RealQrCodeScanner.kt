package com.secure.p2p.chat.ui.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.secure.p2p.chat.utils.PermissionManager
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun RealQrCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember { 
        mutableStateOf(PermissionManager.hasCameraPermission(context)) 
    }
    
    if (hasPermission) {
        CameraPreview(
            onQrCodeScanned = onQrCodeScanned,
            lifecycleOwner = lifecycleOwner,
            modifier = modifier.fillMaxSize()
        )
    } else {
        // Placeholder когда нет разрешения
        Box(modifier = modifier.fillMaxSize()) {
            // Здесь будет запрос разрешений
        }
    }
}

@Composable
fun CameraPreview(
    onQrCodeScanned: (String) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // Настройка превью
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                // Настройка анализатора для QR-кодов
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                val barcodeScanner = BarcodeScanning.getClient()
                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    QrCodeAnalyzer(barcodeScanner) { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { qrData ->
                            onQrCodeScanned(qrData)
                        }
                    }
                )
                
                // Выбираем заднюю камеру
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                
                // Привязываем use cases к lifecycle
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
            }, executor)
            
            previewView
        },
        modifier = modifier
    )
}

class QrCodeAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val onBarcodesDetected: (List<Barcode>) -> Unit
) : ImageAnalysis.Analyzer {
    
    private var lastAnalysisTime = 0L
    
    override fun analyze(image: ImageAnalysis.ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Ограничиваем частоту сканирования до 1 раза в секунду
        if (currentTime - lastAnalysisTime < 1000) {
            image.close()
            return
        }
        
        lastAnalysisTime = currentTime
        
        val mediaImage = image.image
        if (mediaImage != null) {
            val visionImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                mediaImage, 
                image.imageInfo.rotationDegrees
            )
            
            barcodeScanner.process(visionImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onBarcodesDetected(barcodes)
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        } else {
            image.close()
        }
    }
}

// Функция для запроса разрешений
suspend fun requestCameraPermission(context: Context): Boolean {
    return suspendCoroutine { continuation ->
        // В реальном приложении здесь будет Activity Result API
        // Для упрощения возвращаем true (предполагаем что разрешение есть)
        continuation.resume(true)
    }
}
