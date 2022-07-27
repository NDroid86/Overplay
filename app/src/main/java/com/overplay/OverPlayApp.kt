package com.overplay

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache

/**
 * Created by Nishant Rajput on 27/07/22.
 *
 */
class OverPlayApp : Application() {

    companion object {
        lateinit var cache: SimpleCache
    }

    private val cacheSize: Long = 90 * 1024 * 1024
    private lateinit var cacheEvictor: LeastRecentlyUsedCacheEvictor
    private lateinit var exoplayerDatabaseProvider: ExoDatabaseProvider

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        exoplayerDatabaseProvider = ExoDatabaseProvider(this@OverPlayApp)
        cache = SimpleCache(cacheDir, cacheEvictor, exoplayerDatabaseProvider)
    }
}