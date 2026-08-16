package com.pathsathi.app.engine

import com.pathsathi.app.data.model.ItineraryDay

/**
 * Minimal dependency-free encode/decode for List<ItineraryDay> so we can store
 * itineraries as a single TEXT column in Room without pulling in a JSON library.
 * Field separator: "|", list separator (places/food): ",", day separator: "\n".
 * Values are escaped so our separators can't appear raw inside content.
 */
object ItinerarySerializer {
    private const val FIELD_SEP = "|"
    private const val LIST_SEP = ","
    private const val DAY_SEP = "\n"

    private fun esc(s: String) = s.replace("|", "¦").replace(",", "，").replace("\n", " ")

    fun encode(days: List<ItineraryDay>): String =
        days.joinToString(DAY_SEP) { d ->
            listOf(
                d.dayNumber.toString(),
                d.places.joinToString(LIST_SEP) { esc(it) },
                esc(d.travelSequence),
                esc(d.transportation),
                d.foodStops.joinToString(LIST_SEP) { esc(it) },
                d.restTimeMinutes.toString(),
                esc(d.stayInfo),
                d.estimatedCostInr.toString(),
                esc(d.scheduleNote)
            ).joinToString(FIELD_SEP)
        }

    fun decode(raw: String): List<ItineraryDay> {
        if (raw.isBlank()) return emptyList()
        return raw.split(DAY_SEP).mapNotNull { line ->
            val parts = line.split(FIELD_SEP)
            if (parts.size < 9) return@mapNotNull null
            ItineraryDay(
                dayNumber = parts[0].toIntOrNull() ?: 0,
                places = if (parts[1].isBlank()) emptyList() else parts[1].split(LIST_SEP),
                travelSequence = parts[2],
                transportation = parts[3],
                foodStops = if (parts[4].isBlank()) emptyList() else parts[4].split(LIST_SEP),
                restTimeMinutes = parts[5].toIntOrNull() ?: 0,
                stayInfo = parts[6],
                estimatedCostInr = parts[7].toIntOrNull() ?: 0,
                scheduleNote = parts[8]
            )
        }
    }
}
