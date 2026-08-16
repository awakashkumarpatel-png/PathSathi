package com.pathsathi.app.engine

object AutoTravelEngine {
    data class ProgressInput(
        val dayNumber: Int,
        val totalDays: Int,
        val plannedMinutesElapsedByNow: Int,
        val actualMinutesElapsed: Int,
        val budgetForDayInr: Int,
        val spentSoFarInr: Int,
        val nextDestinationName: String,
        val nextDestinationDistanceKm: Double?
    )

    fun evaluate(input: ProgressInput): String {
        val delayMinutes = input.actualMinutesElapsed - input.plannedMinutesElapsedByNow
        val budgetLeft = input.budgetForDayInr - input.spentSoFarInr
        val sb = StringBuilder("Day ${input.dayNumber} of ${input.totalDays}. ")
        when {
            delayMinutes > 20 -> sb.append("The itinerary is about $delayMinutes minutes behind schedule. ")
            delayMinutes < -20 -> sb.append("You're about ${-delayMinutes} minutes ahead of schedule. ")
            else -> sb.append("You're roughly on schedule. ")
        }
        sb.append("Next stop: ${input.nextDestinationName}. ")
        input.nextDestinationDistanceKm?.let { distance ->
            val eta = estimateTravelMinutes(distance)
            sb.append("About ${"%.1f".format(distance)} km away (~$eta min). ")
        } ?: sb.append("Distance is unavailable until a real map point is available. ")
        if (budgetLeft < 0) sb.append("You're over today's budget by ₹${-budgetLeft}. ")
        else sb.append("₹$budgetLeft left in today's budget. ")
        if (delayMinutes > 20) sb.append("Suggestion: prioritize the main destination and skip a lower-priority stop if needed.")
        return sb.toString().trim()
    }

    private fun estimateTravelMinutes(distanceKm: Double): Int =
        ((distanceKm / 12.0) * 60).toInt().coerceAtLeast(1)
}
