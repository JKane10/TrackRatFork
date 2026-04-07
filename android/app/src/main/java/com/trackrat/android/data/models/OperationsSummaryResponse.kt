package com.trackrat.android.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

/**
 * Response from /api/v2/routes/summary endpoint.
 * Provides a human-readable summary of recent train operations
 * with optional raw metrics for UI display.
 */
@JsonClass(generateAdapter = true)
data class OperationsSummaryResponse(
    /** Short headline for collapsed view (max 50 chars) */
    @Json(name = "headline") val headline: String,

    /** Detailed summary (2-4 sentences) for expanded view */
    @Json(name = "body") val body: String,

    /** Summary scope: network, route, or train */
    @Json(name = "scope") val scope: String,

    /** Time window in minutes (90 for recent, 43200 for 30-day) */
    @Json(name = "time_window_minutes") val timeWindowMinutes: Int,

    /** Age of data in seconds */
    @Json(name = "data_freshness_seconds") val dataFreshnessSeconds: Int,

    /** When summary was generated */
    @Json(name = "generated_at") val generatedAt: ZonedDateTime,

    /** Raw metrics for optional UI display */
    @Json(name = "metrics") val metrics: SummaryMetrics?
)

@JsonClass(generateAdapter = true)
data class SummaryMetrics(
    @Json(name = "on_time_percentage") val onTimePercentage: Double?,
    @Json(name = "average_delay_minutes") val averageDelayMinutes: Double?,
    @Json(name = "arrival_on_time_percentage") val arrivalOnTimePercentage: Double?,
    @Json(name = "arrival_average_delay_minutes") val arrivalAverageDelayMinutes: Double?,
    @Json(name = "cancellation_count") val cancellationCount: Int?,
    @Json(name = "train_count") val trainCount: Int?
)
