package io.github.lucafregona.unscroll.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStatusTest {
    private val self = "io.github.lucafregona.unscroll/io.github.lucafregona.unscroll.service.UnscrollService"

    @Test fun nullAndEmptyMeanOff() {
        assertFalse(isServiceEnabled(null, self))
        assertFalse(isServiceEnabled("", self))
    }

    @Test fun theFullClassNameMatches() = assertTrue(isServiceEnabled(self, self))

    @Test fun theShortClassNameMatches() =
        assertTrue(isServiceEnabled("io.github.lucafregona.unscroll/.service.UnscrollService", self))

    @Test fun aListSeparatedByColonsMatchesWhenOurServiceIsOneOfThem() {
        val list = "com.example.reader/.ReaderService:io.github.lucafregona.unscroll/.service.UnscrollService:com.example.other/.Other"
        assertTrue(isServiceEnabled(list, self))
    }

    @Test fun anotherServiceDoesNotMatch() {
        assertFalse(isServiceEnabled("com.example.reader/.ReaderService", self))
        assertFalse(isServiceEnabled("io.github.lucafregona.unscroll/.service.OtherService", self))
    }
}
