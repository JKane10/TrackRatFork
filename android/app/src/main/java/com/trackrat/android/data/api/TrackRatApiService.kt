package com.trackrat.android.data.api

import com.trackrat.android.data.models.DeparturesResponse
import com.trackrat.android.data.models.OperationsSummaryResponse
import com.trackrat.android.data.models.TrainDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TrackRatApiService {

    /**
     * Get train departures between stations
     * @param from Departure station code (e.g., "NY", "NP")
     * @param to Optional arrival station code
     * @param limit Maximum number of results (default 50, max 100)
     */
    @GET("trains/departures")
    suspend fun getDepartures(
        @Query("from") from: String,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int = 50
    ): DeparturesResponse

    /**
     * Get detailed information about a specific train
     * @param trainId Train ID (can be numeric or alphanumeric like "A174")
     * @param date Journey date in YYYY-MM-DD format
     * @param refresh Force refresh from API if true
     */
    @GET("trains/{trainId}")
    suspend fun getTrainDetails(
        @Path("trainId") trainId: String,
        @Query("date") date: String,
        @Query("refresh") refresh: Boolean = false
    ): TrainDetailsResponse

    /**
     * Get ML-based platform predictions for a train at a specific station
     * @param stationCode Station code (e.g., "NY")
     * @param trainId Train identifier
     * @param journeyDate Journey date in YYYY-MM-DD format
     * @return Platform prediction data with probabilities
     */
    @GET("predictions/track")
    suspend fun getPlatformPrediction(
        @Query("station_code") stationCode: String,
        @Query("train_id") trainId: String,
        @Query("journey_date") journeyDate: String
    ): com.trackrat.android.data.models.PlatformPrediction

    /**
     * Get route congestion data showing train density across network segments
     * @param timeWindowHours Time window in hours for congestion data (default 3)
     * @param maxPerSegment Maximum trains per segment to return (default 200)
     * @return Congestion response with individual segment data
     */
    @GET("routes/congestion")
    suspend fun getCongestionData(
        @Query("time_window_hours") timeWindowHours: Int = 3,
        @Query("max_per_segment") maxPerSegment: Int = 200
    ): com.trackrat.android.data.models.CongestionResponse

    /**
     * Get operations summary for a route, network, or train
     * @param scope Summary scope: "network", "route", or "train"
     * @param fromStation Origin station code (required for route scope)
     * @param toStation Destination station code (required for route scope)
     * @param trainId Train ID (required for train scope)
     * @param dataSource Optional data source filter
     */
    @GET("routes/summary")
    suspend fun getOperationsSummary(
        @Query("scope") scope: String,
        @Query("from_station") fromStation: String? = null,
        @Query("to_station") toStation: String? = null,
        @Query("train_id") trainId: String? = null,
        @Query("data_source") dataSource: String? = null
    ): OperationsSummaryResponse

    /**
     * Health check endpoint
     * Returns backend server status
     */
    @GET("../health")  // Go up one level from /api/v2 to /health
    suspend fun getHealth(): Map<String, String>
}