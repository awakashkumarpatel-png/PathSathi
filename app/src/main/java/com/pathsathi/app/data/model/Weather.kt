package com.pathsathi.app.data.model

data class DailyForecast(
    val dateEpochDay: Long,
    val maxTempC: Double,
    val minTempC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int,
    val sunrise: String,
    val sunset: String
) {
    val condition: String get() = conditionFor(weatherCode)
    val emoji: String get() = emojiFor(weatherCode, isDay = true)
}

data class WeatherAlert(
    val title: String,
    val message: String,
    val severity: Severity
) {
    enum class Severity { INFO, WARNING, SEVERE }
}

data class WeatherInfo(
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidityPercent: Int = -1,
    val visibilityMeters: Int = -1,
    val precipitationProbability: Int = -1,
    val sunrise: String = "",
    val sunset: String = "",
    val dailyForecast: List<DailyForecast> = emptyList(),
    val alerts: List<WeatherAlert> = emptyList()
) {
    val condition: String get() = conditionFor(weatherCode)
    val emoji: String get() = emojiFor(weatherCode, isDay)
}

private fun conditionFor(weatherCode: Int): String = when (weatherCode) {
    0 -> "Clear sky"
    1, 2, 3 -> "Partly cloudy"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Rain showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Unknown"
}

private fun emojiFor(weatherCode: Int, isDay: Boolean): String = when (weatherCode) {
    0 -> if (isDay) "☀️" else "🌙"
    1, 2, 3 -> "⛅"
    45, 48 -> "🌫️"
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
    71, 73, 75 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}
