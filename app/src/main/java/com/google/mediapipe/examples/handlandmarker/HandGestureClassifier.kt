package com.google.mediapipe.examples.handlandmarker


import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class HandGestureClassifier(context: Context) {
    private val classifier: ImageClassifier

    init {
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(1)
            .setScoreThreshold(0.5f)
            .build()

        classifier = ImageClassifier.createFromFileAndOptions(
            context,
            "model.tflite",
            options
        )
    }

    fun classify(bitmap: Bitmap): String {
        val image = TensorImage.fromBitmap(bitmap)
        val results = classifier.classify(image)
        return results.firstOrNull()?.categories?.firstOrNull()?.label ?: "알 수 없음"
    }
}