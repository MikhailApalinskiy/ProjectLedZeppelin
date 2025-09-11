package com.javarush.apalinskiy.web.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EdgeSeg")
class EdgeSegTest {

    @Nested
    @DisplayName("constructor & getters")
    class ConstructorAndGetters {

        @Test
        @DisplayName("given valid args when constructed then getters return same values")
        void validArgs() {
            // given
            int from = 1;
            int to = 2;
            String label = "edge";
            double sx = 10.5, sy = 20.5, tx = 30.5, ty = 40.5;
            // when
            EdgeSeg seg = new EdgeSeg(from, to, label, sx, sy, tx, ty);
            // then
            assertEquals(from, seg.getFrom());
            assertEquals(to, seg.getTo());
            assertEquals(label, seg.getLabel());
            assertEquals(sx, seg.getSx());
            assertEquals(sy, seg.getSy());
            assertEquals(tx, seg.getTx());
            assertEquals(ty, seg.getTy());
        }

        @Test
        @DisplayName("given negative coordinates when constructed then preserved")
        void negativeValues() {
            // given
            double sx = -1.0, sy = -2.0, tx = -3.0, ty = -4.0;
            // when
            EdgeSeg seg = new EdgeSeg(10, 20, "neg", sx, sy, tx, ty);
            // then
            assertEquals(-1.0, seg.getSx());
            assertEquals(-2.0, seg.getSy());
            assertEquals(-3.0, seg.getTx());
            assertEquals(-4.0, seg.getTy());
        }

        @Test
        @DisplayName("given null label when constructed then getter returns null")
        void nullLabel() {
            // given
            // when
            EdgeSeg seg = new EdgeSeg(1, 2, null, 0.0, 0.0, 1.0, 1.0);
            // then
            assertNull(seg.getLabel());
        }

        @Test
        @DisplayName("given from==to when constructed then allowed")
        void sameFromTo() {
            // given
            // when
            EdgeSeg seg = new EdgeSeg(5, 5, "loop", 1.0, 2.0, 3.0, 4.0);
            // then
            assertEquals(5, seg.getFrom());
            assertEquals(5, seg.getTo());
            assertEquals("loop", seg.getLabel());
        }
    }
}