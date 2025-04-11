package com.google.mediapipe.examples.handlandmarker.onboarding


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.handlandmarker.R


class OnboardingFragment1 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_onboarding1, container, false)
    }
}