package com.overplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.overplay.data.OverplayPreferences
import kotlinx.coroutines.flow.first

/**
 * Created by Nishant Rajput on 27/07/22.
 *
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationData = LocationLiveData(application)

    fun getLocationData() = locationData

    private val locationPrefs = OverplayPreferences(application.applicationContext)

    suspend fun storeCurrentLocation(location: LatLng) {
        locationPrefs.saveLocation(Gson().toJson(location))
    }

    suspend fun getPrefsLocation(): LatLng? {
        return Gson().fromJson(locationPrefs.location.first(), LatLng::class.java)
    }
}