package com.epicqueststudios.beamtaxi.domain.activities

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.epicqueststudios.beamtaxi.R
import com.epicqueststudios.beamtaxi.presentation.factories.SharedViewModelFactory
import com.epicqueststudios.beamtaxi.databinding.ActivityMainBinding
import com.epicqueststudios.beamtaxi.data.models.ActionType
import com.epicqueststudios.beamtaxi.presentation.viewmodels.SharedViewModel
import com.epicqueststudios.beamtaxi.presentation.utils.toast
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.navigation.NavigationView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val geocoder: Geocoder by lazy { Geocoder(this, Locale.getDefault()) }
    private val prefs by lazy { this@MainActivity.getSharedPreferences(PREFS_FILENAME, 0)}
    private val locationCallback: LocationCallback? = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            onLocationChanged(locationResult.lastLocation)
        }
    }
    private var requestingLocationUpdates: Boolean = false
    private var locationRequest: LocationRequest? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var locationPermissionGranted: Boolean = false
    private var lastKnownLocation: Location? = null
    private var cameraPosition: CameraPosition? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: SharedViewModel by viewModels {
        SharedViewModelFactory(
            geocoder
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this,
            R.layout.activity_main
        )
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        setSupportActionBar(toolbar)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_map,
                R.id.nav_profile
            ), drawer_layout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)
        nav_view.setNavigationItemSelectedListener(this)

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
            requestingLocationUpdates = savedInstanceState.getBoolean(
                KEY_REQUESTING_LOCATION_UPDATES
            )
        }

        Places.initialize(applicationContext, getString(R.string.maps_api_key))
        placesClient = Places.createClient(this)
        fusedLocationProviderClient = getFusedLocationProviderClient(this)

        Dexter.withContext(this)
            .withPermissions(
                permission.ACCESS_FINE_LOCATION,
                permission.ACCESS_COARSE_LOCATION,
                permission.CAMERA,
                permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (!report.areAllPermissionsGranted()){
                        toast(R.string.permissions_not_granted)
                        finish()
                    } else {
                        locationPermissionGranted = true
                        updateLocationUI()
                        startLocationUpdates()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()

        viewModel.action.observe(this, Observer {
            when(it.getContentIfNotHandled()){
                ActionType.MAP_READY -> updateLocationUI()
                ActionType.SEARCH_GALLERY -> searchGallery()
                ActionType.TAKE_PHOTO -> takePhoto()
            }
        })

        viewModel.updates.observe(this, Observer {
            if (it) {
                startLocationUpdates()
            } else {
                stopLocationUpdates()
            }
        })
        viewModel.profile.observe(this, Observer {
            prefs.edit().apply {
                putString(PREFS_KEY_PROFILE_NAME, it.name)
                apply()
            }
        })
        viewModel.downloadProfile()
    }

    private fun searchGallery() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        if (photoPickerIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(photoPickerIntent,
                REQUEST_GALLERY_IMAGE
            )
        }
    }

    private fun takePhoto() {
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePhotoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePhotoIntent,
                REQUEST_IMAGE_CAPTURE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        requestingLocationUpdates = viewModel.manuallySelectedMarker != null
    }

    override fun onDestroy() {
        viewModel.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    (data?.extras?.get("data") as? Bitmap?)?.let {
                        viewModel.onPhotoAcquired(it)
                    }
                }

                REQUEST_GALLERY_IMAGE -> {
                    try {
                        val imageUri = data?.data
                        val imageStream: InputStream? =
                            contentResolver.openInputStream(imageUri!!)
                        viewModel.onPhotoAcquired(BitmapFactory.decodeStream(imageStream))
                    } catch (e: FileNotFoundException) {
                        Log.e(TAG, e.message, e)
                    }
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
            outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
        }
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (locationPermissionGranted && viewModel.map != null) {
            viewModel.map?.isMyLocationEnabled = true
            viewModel.map?.uiSettings?.setAllGesturesEnabled(true)
            viewModel.map?.uiSettings?.isMyLocationButtonEnabled = true
            viewModel.map?.setOnMapLongClickListener {
                viewModel.setLocationManually(it)
                toast(getString(R.string.set_location_manually, it.toString()))
                stopLocationUpdates()
            }
            viewModel.showCars()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval =
                UPDATE_INTERVAL
            fastestInterval =
                FASTEST_INTERVAL
        }
        locationRequest?.let {
            val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(it)
            val locationSettingsRequest: LocationSettingsRequest = builder.build()
            val settingsClient: SettingsClient = LocationServices.getSettingsClient(this@MainActivity)
            settingsClient.checkLocationSettings(locationSettingsRequest)
            getFusedLocationProviderClient(this@MainActivity).requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.myLooper()
            )
        }
    }

    fun onLocationChanged(location: Location) {
        viewModel.updateLocation(LatLng(location.latitude, location.longitude))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_location -> {
                viewModel.showCurrentLocation()
                true
            }
            R.id.action_reset_location -> {
                viewModel.resetLocation()
                true
            }
            R.id.action_reset_cars -> {
                viewModel.resetCars()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showScreen(resId: Int) {
        nav_host_fragment.findNavController().apply {
            popBackStack()
            navigate(resId)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        showScreen(item.itemId)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        const val TAG = "MainActivity"
        const val KEY_CAMERA_POSITION = "KEY_CAMERA_POSITION"
        const val KEY_LOCATION = "KEY_LOCATION"
        const val KEY_REQUESTING_LOCATION_UPDATES= "KEY_REQUESTING_LOCATION_UPDATES"
        const val DEFAULT_ZOOM = 12
        const val PREFS_FILENAME = "com.epicqueststudios.beamtaxi.prefs"
        const val PREFS_KEY_PROFILE_NAME = "prefs_name"

        const val REQUEST_IMAGE_CAPTURE = 1001
        const val REQUEST_GALLERY_IMAGE = 1002

        private const val UPDATE_INTERVAL = 10 * 1000L /* 10 secs */
        private const val FASTEST_INTERVAL = 3000L /* 3 sec */
    }
}
