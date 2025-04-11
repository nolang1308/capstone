package com.google.mediapipe.examples.handlandmarker


// MainActivity.kt
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

class LearningPage : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var resultTextView: TextView

    private lateinit var tflite: Interpreter
    private val IMAGE_SIZE = 224 // 모델에 맞게 조정 필요
    private val TAG = "TFLite_CameraX"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)

        previewView = findViewById(R.id.previewView)
        resultTextView = findViewById(R.id.resultTextView)

        // TensorFlow Lite 모델 로드
        try {
            val tfliteModel = loadModelFile("model.tflite")
            tflite = Interpreter(tfliteModel)
        } catch (e: Exception) {
            Log.e(TAG, "모델 로드 실패: ${e.message}")
            resultTextView.text = "모델 로드 실패: ${e.message}"
        }

        // 카메라 실행 전 권한 확인
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 카메라 제공자 가져오기
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 프리뷰 설정
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // 이미지 분석 설정
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                }

            // 카메라 선택 (후면 카메라)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 기존 바인딩 해제
                cameraProvider.unbindAll()

                // 새로운 바인딩 생성
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "카메라 바인딩 실패", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {

        private var lastAnalyzedTimestamp = 0L
        private val INTERVAL_MS = 300 // 300ms마다 분석 (초당 약 3회)

        override fun analyze(image: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()

            // 분석 간격 제한
            if (currentTimestamp - lastAnalyzedTimestamp >= INTERVAL_MS) {

                val rotationDegrees = image.imageInfo.rotationDegrees

                // 이미지 변환
                val bitmap = imageProxyToBitmap(image)
                val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees.toFloat())

                // 이미지 분류
                val result = classifyImage(rotatedBitmap)

                // UI 업데이트
                runOnUiThread {
                    resultTextView.text = result
                }

                lastAnalyzedTimestamp = currentTimestamp
            }

            image.close()
        }

        private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // YUV -> NV21 변환
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                100,
                out
            )
            val jpegByteArray = out.toByteArray()
            return BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
        }

        private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees)
            return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        }
    }

    // 이미지 분류
    private fun classifyImage(bitmap: Bitmap): String {
        // 중앙 부분 정사각형으로 자르기
        val dimension = min(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - dimension) / 2
        val yOffset = (bitmap.height - dimension) / 2
        val croppedBitmap = Bitmap.createBitmap(
            bitmap, xOffset, yOffset, dimension, dimension
        )

        // 모델 입력 크기에 맞게 비트맵 리사이즈
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, IMAGE_SIZE, IMAGE_SIZE, true)

        // 입력 ByteBuffer 생성
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // 출력 배열 설정 (클래스 수에 따라 조정 필요)
        val outputSize = 2 // 예시로 1000개 클래스가 있다고 가정
        val outputs = Array(1) { FloatArray(outputSize) }

        try {
            // 추론 실행
            tflite.run(byteBuffer, outputs)

            // 결과 해석
            val results = outputs[0]

            // 상위 3개 결과 찾기
            val topResults = results.withIndex()
                .sortedByDescending { it.value }
                .take(3)

            val resultText = StringBuilder("분석 결과:\n")
            for ((index, confidence) in topResults) {
                // 여기서는 실제 라벨을 매핑해야 합니다
                resultText.append("클래스 $index: ${String.format("%.1f", confidence * 100)}%\n")
            }

            return resultText.toString()
        } catch (e: Exception) {
            Log.e(TAG, "분류 중 오류 발생: ${e.message}")
            return "분류 오류: ${e.message}"
        }
    }

    // 비트맵을 ByteBuffer로 변환
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // 모델 입력에 맞게 설정 (채널 수, 정규화 등)
        val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3) // 4바이트 * 너비 * 높이 * 채널(RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            // 색상 추출 및 정규화 (0-255 -> 0-1)
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            // ByteBuffer에 추가 (모델에 따라 순서와 정규화 방식 조정 필요)
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    // TensorFlow Lite 모델 파일 로드
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                resultTextView.text = "카메라 권한이 필요합니다"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}