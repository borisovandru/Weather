package com.android.weather.data.utils

import com.android.weather.data.model.response.WeatherDTO
import com.android.weather.data.model.City
import com.android.weather.data.model.Weather
import com.android.weather.data.model.getDefaultCity
import com.android.weather.data.room.HistoryEntity

fun convertDtoToModel(weatherDTO: WeatherDTO): MutableList<Weather> {
    val fact = weatherDTO.fact!!
    return mutableListOf(
        Weather(
            getDefaultCity(),
            fact.temp!!,
            fact.feels_like!!,
            fact.condition!!
        )
    )
}

fun convertHistoryEntityToWeather(entityList: List<HistoryEntity>): MutableList<Weather> {
    return entityList.map {
        Weather(City(it.city, 0.0, 0.0), it.temperature, 0, it.condition)
    } as MutableList<Weather>
}

fun convertWeatherToEntity(weather: Weather): HistoryEntity {
    return HistoryEntity(0, weather.city.city, weather.temperature, weather.condition)
}
