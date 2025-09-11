package com.javarush.apalinskiy.web.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NodePos")
class NodePosTest {

    @Nested
    @DisplayName("constructor & getters")
    class ConstructorAndGetters {

        @Test
        @DisplayName("given valid args when constructed then getters return same values")
        void validArgs() {
            // given
            int id = 1, x = 10, y = 20;
            boolean fin = true, start = false;
            List<String> labels = List.of("line1", "line2");
            String image = "img.png";
            // when
            NodePos pos = new NodePos(id, x, y, fin, start, labels, image);
            // then
            assertEquals(id, pos.getId());
            assertEquals(x, pos.getX());
            assertEquals(y, pos.getY());
            assertTrue(pos.isFin());
            assertFalse(pos.isStart());
            assertEquals(labels, pos.getLabelLines());
            assertEquals(image, pos.getImage());
        }

        @Test
        @DisplayName("given null labelLines when constructed then replaced with empty list")
        void nullLabelLines() {
            // when
            NodePos pos = new NodePos(1, 2, 3, false, true, null, "pic");
            // then
            assertNotNull(pos.getLabelLines());
            assertTrue(pos.getLabelLines().isEmpty());
        }

        @Test
        @DisplayName("given null image when constructed then image=null")
        void nullImage() {
            // when
            NodePos pos = new NodePos(1, 2, 3, false, false, List.of(), null);
            // then
            assertNull(pos.getImage());
        }

        @Test
        @DisplayName("given blank image when constructed then image=null")
        void blankImage() {
            // when
            NodePos pos = new NodePos(1, 2, 3, false, false, List.of(), "   ");
            // then
            assertNull(pos.getImage());
        }

        @Test
        @DisplayName("given edge case values when constructed then preserved")
        void edgeCaseValues() {
            // given
            int id = -1, x = 0, y = Integer.MAX_VALUE;
            boolean fin = false, start = true;
            List<String> labels = List.of();
            String image = "x";
            // when
            NodePos pos = new NodePos(id, x, y, fin, start, labels, image);
            // then
            assertEquals(id, pos.getId());
            assertEquals(x, pos.getX());
            assertEquals(y, pos.getY());
            assertFalse(pos.isFin());
            assertTrue(pos.isStart());
            assertEquals(labels, pos.getLabelLines());
            assertEquals("x", pos.getImage());
        }
    }
}