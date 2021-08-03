package com.android.weather.model.repository

import com.android.weather.model.data.Weather
import com.android.weather.model.data.getRussianCities
import com.android.weather.model.data.getWorldCities

class RepositoryImpl : Repository {
    override fun getWeatherFromServer() = Weather()
    override fun getWeatherFromLocalStorageRus() = getRussianCities()
    override fun getWeatherFromLocalStorageWorld() = getWorldCities()
}