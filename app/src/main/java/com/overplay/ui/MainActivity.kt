package com.overplay.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.overplay.R
import com.overplay.databinding.ActivityMainBinding
import com.overplay.utils.VideoPreloadWorker

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.toString()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
    }
}
