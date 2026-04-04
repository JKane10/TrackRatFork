package com.trackrat.android.data.models

/**
 * Transit systems supported by TrackRat.
 * Mirrors iOS TrainSystem enum.
 */
enum class TransitSystem(
    val code: String,
    val displayName: String,
    val isBeta: Boolean = false
) {
    NJT("NJT", "NJ Transit"),
    AMTRAK("AMTRAK", "Amtrak"),
    PATH("PATH", "PATH"),
    PATCO("PATCO", "PATCO", isBeta = true),
    LIRR("LIRR", "Long Island Rail Road", isBeta = true),
    MNR("MNR", "Metro-North", isBeta = true),
    SUBWAY("SUBWAY", "NYC Subway", isBeta = true),
    METRA("METRA", "Metra", isBeta = true),
    WMATA("WMATA", "DC Metro", isBeta = true),
    BART("BART", "BART", isBeta = true),
    MBTA("MBTA", "MBTA", isBeta = true);

    companion object {
        fun fromCode(code: String): TransitSystem? = values().find { it.code == code }

        val mainSystems: List<TransitSystem> = values().filter { !it.isBeta }.sortedBy { it.displayName }
        val betaSystems: List<TransitSystem> = values().filter { it.isBeta }.sortedBy { it.displayName }
    }
}
