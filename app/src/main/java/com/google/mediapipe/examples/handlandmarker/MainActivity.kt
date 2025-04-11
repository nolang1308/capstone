package com.google.mediapipe.examples.handlandmarker



import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {



    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton = findViewById<Button>(R.id.add_motion_page_btn)
//        val toThirdPageButton = findViewById<Button>(R.id.ThirdPageBtn)
        val toggleButton = findViewById<ToggleButton>(R.id.toggleButton1)


        startButton.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
//
//        toThirdPageButton.setOnClickListener {
//            val intent = Intent(this, LearningPage::class.java)
//            startActivity(intent)
//        }

        val gridView = findViewById<GridView>(R.id.gridView)

        val itemList = listOf(
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),
            GridItem(R.drawable.add_btn, "#"+"캡쳐", "2020.12.12"),

        )

        val adapter = GridAdapter(this, itemList)
        gridView.adapter = adapter

        // 권한 확인 및 요청
        checkPermissions()

//        startOverlayButton.setOnClickListener {
//            if (!Settings.canDrawOverlays(this)) {
//                requestOverlayPermission()
//            } else if (!isCameraPermissionGranted()) {
//                requestCameraPermission()
//            } else {
//                startOverlayService()
//            }
//        }
//
//        stopOverlayButton.setOnClickListener {
//            stopOverlayService()
//        }
        toggleButton.setOnClickListener {
            if (toggleButton.isChecked) {
                toggleButton.isChecked = true
                Toast.makeText(this, "on", Toast.LENGTH_SHORT).show()
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission()
                } else if (!isCameraPermissionGranted()) {
                    requestCameraPermission()
                } else {
                    startOverlayService()
                }
            } else {
                Toast.makeText(this, "off", Toast.LENGTH_SHORT).show()
                stopOverlayService()

            }
        }
    }



    // 권한 확인 함수
    private fun checkPermissions() {
        if (!isCameraPermissionGranted()) {
            requestCameraPermission()
        }

        if (!isExternalStoragePermissionGranted() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestExternalStoragePermission()
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun isExternalStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun requestExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
    }

    // 오버레이 권한 요청
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            } else {
                Toast.makeText(this, "오버레이 권한이 필요합니다!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startOverlayService() {
        val serviceIntent = Intent(this, CameraOverlayService::class.java)
        Log.d("MainActivity", "🚀 startOverlayService() 호출됨")
        startService(serviceIntent)
    }

    private fun stopOverlayService() {
        val serviceIntent = Intent(this, CameraOverlayService::class.java)
        stopService(serviceIntent)
    }



    override fun onPause() {
        super.onPause()
        // 앱이 백그라운드로 가기 전에 필요한 작업을 수행할 수 있습니다
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1002
    }
}