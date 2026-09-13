package com.sunisland

import com.sunisland.astronomy.SolarCalculator
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarCalculatorTest {
    @Test fun sofiaSummerHasSunriseBeforeSunset() {
        val t = SolarCalculator.calculate(LocalDate.of(2026, 6, 21), 42.6977, 23.3219, ZoneId.of("Europe/Sofia"))
        assertTrue(t.sunrise != null)
        assertTrue(t.sunset != null)
        assertTrue(t.sunrise!!.isBefore(t.solarNoon))
        assertTrue(t.solarNoon.isBefore(t.sunset!!))
    }

    @Test fun polarCircleCanHaveNoSunrise() {
        val t = SolarCalculator.calculate(LocalDate.of(2026, 12, 21), 80.0, 0.0, ZoneId.of("UTC"))
        assertTrue(t.sunrise == null || t.sunset == null)
    }
}
