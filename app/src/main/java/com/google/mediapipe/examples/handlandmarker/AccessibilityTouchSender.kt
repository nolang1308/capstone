package com.google.mediapipe.examples.handlandmarker

import android.content.Context
import android.content.Intent

object AccessibilityTouchSender {
    fun requestClick(context: Context, x: Float, y: Float) {
        val intent = Intent("ACTION_TOUCH_FROM_VIEW").apply {
            putExtra("x", x)
            putExtra("y", y)
        }
        context.sendBroadcast(intent)
    }

}