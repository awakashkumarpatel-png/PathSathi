package com.pathsathi.app.data.network

import com.pathsathi.app.data.model.DailyForecast
import com.pathsathi.app.data.model.WeatherAlert
import com.pathsathi.app.data.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Fetches live weather automatically for any lat/lng using Open-Meteo.
 * No API key required, so weather works out of the box - fully automatic.
 * Now also pulls a 5-day forecast plus humidity/visibility/sunrise-sunset
 * for the "Advanced Weather" feature, and derives simple safety alerts
 * (heavy rain, storms, high wind, poor visibility) from the current data.
 */
class WeatherRepository {

    private val client = OkHttpClient()

    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherInfo> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,wind_speed_10m,weather_code,is_day,relative_humidity_2m,visibility" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                        "&forecast_days=5&timezone=auto"

                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        IOException("Empty response")
                    )
                    val json = JSONObject(body)
                    val current = json.getJSONObject("current")
                    val daily = json.optJSONObject("daily")

                    val humidity = current.optInt("relative_humidity_2m", -1)
                    val visibility = current.optInt("visibility", -1)
                    val weatherCode = current.getInt("weather_code")
                    val windSpeed = current.getDouble("wind_speed_10m")

                    var todaySunrise = ""
                    var todaySunset = ""
                    var todayPrecipProb = -1
                    val forecast = mutableListOf<DailyForecast>()

                    if (daily != null) {
                        val times = daily.optJSONArray("time")
                        val codes = daily.optJSONArray("weather_code")
                        val maxTemps = daily.optJSONArray("temperature_2m_max")
                        val minTemps = daily.optJSONArray("temperature_2m_min")
                        val precipProbs = daily.optJSONArray("precipitation_probability_max")
                        val sunrises = daily.optJSONArray("sunrise")
                        val sunsets = daily.optJSONArray("sunset")

                        val count = times?.length() ?: 0
                        for (i in 0 until count) {
                            val sunrise = sunrises?.optString(i)?.substringAfter("T") ?: ""
                            val sunset = sunsets?.optString(i)?.substringAfter("T") ?: ""
                            val precipProb = precipProbs?.optInt(i) ?: -1
                            forecast.add(
                                DailyForecast(
                                    dateEpochDay = i.toLong(),
                                    maxTempC = maxTemps?.optDouble(i) ?: 0.0,
                                    minTempC = minTemps?.optDouble(i) ?: 0.0,
                                    weatherCode = codes?.optInt(i) ?: 0,
                                    precipitationProbability = precipProb,
                                    sunrise = sunrise,
                                    sunset = sunset
                                )
                            )
                            if (i == 0) {
                                todaySunrise = sunrise
                                todaySunset = sunset
                                todayPrecipProb = precipProb
                            }
                        }
                    }

                    val alerts = buildAlerts(weatherCode, windSpeed, visibility, todayPrecipProb)

                    val info = WeatherInfo(
                        temperatureC = current.getDouble("temperature_2m"),
                        windSpeedKmh = windSpeed,
                        weatherCode = weatherCode,
                        isDay = current.getInt("is_day") == 1,
                        humidityPercent = humidity,
                        visibilityMeters = visibility,
                        precipitationProbability = todayPrecipProb,
                        sunrise = todaySunrise,
                        sunset = todaySunset,
                        dailyForecast = forecast,
                        alerts = alerts
                    )
                    Result.success(info)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildAlerts(
        weatherCode: Int,
        windSpeedKmh: Double,
        visibilityMeters: Int,
        precipProbability: Int
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()

        if (weatherCode in listOf(95, 96, 99)) {
            alerts.add(
                WeatherAlert(
                    "Thunderstorm warning",
                    "Thunderstorms are expected. Avoid exposed ridgelines and peaks.",
                    WeatherAlert.Severity.SEVERE
                )
            )
        }
        if (weatherCode in listOf(65, 82) || precipProbability >= 70) {
            alerts.add(
                WeatherAlert(
                    "Heavy rain expected",
                    "High chance of heavy rainfall - trails may be slippery or flooded.",
                    WeatherAlert.Severity.WARNING
                )
            )
        }
        if (windSpeedKmh >= 40) {
            alerts.add(
                WeatherAlert(
                    "High wind warning",
                    "Wind speeds are ${windSpeedKmh.toInt()} km/h - use caution at exposed sections.",
                    WeatherAlert.Severity.WARNING
                )
            )
        }
        if (visibilityMeters in 1..2000) {
            alerts.add(
                WeatherAlert(
                    "Low visibility",
                    "Visibility is reduced to about ${visibilityMeters}m - consider delaying travel.",
                    WeatherAlert.Severity.WARNING
                )
            )
        }
        if (weatherCode in listOf(71, 73, 75)) {
            alerts.add(
                WeatherAlert(
                    "Snow conditions",
                    "Snowfall expected - carry appropriate gear and watch footing.",
                    WeatherAlert.Severity.INFO
                )
            )
        }
        return alerts
    }
}
