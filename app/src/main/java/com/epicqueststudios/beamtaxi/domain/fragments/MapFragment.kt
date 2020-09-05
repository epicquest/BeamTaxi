package com.epicqueststudios.beamtaxi.domain.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.epicqueststudios.beamtaxi.domain.activities.MainActivity
import com.epicqueststudios.beamtaxi.R
import com.epicqueststudios.beamtaxi.presentation.viewmodels.SharedViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment

class MapFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_map, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync {googleMap ->
                if (googleMap != null) {
                    viewModel.map = googleMap
                    viewModel.map?.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(SharedViewModel.defaultLocation, MainActivity.DEFAULT_ZOOM.toFloat()))
                    viewModel.onMapReady()
                }
            }
    }
}