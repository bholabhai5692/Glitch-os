package com.example.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationCoordinatesTest {
    @Test
    fun parsesValidCoordinates() {
        assertEquals(
            LocationCoordinates(28.6139, 77.2090),
            LocationCoordinates.parse(" 28.6139 ", "77.2090")
        )
    }

    @Test
    fun rejectsOutOfRangeCoordinates() {
        assertNull(LocationCoordinates.parse("90.01", "0"))
        assertNull(LocationCoordinates.parse("0", "-180.01"))
    }

    @Test
    fun rejectsMissingAndNonFiniteCoordinates() {
        assertNull(LocationCoordinates.parse("", "10"))
        assertNull(LocationCoordinates.parse("NaN", "10"))
        assertNull(LocationCoordinates.parse("10", "Infinity"))
    }
}
