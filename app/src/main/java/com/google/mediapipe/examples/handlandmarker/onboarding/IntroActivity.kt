package com.google.mediapipe.examples.handlandmarker.onboarding
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.mediapipe.examples.handlandmarker.R

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val fragments = listOf(OnboardingFragment1(), OnboardingFragment2())
        viewPager.adapter = OnboardingPagerAdapter(this, fragments)
    }
}