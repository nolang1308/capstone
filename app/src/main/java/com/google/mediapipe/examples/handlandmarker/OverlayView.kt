/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.handlandmarker
import kotlin.math.sqrt
import kotlin.math.pow  // 이거 추가

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max
import kotlin.math.min
class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: HandLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var pathPaint = Paint()
    private val drawingPath = mutableListOf<Pair<Float, Float>>()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        pathPaint.color = Color.RED
        pathPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pathPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawPath(canvas) // 저장된 경로를 그림

        results?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                var thumbX = 0f
                var thumbY = 0f
                var indexX = 0f
                var indexY = 0f

                for ((pointIndex, normalizedLandmark) in landmark.withIndex()) {
                    val x = normalizedLandmark.x() * imageWidth * scaleFactor
                    val y = normalizedLandmark.y() * imageHeight * scaleFactor

                    canvas.drawPoint(x, y, pointPaint)
                    when (pointIndex) {
                        4 -> { thumbX = x; thumbY = y }
                        8 -> { indexX = x; indexY = y }
                    }
                }

                val distance = sqrt((indexX - thumbX).pow(2) + (indexY - thumbY).pow(2))

                if (distance <= 150) {
                    val midX = (thumbX + indexX) / 2
                    val midY = (thumbY + indexY) / 2
                    drawingPath.add(Pair(midX, midY)) // 경로 추가
                    invalidate()
                } else {
                    drawingPath.clear() // 손을 떼면 그림 삭제
                    invalidate()
                }
            }
        }
    }

    private fun drawPath(canvas: Canvas) {
        for (i in 1 until drawingPath.size) {
            val (prevX, prevY) = drawingPath[i - 1]
            val (currX, currY) = drawingPath[i]
            canvas.drawLine(prevX, prevY, currX, currY, pathPaint)
        }
    }

    fun clearDrawing() {
        drawingPath.clear()
        invalidate()
    }

    fun setResults(
        handLandmarkerResults: HandLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = handLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO -> min(width * 1f / imageWidth, height * 1f / imageHeight)
            RunningMode.LIVE_STREAM -> max(width * 1f / imageWidth, height * 1f / imageHeight)
        }
        invalidate()
    }



    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}

