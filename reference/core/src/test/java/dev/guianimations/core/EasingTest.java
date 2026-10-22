package dev.guianimations.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EasingTest {
    @ParameterizedTest
    @EnumSource(Easing.class)
    void curvesStayBoundedMonotonicAndMeetEndpoints(Easing easing) {
        assertEquals(0.0, easing.apply(0.0));
        assertEquals(1.0, easing.apply(1.0));
        assertEquals(0.0, easing.apply(-1.0));
        assertEquals(1.0, easing.apply(2.0));
        double previous = 0.0;
        for (int step = 1; step <= 1_000; step++) {
            double value = easing.apply(step / 1_000.0);
            assertTrue(value >= previous && value <= 1.0);
            previous = value;
        }
        assertThrows(IllegalArgumentException.class, () -> easing.apply(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> easing.apply(Double.POSITIVE_INFINITY));
    }
}
