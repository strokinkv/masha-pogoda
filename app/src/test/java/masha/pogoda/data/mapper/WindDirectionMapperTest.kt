package masha.pogoda.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class WindDirectionMapperTest {
    @Test
    fun degrees() {
        assertEquals("С", degreesToRu(0))
        assertEquals("В", degreesToRu(90))
        assertEquals("Ю", degreesToRu(180))
        assertEquals("З", degreesToRu(270))
        assertEquals("СЗ", degreesToRu(315))
    }
}

