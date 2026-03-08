package dev.pola.vexflow.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VFFractionTest {

    @Test
    fun `of reduces 4 over 8 to 1 over 2`() {
        val f = VFFraction.of(4, 8)
        assertEquals(1, f.numerator)
        assertEquals(2, f.denominator)
    }

    @Test
    fun `of normalises negative denominator`() {
        val f = VFFraction.of(3, -4)
        assertEquals(-3, f.numerator)
        assertEquals(4, f.denominator)
    }

    @Test
    fun `plus adds two fractions`() {
        val result = VFFraction.of(1, 4) + VFFraction.of(1, 4)
        assertEquals(VFFraction.of(1, 2), result)
    }

    @Test
    fun `minus subtracts fractions`() {
        val result = VFFraction.of(3, 4) - VFFraction.of(1, 4)
        assertEquals(VFFraction.of(1, 2), result)
    }

    @Test
    fun `times multiplies fractions`() {
        val result = VFFraction.of(2, 3) * VFFraction.of(3, 4)
        assertEquals(VFFraction.of(1, 2), result)
    }

    @Test
    fun `div divides fractions`() {
        val result = VFFraction.of(1, 2) / VFFraction.of(1, 4)
        assertEquals(VFFraction.of(2, 1), result)
    }

    @Test
    fun `compareTo orders correctly`() {
        assertTrue(VFFraction.of(1, 4) < VFFraction.of(1, 2))
        assertTrue(VFFraction.of(1, 1) > VFFraction.of(3, 4))
        assertEquals(0, VFFraction.of(2, 4).compareTo(VFFraction.of(1, 2)))
    }

    @Test
    fun `fromDurationString parses quarter note`() {
        assertEquals(VFFraction.of(1, 4), VFFraction.fromDurationString("4"))
        assertEquals(VFFraction.of(1, 4), VFFraction.fromDurationString("4r"))
    }

    @Test
    fun `fromDurationString parses dotted half`() {
        assertEquals(VFFraction.of(3, 4), VFFraction.fromDurationString("2d"))
    }

    @Test
    fun `fromDurationString returns null for unknown`() {
        assertNull(VFFraction.fromDurationString("xyz"))
    }

    @Test
    fun `doubleValue is correct`() {
        assertEquals(0.25, VFFraction.of(1, 4).doubleValue, 1e-9)
    }

    @Test
    fun `ZERO and ONE constants`() {
        assertEquals(0, VFFraction.ZERO.numerator)
        assertEquals(1, VFFraction.ONE.numerator)
        assertEquals(1, VFFraction.ONE.denominator)
    }
}
