package com.google.mediapipe.examples.handlandmarker.onboarding


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.handlandmarker.MainActivity
import com.google.mediapipe.examples.handlandmarker.R

class OnboardingFragment2 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_onboarding2, container, false)

        val startButton = view.findViewById<Button>(R.id.btn_start)
        startButton.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isFirstTime", false).apply()

            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }

        return view
    }
}