package com.epicqueststudios.beamtaxi.presentation.factories

import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epicqueststudios.beamtaxi.presentation.viewmodels.SharedViewModel

class SharedViewModelFactory(private val geocoder: Geocoder) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SharedViewModel(
            geocoder
        ) as T
    }

}