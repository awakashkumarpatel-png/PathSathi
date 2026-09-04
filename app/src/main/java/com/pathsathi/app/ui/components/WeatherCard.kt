package com.pathsathi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathsathi.app.data.model.DailyForecast
import com.pathsathi.app.data.model.WeatherAlert
import com.pathsathi.app.data.model.WeatherInfo
import com.pathsathi.app.ui.theme.GreenAccent
import com.pathsathi.app.ui.theme.TealPrimary

@Composable
fun WeatherCard(
    weather: WeatherInfo?,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(TealPrimary, GreenAccent)))
                .padding(20.dp)
        ) {
            when {
                isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("Fetching live weather...", color = Color.White)
                }

                error != null -> Text(
                    "Weather unavailable — $error",
                    color = Color.White
                )

                weather != null -> Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${weather.temperatureC}°C",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(weather.condition, color = Color.White)
                            Text(
                                "Wind ${weather.windSpeedKmh} km/h",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                        Text(weather.emoji, fontSize = 48.sp)
                    }

                    if (weather.humidityPercent >= 0 || weather.visibilityMeters >= 0 || weather.sunrise.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (weather.humidityPercent >= 0) {
                                WeatherMiniStat(Icons.Default.WaterDrop, "${weather.humidityPercent}%", "Humidity")
                            }
                            if (weather.visibilityMeters >= 0) {
                                WeatherMiniStat(
                                    Icons.Default.Visibility,
                                    if (weather.visibilityMeters >= 1000) "${weather.visibilityMeters / 1000}km" else "${weather.visibilityMeters}m",
                                    "Visibility"
                                )
                            }
                            if (weather.precipitationProbability >= 0) {
                                WeatherMiniStat(Icons.Default.WaterDrop, "${weather.precipitationProbability}%", "Rain")
                            }
                            if (weather.sunrise.isNotBlank()) {
                                WeatherMiniStat(Icons.Default.WbSunny, weather.sunrise, "Sunrise")
                            }
                            if (weather.sunset.isNotBlank()) {
                                WeatherMiniStat(Icons.Default.NightsStay, weather.sunset, "Sunset")
                            }
                        }
                    }
                }

                else -> Text("Enable location for automatic weather", color = Color.White)
            }
        }

        if (weather != null && weather.alerts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            weather.alerts.forEach { alert -> WeatherAlertBanner(alert) }
        }

        if (weather != null && weather.dailyForecast.size > 1) {
            Spacer(Modifier.height(10.dp))
            Text("5-Day Forecast", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(weather.dailyForecast) { day -> DailyForecastChip(day) }
            }
        }
    }
}

@Composable
private fun WeatherMiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
    }
}

@Composable
private fun WeatherAlertBanner(alert: WeatherAlert) {
    val bg = when (alert.severity) {
        WeatherAlert.Severity.SEVERE -> Color(0xFFE0463D)
        WeatherAlert.Severity.WARNING -> Color(0xFFFF9F45)
        WeatherAlert.Severity.INFO -> Color(0xFF4E8FF7)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = bg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = bg)
            Text(alert.message, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DailyForecastChip(day: DailyForecast) {
    ElevatedCard(shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (day.dateEpochDay == 0L) "Today" else "Day ${day.dateEpochDay + 1}", fontSize = 11.sp)
            Text(day.emoji, fontSize = 22.sp)
            Text("${day.maxTempC.toInt()}°/${day.minTempC.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (day.precipitationProbability >= 0) {
                Text("${day.precipitationProbability}% rain", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
