package masha.pogoda.data.mapper

import masha.pogoda.domain.model.WeatherCode
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAdviceTest {
    @Test
    fun advice() {
        assertTrue(weatherToAdvice(20, WeatherCode.RAIN, 80, 3.0).contains("зонт"))
        assertTrue(weatherToAdvice(-10, WeatherCode.SNOW, 30, 2.0).contains("тёпл"))
        assertTrue(weatherToAdvice(25, WeatherCode.CLEAR, 0, 1.0).contains("гул"))
    }
}

