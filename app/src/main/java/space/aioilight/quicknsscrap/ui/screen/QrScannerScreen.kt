package space.aioilight.quicknsscrap.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import space.aioilight.quicknsscrap.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import space.aioilight.quicknsscrap.qr.WifiCredentials
import space.aioilight.quicknsscrap.qr.WifiQrParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(onCredentialsFound: (WifiCredentials) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // 横画面: 左にカメラ、右に接続説明
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                QrCameraBox(
                    hasCameraPermission = hasCameraPermission,
                    onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
                    onQrDetected = { raw -> WifiQrParser.parse(raw)?.let(onCredentialsFound) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                QrInstructionsPanel(modifier = Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            // 縦画面: 上にカメラ、下に接続説明
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                QrCameraBox(
                    hasCameraPermission = hasCameraPermission,
                    onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
                    onQrDetected = { raw -> WifiQrParser.parse(raw)?.let(onCredentialsFound) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                QrInstructionsPanel(modifier = Modifier.weight(1.5f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun QrCameraBox(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (hasCameraPermission) {
            CameraPreview(onQrDetected = onQrDetected)
            QrScannerOverlay()
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.camera_permission_required))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}

@Composable
private fun QrInstructionsPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.scan_qr_prompt),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.how_to_connect),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            StepItem(
                step = 1,
                title = stringResource(R.string.step1_title),
                description = stringResource(R.string.step1_description),
            )
            Spacer(Modifier.height(16.dp))
            StepItem(
                step = 2,
                title = stringResource(R.string.step2_title),
                description = stringResource(R.string.step2_description),
            )
            Spacer(Modifier.height(16.dp))
            StepItem(
                step = 3,
                title = stringResource(R.string.step3_title),
                description = stringResource(R.string.step3_description),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.vpn_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepItem(step: Int, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$step",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    // DisposableEffect でライフサイクルに連動したカメラのセットアップと解放を行う
    DisposableEffect(lifecycleOwner) {
        // 連続フレームで同じ QR コードを何度も検出・通知しないよう前回の値を保持する
        var lastDetected = ""
        // CameraProvider の取得は非同期: ListenableFuture でコールバックを受け取る
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val analysis = ImageAnalysis.Builder()
                // STRATEGY_KEEP_ONLY_LATEST: 処理中のフレームをスキップして常に最新フレームを解析する
                // これによりバーコード検出の遅延が積み重ならない
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                        val mediaImage = proxy.image
                        if (mediaImage != null) {
                            // 回転角を渡すことで縦横どの向きでも正しく認識させる
                            val image = InputImage.fromMediaImage(
                                mediaImage, proxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { value ->
                                        // 前回と同じ値なら通知しない（重複呼び出し防止）
                                        if (value != lastDetected) {
                                            lastDetected = value
                                            onQrDetected(value)
                                        }
                                    }
                                }
                                // 成功・失敗に関わらず必ず proxy を閉じる（閉じないと次フレームが来ない）
                                .addOnCompleteListener { proxy.close() }
                        } else {
                            proxy.close()
                        }
                    }
                }

            // 既存のバインドをすべて解除してからライフサイクルに紐付け直す
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }, ContextCompat.getMainExecutor(context))

        // Composable が破棄されたときにスキャナーリソースを解放する
        onDispose { barcodeScanner.close() }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun QrScannerOverlay(modifier: Modifier = Modifier) {
    val frameColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val squareSize = minOf(w, h) * 0.65f
        val left = (w - squareSize) / 2f
        val top = (h - squareSize) / 2f
        val right = left + squareSize
        val bottom = top + squareSize

        // 四隅以外を暗くする（4枚の矩形で構成）
        val dim = Color(0x88000000)
        drawRect(dim, topLeft = Offset(0f, 0f), size = Size(w, top))
        drawRect(dim, topLeft = Offset(0f, bottom), size = Size(w, h - bottom))
        drawRect(dim, topLeft = Offset(0f, top), size = Size(left, squareSize))
        drawRect(dim, topLeft = Offset(right, top), size = Size(w - right, squareSize))

        // コーナーブラケットを描画
        val cornerLen = squareSize * 0.12f
        val stroke = 4.dp.toPx()

        // 左上
        drawLine(frameColor, Offset(left, top + cornerLen), Offset(left, top), strokeWidth = stroke)
        drawLine(frameColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = stroke)
        // 右上
        drawLine(frameColor, Offset(right - cornerLen, top), Offset(right, top), strokeWidth = stroke)
        drawLine(frameColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = stroke)
        // 左下
        drawLine(frameColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeWidth = stroke)
        drawLine(frameColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = stroke)
        // 右下
        drawLine(frameColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeWidth = stroke)
        drawLine(frameColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = stroke)
    }
}
