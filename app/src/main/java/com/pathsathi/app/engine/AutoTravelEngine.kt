package com.pathsathi.app.engine

/**
 * Modular "travel engine": uses current progress vs planned schedule and budget
 * to produce guidance text. Fully offline / rule-based — no network required.
 */
object AutoTravelEngine {

    data class ProgressInput(
        val dayNumber: Int,
        val totalDays: Int,
        val plannedMinutesElapsedByNow: Int,
        val actualMinutesElapsed: Int,
        val budgetForDayInr: Int,
        val spentSoFarInr: Int,
        val nextDestinationName: String,
        val nextDestinationDistanceKm: Double
    )

    fun evaluate(input: ProgressInput): String {
        val delayMinutes = input.actualMinutesElapsed - input.plannedMinutesElapsedByNow
        val budgetLeft = input.budgetForDayInr - input.spentSoFarInr
        val etaMinutes = estimateTravelMinutes(input.nextDestinationDistanceKm)

        val sb = StringBuilder()
        sb.append("Day ${input.dayNumber} of ${input.totalDays}. ")

        when {
            delayMinutes > 20 -> sb.append("The itinerary is approximately $delayMinutes minutes behind schedule. ")
            delayMinutes < -20 -> sb.append("You're ahead of schedule by about ${-delayMinutes} minutes. ")
            else -> sb.append("You're roughly on schedule. ")
        }

        sb.append("Next stop: ${input.nextDestinationName}, about ${"%.1f".format(input.nextDestinationDistanceKm)} km away ")
        sb.append("(~$etaMinutes min). ")

        if (budgetLeft < 0) {
            sb.append("You're over today's budget by ₹${-budgetLeft}. ")
        } else {
            sb.append("₹$budgetLeft left in today's budget. ")
        }

        if (delayMinutes > 20) {
            sb.append("Suggestion: prioritize the main destination and consider skipping a lower-priority stop today.")
        }

        return sb.toString().trim()
    }

    /** Simple offline ETA estimate assuming a mixed walking/local-transport pace. Not a live traffic estimate. */
    private fun estimateTravelMinutes(distanceKm: Double): Int {
        val avgSpeedKmh = 12.0 // conservative mixed-mode average for offline estimate
        return ((distanceKm / avgSpeedKmh) * 60).toInt().coerceAtLeast(1)
    }
}
