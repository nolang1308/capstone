package com.google.mediapipe.examples.handlandmarker

import android.accessibilityservice.AccessibilityService

class ActiveType {
    // 전역 또는 파일 상단에 위치
    val actions = arrayOf(
        AccessibilityService.GLOBAL_ACTION_BACK,
        AccessibilityService.GLOBAL_ACTION_HOME,
        AccessibilityService.GLOBAL_ACTION_RECENTS,
        AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,

    )}