package com.epicqueststudios.beamtaxi.domain.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.epicqueststudios.beamtaxi.R
import com.epicqueststudios.beamtaxi.databinding.FragmentProfileBinding
import com.epicqueststudios.beamtaxi.data.models.ProfileModel
import com.epicqueststudios.beamtaxi.presentation.viewmodels.SharedViewModel


class ProfileFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
         container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentProfileBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_profile, container, false
        )
        val view: View = binding.root
        binding.viewModel = sharedViewModel
        binding.lifecycleOwner = this
        binding.profile = sharedViewModel.profile.value?:ProfileModel("")

        return view
    }

     companion object {

         @BindingAdapter("profileImage")
         @JvmStatic
         fun loadImage(view: ImageView, bitmap: Bitmap?) {
             Glide.with(view.context)
                 .load(bitmap).apply(RequestOptions().circleCrop())
                 .into(view)
         }

         @BindingAdapter("textChangedListener")
         @JvmStatic
         fun bindTextWatcher(editText: EditText, textWatcher: TextWatcher?) {
             editText.addTextChangedListener(textWatcher)
         }
     }
}