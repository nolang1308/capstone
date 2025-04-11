package com.google.mediapipe.examples.handlandmarker

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


class MyAccessibilityService : AccessibilityService() {
    val activeType = ActiveType()


    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter("ACTION_TOUCH_FROM_VIEW")
        registerReceiver(touchReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private val touchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val x = intent?.getFloatExtra("x", 0f) ?: return
            val y = intent.getFloatExtra("y", 0f)

            performTouch(x, y)
            Log.d("MyAccessibilityService", "Touch 요청 수신: x=$x, y=$y")

        }
    }

    private fun performTouch(x: Float, y: Float) {
//        val path = Path().apply { // 터치
//            moveTo(x, y)
//        }
//        val gesture = GestureDescription.Builder()
//            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
//            .build()
//        dispatchGesture(gesture, null, null)

//        val startX = x            // 위로 드래그
//        val startY = y
//        val endX = x
//        val endY = y - 300f // 위로 300픽셀 드래그
//
//        val path = Path().apply {
//            moveTo(startX, startY)
//            lineTo(endX, endY)
//        }
//
//        val gesture = GestureDescription.Builder()
//            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
//            .build()
//
//        dispatchGesture(gesture, null, null)

        performGlobalAction(activeType.actions[3])




    }

    override fun onDestroy() {
        unregisterReceiver(touchReceiver)
        super.onDestroy()
    }
}