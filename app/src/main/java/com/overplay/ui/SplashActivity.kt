package com.overplay.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.overplay.R
import com.overplay.databinding.ActivitySplashBinding
import com.overplay.utils.VideoPreloadWorker

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        schedulePreloadWork(getString(R.string.video_url))

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, PlayerActivity::class.java)
            startActivity(intent)
            finish()
        }, 4000)
    }

    private fun schedulePreloadWork(videoUrl: String) {
        val workManager = WorkManager.getInstance(applicationContext)
        val videoPreloadWorker = VideoPreloadWorker.buildWorkRequest(videoUrl)
        workManager.enqueueUniqueWork(
            "VideoPreloadWorker",
            ExistingWorkPolicy.KEEP,
            videoPreloadWorker
        )
    }
}