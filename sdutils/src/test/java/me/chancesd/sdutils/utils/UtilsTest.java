package me.chancesd.sdutils.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void testIsVersionAtLeast() {
        assertTrue(Utils.isVersionAtLeast("1.16.5", "1.16.4"));
        assertFalse(Utils.isVersionAtLeast("1.16.4", "1.16.5"));
        assertTrue(Utils.isVersionAtLeast("1.16.5", "1.16.5"));
    }

    @Test
    void testStripTags() {
        assertEquals("1.16.5", Utils.stripTags("1.16.5-DEV"));
        assertEquals("1.16.5", Utils.stripTags("1.16.5+build.123"));
    }

    @Test
    void testRoundTo1Decimal() {
        assertEquals(1.2, Utils.roundTo1Decimal(1.23));
        assertEquals(1.0, Utils.roundTo1Decimal(1.04));
    }

    @Test
    void testRoundTo2Decimal() {
        assertEquals(1.23, Utils.roundTo2Decimal(1.234));
        assertEquals(1.00, Utils.roundTo2Decimal(1.004));
    }
}
