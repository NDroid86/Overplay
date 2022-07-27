package com.overplay.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource.*
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.eazypermissions.common.model.PermissionResult
import com.eazypermissions.coroutinespermission.PermissionManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.SphericalUtil
import com.overplay.BuildConfig
import com.overplay.OverPlayApp
import com.overplay.R
import com.overplay.databinding.ActivityPlayerBinding
import com.overplay.hasPermission
import com.overplay.viewmodel.LocationViewModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.sqrt


class PlayerActivity : AppCompatActivity() {
    private val TAG = PlayerActivity::class.java.toString()
    private lateinit var binding: ActivityPlayerBinding
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(parentJob + Dispatchers.Default)
    private val locationViewModel: LocationViewModel by viewModels()
    private lateinit var mHttpDataSourceFactory: Factory
    private lateinit var mDefaultDataSourceFactory: DefaultDataSourceFactory
    private lateinit var mCacheDataSourceFactory: DataSource.Factory
    private val cache: SimpleCache = OverPlayApp.cache
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    private var audioManager: AudioManager? = null

    // Declaring sensorManager
    // and acceleration constants
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        requestLocationPermission()
        initSensor()
    }

    private fun initSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        Objects.requireNonNull(sensorManager)!!.registerListener(
            sensorListener, sensorManager!!
                .getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL
        )
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer

                mHttpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)

                this.mDefaultDataSourceFactory = DefaultDataSourceFactory(
                    applicationContext, mHttpDataSourceFactory
                )

                mCacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(mHttpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                val videoUri = Uri.parse(getString(R.string.video_url))
                val mediaItem = MediaItem.fromUri(videoUri)
                val mediaSource =
                    ProgressiveMediaSource.Factory(mCacheDataSourceFactory)
                        .createMediaSource(mediaItem)

                exoPlayer.setMediaSource(mediaSource, true)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.prepare()
            }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    private fun pausePlayer() {
        player!!.playWhenReady = false
        player!!.playbackState
    }

    private fun startPlayer() {
        player!!.playWhenReady = true
        player!!.playbackState
    }

    private fun isPlaying(): Boolean {
        return player != null && player!!.playbackState != Player.STATE_ENDED && player!!.playbackState != Player.STATE_IDLE && player!!.playWhenReady
    }

    private fun handleResult(permissionResult: PermissionResult) {
        when (permissionResult) {
            is PermissionResult.PermissionGranted -> {
                startLocationUpdate()
            }
            is PermissionResult.PermissionDenied -> locationPermissionRationalSnackbar
            is PermissionResult.ShowRational -> locationPermissionRationalSnackbar
            is PermissionResult.PermissionDeniedPermanently -> locationPermissionRationalSnackbar

        }
    }

    private fun startLocationUpdate() {
        locationViewModel.getLocationData().observe(this, Observer {
            lifecycleScope.launch {

                val currentLocation = LatLng(it.longitude, it.latitude)
                val storedLocation: LatLng? = locationViewModel.getPrefsLocation()
                if (storedLocation == null) {
                    locationViewModel.storeCurrentLocation(currentLocation)
                } else {
                    val distance =
                        SphericalUtil.computeDistanceBetween(currentLocation, storedLocation)
                    // If distance greater than or equal to 10 meters ExoPlayer will reset and restart the video
                    if (distance >= 10.0) {
                        locationViewModel.storeCurrentLocation(currentLocation)
                        player?.seekTo(0);
                        player?.setPlayWhenReady(true)
                    }
                }
            }
        })
    }

    private fun requestLocationPermission() {
        val permissionApproved =
            this@PlayerActivity.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ?: return

        if (permissionApproved) {
            startLocationUpdate()
        } else {
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    handleResult(
                        PermissionManager.requestPermissions(
                            this@PlayerActivity, 1,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }

    private val locationPermissionRationalSnackbar by lazy {
        Snackbar.make(
            binding.videoView,
            R.string.fine_permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                // Build intent that displays the App settings screen.
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    BuildConfig.APPLICATION_ID,
                    null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            .show()
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            // acceleration value is over 12 device shake event detected
            if (acceleration > 12 && isPlaying()) {
                pausePlayer()
            }

            //Increase/Decrease video position on rotating though Z-Axis
            if (z >= 5.0 && z <= 8.0) {
                var position = player?.currentPosition
                if (position != null) {
                    position = position.plus(50)
                    player?.seekTo(position)
                }
            } else if (z <= -5.0) {
                var position = player?.currentPosition
                if (position != null) {
                    position = position.minus(50)
                    player?.seekTo(position)
                }
            }

            //Increase/Decrease volume on rotating though X-Axis
            if (x >= 3) {
                audioManager?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            } else if (x <= -3) {
                audioManager?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }


    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        sensorManager?.registerListener(
            sensorListener, sensorManager!!.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            ), SensorManager.SENSOR_DELAY_NORMAL
        )
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(sensorListener)
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }


    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

}