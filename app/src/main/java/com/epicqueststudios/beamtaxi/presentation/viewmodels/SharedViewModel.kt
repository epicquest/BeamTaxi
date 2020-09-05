package com.epicqueststudios.beamtaxi.presentation.viewmodels

import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.epicqueststudios.beamtaxi.data.common.BaseEvent
import com.epicqueststudios.beamtaxi.data.models.ActionType
import com.epicqueststudios.beamtaxi.data.models.ProfileModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import java.io.IOException
import kotlin.random.Random

class SharedViewModel(private val geocoder: Geocoder) : ViewModel() {

    private var tickerChannel: ReceiveChannel<Unit>? = null
    private var lastCheckedLocation: LatLng? = null
    val currentStreetName = MutableLiveData<String>()

    val action = MutableLiveData<BaseEvent<ActionType>>()
    val updates = MutableLiveData<Boolean>()
    val profile = MutableLiveData<ProfileModel>()
    val photoBitmap = MutableLiveData<Bitmap>()
    private var lastMarker: Marker? = null
    private var cars: MutableList<Marker> = mutableListOf()
    var manuallySelectedMarker: Marker? = null

    var map: GoogleMap? = null

    fun updateLocation(loc: LatLng) {
        val markerOptions = MarkerOptions().position(loc).title("test: $loc")
        lastMarker?.remove() ?: run {
            map?.moveCamera(CameraUpdateFactory.newLatLng(loc))
        }
        lastMarker = map?.addMarker(markerOptions)
        updateStreetName(loc)
    }

    fun showCurrentLocation() {
        lastMarker?.let {
            map?.moveCamera(CameraUpdateFactory.newLatLng(it.position))
        }
    }

    fun setLocationManually(pos: LatLng) {
        manuallySelectedMarker?.remove()
        val markerOption = MarkerOptions()
        markerOption.position(pos)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

        manuallySelectedMarker = map?.addMarker(markerOption)
        lastMarker?.remove()
        map?.moveCamera(CameraUpdateFactory.newLatLng(pos))
        updateStreetName(pos)
    }

    fun onMapReady() {
        action.value =
            BaseEvent(ActionType.MAP_READY)
    }

    fun resetLocation() {
        manuallySelectedMarker?.remove()
        manuallySelectedMarker = null
        lastMarker = null
        updates.postValue(true)
    }

    private fun updateStreetName(pos: LatLng) {
        GlobalScope.launch {
            updateStreetNameOnBackground(pos)
        }
    }

    private suspend fun updateStreetNameOnBackground(pos: LatLng) {
        val location = pos // do no inline this variable, it causes bug on emulator, M.S
        withContext(Dispatchers.IO) {
             if (lastCheckedLocation != location) {
                 var streetName: String?
                 try {
                     val addresses: List<Address>? =
                         geocoder.getFromLocation(location.latitude, location.longitude, 1)
                     if (addresses != null) {
                         val returnedAddress: Address = addresses[0]
                         val strReturnedAddress = StringBuilder()
                         for (i in 0..returnedAddress.maxAddressLineIndex) {
                             strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("")
                         }
                         streetName = "$strReturnedAddress"
                         lastCheckedLocation = location
                     } else {
                         streetName = ""
                     }
                 } catch (e: IOException) {
                     Log.e(TAG, e.message ,e)
                     streetName = ""
                 }
                 currentStreetName.postValue(streetName)
             }
        }
    }

    fun downloadProfile() {
        GlobalScope.launch {
            downloadProfileOnBackground()
        }
    }

    private suspend fun downloadProfileOnBackground() {
        withContext(Dispatchers.IO) {
            delay(5000L)
            profile.postValue(ProfileModel("Tomas Marny"))
        }
    }

    fun onPhotoAcquired(imageBitmap: Bitmap) {
        photoBitmap.postValue(imageBitmap)
    }

    fun searchGallery( ) {
        action.value =
            BaseEvent(ActionType.SEARCH_GALLERY)
    }

    fun takePhoto( ) {
        action.value =
            BaseEvent(ActionType.TAKE_PHOTO)
    }

    fun showCars() {
        generateCars(CAR_COUNT)
        GlobalScope.launch {
            withContext(Dispatchers.Main){
                tickerChannel?.cancel()
                 tickerChannel = ticker(delayMillis = 3_000, initialDelayMillis = 0)
                 for (event in tickerChannel!!) {
                     updateCarsPosition()
                 }

            }
        }
    }

    private fun generateCars(carCount: Int) {
        cars.forEach { it.remove() }
        cars.clear()
        List(carCount) {
            updateCarPosition(lastMarker?.position?: defaultLocation)?.let {
                cars.add(it)
            }
        }
    }

    private fun updateCarPosition(position: LatLng): Marker? {
        val markerOption = MarkerOptions()
        val rand1 = ( Random.nextInt(1000) - 500)/ 100000f
        val rand2 = ( Random.nextInt(1000) - 500)/ 100000f
        val randomPositionAround = LatLng(position.latitude + rand1, position.longitude + rand2)

        markerOption.position(randomPositionAround)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        return map?.addMarker(markerOption)
    }

    private fun updateCarsPosition() {
        val newCars = mutableListOf<Marker>()
        cars.forEach {
            updateCarPosition(it.position)?.let { newMarker ->
                newCars.add(newMarker)
            }
            it.remove()
        }
        cars = newCars
    }

    fun resetCars() {
        tickerChannel?.cancel()
        showCars()
    }

    fun onDestroy() {
        tickerChannel?.cancel()
    }

    val nameTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence,
            start: Int,
            count: Int,
            after: Int
        ) {
            // Do nothing.
        }

        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
            profile.postValue(ProfileModel("$s"))
        }

        override fun afterTextChanged(s: Editable) {
            // Do nothing.
        }
    }

    companion object {
        val defaultLocation = LatLng(50.068675, 14.435715)
        const val TAG = "SharedViewModel"
        const val CAR_COUNT = 5
    }
}