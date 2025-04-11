package com.google.mediapipe.examples.handlandmarker

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val textView = findViewById<TextView>(R.id.textView2)
        val fullText = "사용 할 모션을 선택해주세요."
        val spannable = SpannableString(fullText)

        // "사용 할 모션"만 색상 변경 (0번째부터 7번째까지)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#40A4FF")), // 원하는 색상
            0,
            7,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable

        // 상단 바에 뒤로가기 버튼 활성화
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "모션 추가" // 여기서 타이틀 설정

    }

    // ← 버튼 눌렀을 때 동작 정의
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 현재 액티비티 종료 (뒤로가기)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}