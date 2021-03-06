package com.android.weather.presentation.view.fragment.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.weather.R
import com.android.weather.data.model.City
import com.android.weather.data.model.Weather
import com.android.weather.data.utils.makeGone
import com.android.weather.data.utils.makeVisible
import com.android.weather.data.utils.showSnackBar
import com.android.weather.databinding.FragmentHomeBinding
import com.android.weather.presentation.state.AppState
import com.android.weather.presentation.view.fragment.info.viewpager.InfoFragment
import com.android.weather.presentation.view.fragment.weather.WeatherFragment
import com.android.weather.presentation.viewmodel.home.HomeViewModel
import java.io.IOException

private const val IS_RUSSIAN_KEY = "LIST_OF_RUSSIAN_KEY"
private const val REFRESH_PERIOD = 60000L
private const val MINIMAL_DISTANCE = 100f

class HomeFragment : Fragment() {

    private var isDataSetRus: Boolean = true
    private var fabGroupVisibility: Boolean = true

    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this).get(HomeViewModel::class.java)
    }

    private val adapter = HomeFragmentAdapter()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.cityListRecyclerView.adapter = adapter

        adapter.setOnItemViewClickListener { weather ->
            openWeatherFragment(weather)
        }

        with(binding) {
            locationSearchingAndGetWeatherFab.setOnClickListener {
                checkPermission()
            }

            changeCityListFab.setOnClickListener {
                changeWeatherDataSet()
                saveListOfTowns()
            }

            toolbarOnHomeFragment.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.info -> {
                        openInfoFragment()
                        true
                    }
                    R.id.visibility -> {
                        fabGroupVisibility = !fabGroupVisibility
                        groupFabs.visibility =
                            if (fabGroupVisibility) View.VISIBLE else View.GONE
                        true
                    }
                    else -> false
                }
            }
        }

        homeViewModel.getData().observe(viewLifecycleOwner) { renderData(it) }
        loadListOfTowns()
        showWeatherDataSet()
    }

    private fun checkPermission() {
        activity?.let {
            when {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    getLocation()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    showRationaleDialog()
                }
                else -> {
                    requestPermission()
                }
            }
        }
    }

    private fun showRationaleDialog() {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_rationale_title))
                .setMessage(getString(R.string.dialog_rationale_message))
                .setPositiveButton(getString(R.string.dialog_rationale_give_access)) { _, _ ->
                    requestPermission()
                }
                .setNegativeButton(getString(R.string.dialog_rationale_decline)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocation()
            } else {
                showDialog(
                    getString(R.string.dialog_title_no_gps),
                    getString(R.string.dialog_message_no_gps)
                )
            }
        }

    private fun showDialog(title: String, message: String) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(getString(R.string.dialog_button_close)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    private fun getLocation() {
        activity?.let { context ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
                    provider?.let {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            REFRESH_PERIOD,
                            MINIMAL_DISTANCE,
                            onLocationListener
                        )
                    }
                } else {
                    val location =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location == null) {
                        showDialog(
                            getString(R.string.dialog_title_gps_turned_off),
                            getString(R.string.dialog_message_last_location_unknown)
                        )
                    } else {
                        getAddress(context, location)
                        showDialog(
                            getString(R.string.dialog_title_gps_turned_off),
                            getString(R.string.dialog_message_last_known_location)
                        )
                    }
                }
            } else {
                showRationaleDialog()
            }
        }
    }

    private val onLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            context?.let {
                getAddress(it, location)
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun getAddress(context: Context, location: Location) {
        val geoCoder = Geocoder(context)
        Thread {
            try {
                val addresses = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
                binding.changeCityListFab.post {
                    showAddressDialog(addresses.first().getAddressLine(0), location)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun showAddressDialog(address: String, location: Location) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_address_title))
                .setMessage(address)
                .setPositiveButton(getString(R.string.dialog_address_get_weather)) { _, _ ->
                    openWeatherFragment(
                        Weather(
                            City(
                                address,
                                location.latitude,
                                location.longitude
                            )
                        )
                    )
                }
                .setNegativeButton(getString(R.string.dialog_button_close)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    private fun openWeatherFragment(weather: Weather) {
        activity?.supportFragmentManager?.apply {
            beginTransaction()
                .replace(R.id.container, WeatherFragment.newInstance(Bundle().apply {
                    putParcelable(WeatherFragment.BUNDLE_EXTRA, weather)
                }))
                .addToBackStack("")
                .commitAllowingStateLoss()
        }
    }

    private fun loadListOfTowns() {
        requireActivity().apply {
            isDataSetRus = getPreferences(Context.MODE_PRIVATE).getBoolean(IS_RUSSIAN_KEY, true)
        }
    }

    private fun saveListOfTowns() {
        requireActivity().apply {
            getPreferences(Context.MODE_PRIVATE).edit {
                putBoolean(IS_RUSSIAN_KEY, isDataSetRus)
                apply()
            }
        }
    }

    private fun changeWeatherDataSet() {
        isDataSetRus = !isDataSetRus
        showWeatherDataSet()
    }

    private fun showWeatherDataSet() {
        if (isDataSetRus) {
            homeViewModel.getWeatherFromLocalSourceRus()
            binding.changeCityListFab.setImageResource(R.drawable.ic_change_city_list)
        } else {
            homeViewModel.getWeatherFromLocalSourceWorld()
            binding.changeCityListFab.setImageResource(R.drawable.ic_change_city_list)
        }
    }

    private fun openInfoFragment() {
        activity?.supportFragmentManager?.apply {
            beginTransaction()
                .replace(R.id.container, InfoFragment())
                .addToBackStack("")
                .commitAllowingStateLoss()
        }
    }

    private fun renderData(data: AppState) {
        when (data) {
            is AppState.Success -> {
                binding.includeProgressBarLayout.progressBarLayout.makeGone()
                adapter.setWeather(data.weatherData)
            }
            is AppState.Loading -> {
                binding.includeProgressBarLayout.progressBarLayout.makeVisible()
            }
            is AppState.Error -> {
                binding.includeProgressBarLayout.progressBarLayout.makeGone()
                binding.changeCityListFab.showSnackBar(
                    getString(R.string.text_error),
                    getString(R.string.text_reload)
                ) {
                    if (isDataSetRus) {
                        homeViewModel.getWeatherFromLocalSourceRus()
                    } else {
                        homeViewModel.getWeatherFromLocalSourceWorld()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
