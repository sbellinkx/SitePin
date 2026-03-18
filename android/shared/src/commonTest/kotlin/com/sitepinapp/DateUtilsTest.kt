package com.sitepinapp

import com.sitepinapp.data.model.DateUtils
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*

class DateUtilsTest {

    @Test
    fun toISO8601ProducesValidFormat() {
        val epochMillis = 1700000000000L // 2023-11-14T22:13:20Z
        val iso = DateUtils.toISO8601(epochMillis)
        assertTrue(iso.contains("2023-11-14"), "Expected date to contain 2023-11-14, got: $iso")
        assertTrue(iso.contains("T"), "Expected ISO8601 format with T separator")
    }

    @Test
    fun fromISO8601ParsesValidString() {
        val iso = "2023-11-14T22:13:20Z"
        val millis = DateUtils.fromISO8601(iso)
        assertEquals(1700000000000L, millis)
    }

    @Test
    fun roundTripISO8601() {
        val original = 1700000000000L
        val iso = DateUtils.toISO8601(original)
        val roundTripped = DateUtils.fromISO8601(iso)
        assertEquals(original, roundTripped)
    }

    @Test
    fun fromISO8601InvalidStringReturnsCurrent() {
        val before = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val result = DateUtils.fromISO8601("not-a-date")
        val after = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        assertTrue(result in before..after, "Invalid date should return current time")
    }

    @Test
    fun fromISO8601EmptyStringReturnsCurrent() {
        val before = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val result = DateUtils.fromISO8601("")
        val after = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        assertTrue(result in before..after)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun base64RoundTrip() {
        val original = byteArrayOf(0, 1, 2, 127, -128, -1)
        val encoded = DateUtils.encodeBase64(original)
        val decoded = DateUtils.decodeBase64(encoded)
        assertContentEquals(original, decoded)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun base64EmptyData() {
        val encoded = DateUtils.encodeBase64(byteArrayOf())
        val decoded = DateUtils.decodeBase64(encoded)
        assertContentEquals(byteArrayOf(), decoded)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun base64LargeData() {
        val original = ByteArray(10000) { (it % 256).toByte() }
        val encoded = DateUtils.encodeBase64(original)
        val decoded = DateUtils.decodeBase64(encoded)
        assertContentEquals(original, decoded)
    }
}
