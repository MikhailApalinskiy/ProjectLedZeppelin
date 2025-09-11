package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormQuestNodeParser")
class FormQuestNodeParserTest {

    @Mock
    HttpServletRequest req;

    @Nested
    @DisplayName("parseNode() non-final (textarea)")
    class ParseNodeTextarea {

        @Test
        @DisplayName("parses textarea with various arrows, trims image via Web.trimOrNull")
        void parsesTextareaAndCleansArrows() {
            // Given
            when(req.getParameter("id")).thenReturn(" 10 ");
            when(req.getParameter("text")).thenReturn("Hello");
            when(req.getParameter("final")).thenReturn(null);
            when(req.getParameter("image")).thenReturn("  pic name.png  ");
            when(req.getParameter("options")).thenReturn(
                    """
                            open —> 2
                            take key --> 3\r
                            go→4
                            use =>5
                             \s
                            examine -> 6  \s"""
            );
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("  pic name.png  ")).thenReturn("pic name.png");
                // When
                QuestNode n = FormQuestNodeParser.parseNode(req);
                // Then
                assertFalse(n.isFin());
                assertEquals(10, n.getId());
                assertEquals("Hello", n.getText());
                assertEquals("pic name.png", n.getImage());
                assertEquals(5, n.getOptions().size());
                assertEquals("open", n.getOptions().get(0).getChoice());
                assertEquals(2, n.getOptions().get(0).getNext());
                assertEquals("take key", n.getOptions().get(1).getChoice());
                assertEquals(3, n.getOptions().get(1).getNext());
                assertEquals("go", n.getOptions().get(2).getChoice());
                assertEquals(4, n.getOptions().get(2).getNext());
                assertEquals("use", n.getOptions().get(3).getChoice());
                assertEquals(5, n.getOptions().get(3).getNext());
                assertEquals("examine", n.getOptions().get(4).getChoice());
                assertEquals(6, n.getOptions().get(4).getNext());
            }
        }

        @Test
        @DisplayName("throws on bad textarea line: missing '->'")
        void throwsOnBadTextareaLine() {
            // Given
            when(req.getParameter("id")).thenReturn("1");
            when(req.getParameter("text")).thenReturn("t");
            when(req.getParameter("final")).thenReturn("false");
            when(req.getParameter("image")).thenReturn(null);
            when(req.getParameter("options")).thenReturn("bad line without arrow");
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> FormQuestNodeParser.parseNode(req));
            assertTrue(ex.getMessage().contains("Incorrect option format"));
        }

        @Test
        @DisplayName("throws on non-numeric next in textarea")
        void throwsOnBadNextInTextarea() {
            // Given
            when(req.getParameter("id")).thenReturn("1");
            when(req.getParameter("text")).thenReturn("t");
            when(req.getParameter("final")).thenReturn("no");
            when(req.getParameter("image")).thenReturn(null);
            when(req.getParameter("options")).thenReturn("go -> x");
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> FormQuestNodeParser.parseNode(req));
            assertTrue(ex.getMessage().contains("Invalid number in the field 'option.next'"));
        }
    }

    @Nested
    @DisplayName("parseNode() non-final (indexed)")
    class ParseNodeIndexed {

        @Test
        @DisplayName("parses indexed options: picks only filled pairs, trims choice")
        void parsesIndexedOptions() {
            // Given
            when(req.getParameter("id")).thenReturn("1");
            when(req.getParameter("text")).thenReturn("t");
            when(req.getParameter("final")).thenReturn(null);
            when(req.getParameter("image")).thenReturn(null);
            when(req.getParameter("options")).thenReturn("   ");
            when(req.getParameter("opt_choice_1")).thenReturn("  open  ");
            when(req.getParameter("opt_next_1")).thenReturn(" 2 ");
            when(req.getParameter("opt_choice_2")).thenReturn("   ");
            when(req.getParameter("opt_next_2")).thenReturn("   ");
            when(req.getParameter("opt_choice_3")).thenReturn("x");
            when(req.getParameter("opt_next_3")).thenReturn("5");
            // When
            QuestNode n = FormQuestNodeParser.parseNode(req);
            // Then
            assertFalse(n.isFin());
            assertEquals(2, n.getOptions().size());
            assertEquals("open", n.getOptions().get(0).getChoice());
            assertEquals(2, n.getOptions().get(0).getNext());
            assertEquals("x", n.getOptions().get(1).getChoice());
            assertEquals(5, n.getOptions().get(1).getNext());
        }

        @Test
        @DisplayName("throws on incomplete pair (text without next)")
        void throwsOnIncompletePair() {
            // Given
            when(req.getParameter("id")).thenReturn("1");
            when(req.getParameter("text")).thenReturn("t");
            when(req.getParameter("final")).thenReturn("false");
            when(req.getParameter("image")).thenReturn(null);
            when(req.getParameter("options")).thenReturn(null);
            when(req.getParameter("opt_choice_1")).thenReturn("only text");
            when(req.getParameter("opt_next_1")).thenReturn("   ");
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> FormQuestNodeParser.parseNode(req));
            assertTrue(ex.getMessage().startsWith("Incomplete version №1"));
        }

        @Test
        @DisplayName("throws on non-numeric next in indexed")
        void throwsOnBadNextInIndexed() {
            // Given
            when(req.getParameter("id")).thenReturn("1");
            when(req.getParameter("text")).thenReturn("t");
            when(req.getParameter("final")).thenReturn("false");
            when(req.getParameter("image")).thenReturn(null);
            when(req.getParameter("options")).thenReturn(null);
            when(req.getParameter("opt_choice_1")).thenReturn("go");
            when(req.getParameter("opt_next_1")).thenReturn("bad");
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> FormQuestNodeParser.parseNode(req));
            assertTrue(ex.getMessage().contains("Invalid number in the field 'opt_next_1'"));
        }
    }

    @Nested
    @DisplayName("parseNode() final=true (STRICT_FINAL=false)")
    class ParseNodeFinal {

        @Test
        @DisplayName("ignores options when final=true -> returns fin")
        void ignoresOptionsWhenFinal() {
            // Given
            when(req.getParameter("id")).thenReturn("2");
            when(req.getParameter("text")).thenReturn("end");
            when(req.getParameter("final")).thenReturn("Yes");
            // When
            QuestNode n = FormQuestNodeParser.parseNode(req);
            // Then
            assertTrue(n.isFin());
            assertEquals(2, n.getId());
            assertTrue(n.getOptions().isEmpty());
        }

        @Test
        @DisplayName("image blank -> Web.trimOrNull returns null → node.image=null")
        void imageBlankBecomesNull() {
            // Given
            when(req.getParameter("id")).thenReturn("3");
            when(req.getParameter("text")).thenReturn("end");
            when(req.getParameter("final")).thenReturn("on");
            when(req.getParameter("image")).thenReturn("   ");
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.trimOrNull("   ")).thenReturn(null);
                // When
                QuestNode n = FormQuestNodeParser.parseNode(req);
                // Then
                assertTrue(n.isFin());
                assertNull(n.getImage());
            }
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("invalid id -> IAE with field name")
        void invalidId() {
            // Given
            when(req.getParameter("id")).thenReturn("abc");
            // When / Then
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> FormQuestNodeParser.parseNode(req));
            assertTrue(ex.getMessage().contains("Invalid number in the field 'id'"));
        }
    }
}