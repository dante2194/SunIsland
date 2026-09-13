package com.sunisland.astronomy

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset
import kotlin.math.*

enum class SolarEvent { SUNRISE, SOLAR_NOON, SUNSET, SOLAR_MIDNIGHT }

data class SolarTimes(
    val date: LocalDate,
    val sunrise: ZonedDateTime?,
    val solarNoon: ZonedDateTime,
    val sunset: ZonedDateTime?,
    val solarMidnight: ZonedDateTime
) {
    fun at(event: SolarEvent): ZonedDateTime = when (event) {
        SolarEvent.SUNRISE -> sunrise ?: solarNoon
        SolarEvent.SOLAR_NOON -> solarNoon
        SolarEvent.SUNSET -> sunset ?: solarNoon
        SolarEvent.SOLAR_MIDNIGHT -> solarMidnight
    }
}

enum class EventChoice {
    NEXT, SUNRISE, SOLAR_NOON, SUNSET, SOLAR_MIDNIGHT;

    fun toSolarEvent(): SolarEvent = when (this) {
        NEXT -> SolarEvent.SUNSET
        SUNRISE -> SolarEvent.SUNRISE
        SOLAR_NOON -> SolarEvent.SOLAR_NOON
        SUNSET -> SolarEvent.SUNSET
        SOLAR_MIDNIGHT -> SolarEvent.SOLAR_MIDNIGHT
    }
}

/** NOAA solar-position approximation using the fractional-year equations. */
object SolarCalculator {
    private const val ZENITH = 90.8333

    fun calculate(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): SolarTimes {
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)

        val noonMinutes = solarNoonUtcMinutes(date, longitude)
        val noon = utcMinutesToZoned(date, noonMinutes, zone)
        val declination = solarDeclination(date)
        val cosHour = (
            cos(Math.toRadians(ZENITH)) -
                sin(Math.toRadians(latitude)) * sin(Math.toRadians(declination))
            ) / (cos(Math.toRadians(latitude)) * cos(Math.toRadians(declination)))

        val hourAngleDegrees = when {
            cosHour > 1.0 -> null // Sun remains below the horizon.
            cosHour < -1.0 -> null // Sun remains above the horizon.
            else -> Math.toDegrees(acos(cosHour))
        }
        val sunrise = hourAngleDegrees?.let { utcMinutesToZoned(date, noonMinutes - it * 4.0, zone) }
        val sunset = hourAngleDegrees?.let { utcMinutesToZoned(date, noonMinutes + it * 4.0, zone) }

        val tomorrow = date.plusDays(1)
        val tomorrowNoon = utcMinutesToZoned(tomorrow, solarNoonUtcMinutes(tomorrow, longitude), zone)
        val midnight = noon.plusSeconds(Duration.between(noon, tomorrowNoon).seconds / 2)

        return SolarTimes(date, sunrise, noon, sunset, midnight)
    }

    fun nextEvent(now: ZonedDateTime, latitude: Double, longitude: Double, requested: EventChoice): Pair<SolarEvent, ZonedDateTime> {
        val today = calculate(now.toLocalDate(), latitude, longitude, now.zone)
        val tomorrow = calculate(now.toLocalDate().plusDays(1), latitude, longitude, now.zone)
        val candidates = if (requested == EventChoice.NEXT) {
            listOfNotNull(
                today.sunrise?.let { SolarEvent.SUNRISE to it },
                SolarEvent.SOLAR_NOON to today.solarNoon,
                today.sunset?.let { SolarEvent.SUNSET to it },
                SolarEvent.SOLAR_MIDNIGHT to today.solarMidnight,
                tomorrow.sunrise?.let { SolarEvent.SUNRISE to it },
                SolarEvent.SOLAR_NOON to tomorrow.solarNoon,
                tomorrow.sunset?.let { SolarEvent.SUNSET to it },
                SolarEvent.SOLAR_MIDNIGHT to tomorrow.solarMidnight
            )
        } else {
            val event = requested.toSolarEvent()
            listOfNotNull(
                today.sunrise?.takeIf { event == SolarEvent.SUNRISE }?.let { event to it },
                today.solarNoon.takeIf { event == SolarEvent.SOLAR_NOON }?.let { event to it },
                today.sunset?.takeIf { event == SolarEvent.SUNSET }?.let { event to it },
                today.solarMidnight.takeIf { event == SolarEvent.SOLAR_MIDNIGHT }?.let { event to it },
                tomorrow.sunrise?.takeIf { event == SolarEvent.SUNRISE }?.let { event to it },
                tomorrow.solarNoon.takeIf { event == SolarEvent.SOLAR_NOON }?.let { event to it },
                tomorrow.sunset?.takeIf { event == SolarEvent.SUNSET }?.let { event to it },
                tomorrow.solarMidnight.takeIf { event == SolarEvent.SOLAR_MIDNIGHT }?.let { event to it }
            )
        }
        return candidates.firstOrNull { it.second.isAfter(now) }
            ?: (SolarEvent.SOLAR_NOON to tomorrow.solarNoon)
    }

    private fun solarNoonUtcMinutes(date: LocalDate, longitude: Double): Double {
        val eot = equationOfTime(date)
        return 720.0 - (4.0 * longitude) - eot
    }

    private fun solarDeclination(date: LocalDate): Double {
        val gamma = fractionalYear(date)
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        return Math.toDegrees(decl)
    }

    private fun equationOfTime(date: LocalDate): Double {
        val g = fractionalYear(date)
        return 229.18 * (
            0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) -
                0.014615 * cos(2 * g) - 0.040849 * sin(2 * g)
            )
    }

    private fun fractionalYear(date: LocalDate): Double {
        val days = if (date.lengthOfYear() == 366) 366.0 else 365.0
        val hour = 12.0
        return (2.0 * Math.PI / days) * (date.dayOfYear - 1 + (hour - 12.0) / 24.0)
    }

    private fun utcMinutesToZoned(date: LocalDate, utcMinutes: Double, zone: ZoneId): ZonedDateTime {
        val instant = date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds((utcMinutes * 60.0).roundToLong())
        return instant.atZone(ZoneOffset.UTC).withZoneSameInstant(zone)
    }
}
