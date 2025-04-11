package com.google.mediapipe.examples.handlandmarker

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.*
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.IOException
import android.view.View

class CameraOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var camera: Camera? = null
    private var surfaceView: SurfaceView? = null
    private var landmarkOverlayView: HandLandmarkOverlayView? = null




    // MediaPipe 손 랜드마크 감지기
    private var handLandmarker: HandLandmarker? = null

    companion object {
        private const val TAG = "CameraOverlayService"
        private const val MODEL_NAME = "hand_landmarker.task"
    }


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "✅ 서비스 실행됨")

        // MediaPipe 핸드 랜드마커 초기화
        setupHandLandmarker()

        // WindowManager 서비스 가져오기
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 오버레이 레이아웃 생성 (서피스뷰 + 버튼)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_camera, null)

        surfaceView = overlayView?.findViewById(R.id.camera_preview)
        landmarkOverlayView = overlayView?.findViewById(R.id.landmark_overlay)


        val holder = surfaceView!!.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
                    if (camera != null) {
                        camera!!.setDisplayOrientation(90)
                        camera!!.setPreviewDisplay(holder)

                        // 프리뷰 콜백 설정 - 각 프레임마다 손 랜드마크 감지
                        setupPreviewCallback()

                        camera!!.startPreview()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "카메라 오픈 에러: ${e.message}")
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                // 프레임 크기 변경 시 필요한 처리
                if (camera != null) {
                    try {
                        camera!!.stopPreview()

                        // 프리뷰 크기 조정 등 필요한 작업 수행
                        val parameters = camera!!.parameters
                        val sizes = parameters.supportedPreviewSizes
                        val optimalSize = getOptimalPreviewSize(sizes, width, height)
                        parameters.setPreviewSize(optimalSize!!.width, optimalSize.height)
                        camera!!.parameters = parameters

                        setupPreviewCallback()
                        camera!!.startPreview()
                    } catch (e: Exception) {
                        Log.e(TAG, "프리뷰 변경 에러: ${e.message}")
                    }
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }
        })

        // 오버레이 창 속성 설정
        val params = WindowManager.LayoutParams(
            dpToPx(70),
            dpToPx(100),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
//
//        // 캡처 버튼 클릭 리스너 설정
//        captureButton?.setOnClickListener {
//            takePicture()
//        }

        // WindowManager를 통해 오버레이 창 추가
        windowManager!!.addView(overlayView, params)

        // 터치 이벤트를 통해 오버레이 이동 가능하게 설정
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager!!.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // MediaPipe 핸드 랜드마커 설정
    private fun setupHandLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setDelegate(Delegate.GPU)  // GPU 사용 (또는 CPU로 변경 가능)
                .setModelAssetPath(MODEL_NAME)

            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.IMAGE)  // 이미지 모드로 설정
                .setNumHands(2)  // 최대 2개의 손 감지
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)

            handLandmarker = HandLandmarker.createFromOptions(this, optionsBuilder.build())
            Log.d(TAG, "✅ HandLandmarker 초기화 성공")
        } catch (e: Exception) {
            Log.e(TAG, "❌ HandLandmarker 초기화 실패: ${e.message}")
        }
    }

    // 카메라 프리뷰 콜백 설정
    private fun setupPreviewCallback() {
        camera?.let { cam ->
            val parameters = cam.parameters
            val previewSize = parameters.previewSize
            val previewFormat = parameters.previewFormat
            val previewBuffer = ByteArray(
                previewSize.width * previewSize.height *
                        android.graphics.ImageFormat.getBitsPerPixel(previewFormat) / 8
            )

            cam.addCallbackBuffer(previewBuffer)
            cam.setPreviewCallbackWithBuffer { data, camera ->
                // 프리뷰 데이터를 비트맵으로 변환
                try {
                    val bitmap = convertYuvToRgb(data, previewSize.width, previewSize.height)
                    processHandLandmarks(bitmap)

                    // 버퍼 재사용
                    camera.addCallbackBuffer(data)
                } catch (e: Exception) {
                    Log.e(TAG, "프레임 처리 에러: ${e.message}")
                }
            }
        }
    }

    // YUV 데이터를 RGB 비트맵으로 변환 (간단한 구현)
    private fun convertYuvToRgb(data: ByteArray, width: Int, height: Int): Bitmap {
        // 실제 애플리케이션에서는 YUV->RGB 변환을 위한 더 효율적인 네이티브 메서드를 사용해야 합니다.
        // 여기서는 간단한 구현만 제공합니다.
        val yuvImage = android.graphics.YuvImage(
            data,
            android.graphics.ImageFormat.NV21,
            width,
            height,
            null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 90, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // 손 랜드마크 처리
    private fun processHandLandmarks(bitmap: Bitmap) {
        handLandmarker?.let { landmarker ->
            try {
                // 비트맵을 MPImage로 변환
                val mpImage = BitmapImageBuilder(bitmap).build()

                // 손 랜드마크 감지 수행
                val result = landmarker.detect(mpImage)

                // 결과 로깅 및 처리
//                logHandLandmarkResult(result)

                // 랜드마크 오버레이 뷰 업데이트
                updateLandmarkOverlay(result)
            } catch (e: Exception) {
                Log.e(TAG, "손 랜드마크 처리 에러: ${e.message}")
            }
        }
    }

    // 오버레이를 업데이트하는 새 메서드 추가:
    private fun updateLandmarkOverlay(result: HandLandmarkerResult) {
        val numHands = result.handedness().size
        if (numHands > 0) {
            val allHandLandmarks = mutableListOf<List<HandLandmarkOverlayView.LandmarkPoint>>()

            for (i in 0 until numHands) {
                val landmarks = result.landmarks()[i]
                val handLandmarks = landmarks.map { landmark ->
                    HandLandmarkOverlayView.LandmarkPoint(landmark.x(), landmark.y(), landmark.z())
                }
                allHandLandmarks.add(handLandmarks)
            }

            // UI 업데이트는 메인 스레드에서 실행
            landmarkOverlayView?.post {
                landmarkOverlayView?.updateLandmarks(allHandLandmarks)
            }
        } else {
            // 손이 감지되지 않음, 오버레이 지우기
            landmarkOverlayView?.post {
                landmarkOverlayView?.clearLandmarks()
            }
        }
    }


    private fun getOptimalPreviewSize(sizes: List<Camera.Size>, width: Int, height: Int): Camera.Size? {
        val targetRatio = width.toDouble() / height
        if (sizes.isEmpty()) return null

        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > 0.1) continue

            val diff = Math.abs(size.height - height)
            if (diff < minDiff) {
                optimalSize = size
                minDiff = diff.toDouble()
            }
        }

        // 적절한 비율을 찾지 못한 경우, 가장 가까운 크기 반환
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                val diff = Math.abs(size.height - height) + Math.abs(size.width - width)
                if (diff < minDiff) {
                    optimalSize = size
                    minDiff = diff.toDouble()
                }
            }
        }

        return optimalSize
    }

    // 카메라 리소스를 해제하는 메서드
    private fun releaseCamera() {
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }
    }

    // 리소스 해제
    private fun releaseResources() {
        handLandmarker?.close()
        handLandmarker = null
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager!!.removeView(overlayView)
        }
        releaseCamera()
        releaseResources()
    }
}