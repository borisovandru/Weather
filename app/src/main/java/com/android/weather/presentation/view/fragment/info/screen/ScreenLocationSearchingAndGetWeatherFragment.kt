package com.android.weather.presentation.view.fragment.info.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.weather.databinding.FragmentScreenLocationSearchingAndGetWeatherBinding

class ScreenLocationSearchingAndGetWeatherFragment : Fragment() {

    private var _binding: FragmentScreenLocationSearchingAndGetWeatherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentScreenLocationSearchingAndGetWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
