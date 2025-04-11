package com.google.mediapipe.examples.handlandmarker


import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.Toast

class HandLandmarkOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 10f
    }

    private val linePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    // 드로잉 자취를 저장할 리스트
    private val drawingPoints = mutableListOf<Pair<Float, Float>>()

    private var handLandmarks: List<List<LandmarkPoint>> = emptyList()

    data class LandmarkPoint(val x: Float, val y: Float, val z: Float)

    fun updateLandmarks(landmarks: List<List<LandmarkPoint>>) {
        this.handLandmarks = landmarks
        invalidate()
    }

    fun clearLandmarks() {
        handLandmarks = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (handLandmarks.isEmpty()) return

        for (hand in handLandmarks) {
            drawHandConnections(canvas, hand)

            if (hand.size >= 9) {
                val thumbTip = hand[4]
                val indexTip = hand[8]

                val thumbX = (1f - thumbTip.y) * width
                val thumbY = (1f - thumbTip.x) * height
                val indexX = (1f - indexTip.y) * width
                val indexY = (1f - indexTip.x) * height

                val distance = distanceBetweenPoints(thumbX, thumbY, indexX, indexY)

                if (distance <= 20f) {
                    // 가운데 지점 계산
                    val midX = (thumbX + indexX) / 2
                    val midY = (thumbY + indexY) / 2

                    // 점 찍기 + 자취 저장
                    canvas.drawCircle(midX, midY, 10f, paint)
                    drawingPoints.add(Pair(midX, midY))
                    val screenCenterX = resources.displayMetrics.widthPixels / 2f
                    val screenCenterY = resources.displayMetrics.heightPixels / 2f

                    // AccessibilityService에 터치 요청 보내기
                    AccessibilityTouchSender.requestClick(context, screenCenterX, screenCenterY)



                } else {
                    // 거리가 벌어지면 자취 초기화
                    drawingPoints.clear()
                }

                // 자취 그리기
                drawTrail(canvas)
            }
        }
    }

    private fun drawHandConnections(canvas: Canvas, landmarks: List<LandmarkPoint>) {
        val connections = listOf(
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
            Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
            Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
            Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
            Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
            Pair(5, 9), Pair(9, 13), Pair(13, 17)
        )

        for ((startIdx, endIdx) in connections) {
            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val start = landmarks[startIdx]
                val end = landmarks[endIdx]

                val startX = (1f - start.y) * width
                val startY = (1f - start.x) * height
                val endX = (1f - end.y) * width
                val endY = (1f - end.x) * height

                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }
        }
    }

    private fun drawTrail(canvas: Canvas) {
        for ((x, y) in drawingPoints) {
            canvas.drawCircle(x, y, 6f, paint)
        }
    }

    private fun distanceBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
    }
}