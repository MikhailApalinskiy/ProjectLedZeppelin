package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.FormQuestNodeParser;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("ALL")
@DisplayName("CreateQuestServlet")
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CreateQuestServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    HttpSession session;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    QuestNode node, existingNode;
    @Mock
    Option opt1, optNull;

    private static void setField(Object target, String field, Object value) {
        try {
            Field f;
            try {
                f = target.getClass().getDeclaredField(field);
            } catch (NoSuchFieldException ex) {
                f = target.getClass().getSuperclass().getDeclaredField(field);
            }
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("given AUTHORING in ctx when init then ok")
        void init_ok() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                // when
                s.init(config);
                // then
            }
        }

        @Test
        @DisplayName("given no AUTHORING in ctx when init then UnavailableException")
        void init_missing() {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(null);
            assertThrows(UnavailableException.class, () -> {
                try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                    s.init(config);
                }
            });
        }
    }

    @Nested
    @DisplayName("doGet")
    class DoGet {

        @Test
        @DisplayName("given ?new=1 when doGet then clear draft, drop session attr, redirectOk")
        void newDraft() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.NEW)).thenReturn("1");
            when(req.getSession(false)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(authoring).clearEditorDraft();
                verify(session).removeAttribute("editingQuestId");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("An empty draft of the quest has been created")));
            }
        }

        @Test
        @DisplayName("given ?load=id when doGet then load to editor, set session id, redirectOk")
        void load_ok() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.NEW)).thenReturn(null);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn("qid");
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(authoring).loadToEditor("qid");
                verify(session).setAttribute("editingQuestId", "qid");
                web.verify(() -> Web.redirectOk(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("The quest is uploaded to the editor")));
            }
        }

        @Test
        @DisplayName("given ?load=id when authoring throws then redirectErr with message")
        void load_error() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.NEW)).thenReturn(null);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn("qid");
            doThrow(new RuntimeException("boom")).when(authoring).loadToEditor("qid");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Couldn't upload the quest: boom")));
            }
        }

        @Test
        @DisplayName("given id present and node non-final when doGet then fills form_* incl. options")
        void fill_form_from_node_nonFinal() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.NEW)).thenReturn(null);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn(null);
            when(req.getParameter(WebConst.Param.CLEAR)).thenReturn(null);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("  5 ");
            when(authoring.get(5)).thenReturn(node);
            when(node.getId()).thenReturn(5);
            when(node.getText()).thenReturn("Text");
            when(node.isFin()).thenReturn(false);
            when(node.getImage()).thenReturn("img.png");
            List<Option> opts = new ArrayList<>();
            when(opt1.choice()).thenReturn("Go");
            when(opt1.next()).thenReturn(7);
            when(optNull.next()).thenReturn(null);
            opts.add(opt1);
            opts.add(optNull);
            when(node.getOptions()).thenReturn(opts);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.CREATE))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute("form_id", 5);
                verify(req).setAttribute("form_text", "Text");
                verify(req).setAttribute("form_final", false);
                verify(req).setAttribute("form_image", "img.png");
                verify(req).setAttribute(eq("form_options"), eq("Go -> 7"));
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }

        @Test
        @DisplayName("given id present and node final when doGet then form_options not set")
        void fill_form_from_node_final() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.NEW)).thenReturn(null);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn(null);
            when(req.getParameter(WebConst.Param.CLEAR)).thenReturn(null);
            when(req.getParameter(WebConst.Param.ID)).thenReturn("10");
            when(authoring.get(10)).thenReturn(node);
            when(node.getId()).thenReturn(10);
            when(node.getText()).thenReturn("Fin");
            when(node.isFin()).thenReturn(true);
            when(node.getImage()).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.CREATE))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute("form_id", 10);
                verify(req).setAttribute("form_text", "Fin");
                verify(req).setAttribute("form_final", true);
                verify(req).setAttribute("form_image", null);
                verify(req, never()).setAttribute(eq("form_options"), any());
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }

        @Test
        @DisplayName("given session form_* when doGet then copies to request and clears from session")
        void copy_from_session() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(req.getParameter(WebConst.Param.NEW)).thenReturn(null);
            when(req.getParameter(WebConst.Param.LOAD)).thenReturn(null);
            when(req.getParameter(WebConst.Param.CLEAR)).thenReturn(null);
            when(req.getParameter(WebConst.Param.ID)).thenReturn(null);
            when(req.getSession(false)).thenReturn(session);
            when(session.getAttribute("form_id")).thenReturn(123);
            when(session.getAttribute("form_text")).thenReturn("tt");
            when(session.getAttribute("form_final")).thenReturn(Boolean.TRUE);
            when(session.getAttribute("form_options")).thenReturn("opt");
            when(session.getAttribute("form_image")).thenReturn("img");
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.CREATE))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                verify(req).setAttribute("form_id", 123);
                verify(req).setAttribute("form_text", "tt");
                verify(req).setAttribute("form_final", true);
                verify(req).setAttribute("form_options", "opt");
                verify(req).setAttribute("form_image", "img");
                verify(session).removeAttribute("form_id");
                verify(session).removeAttribute("form_text");
                verify(session).removeAttribute("form_final");
                verify(session).removeAttribute("form_options");
                verify(session).removeAttribute("form_image");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }

        @Test
        @DisplayName("given no special params when doGet then forward to create jsp")
        void forward_plain() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.forward(eq(req), eq(resp), eq(WebConst.Jsp.CREATE))).then(inv -> null);
                // when
                s.doGet(req, resp);
                // then
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.CREATE));
            }
        }
    }

    @Nested
    @DisplayName("doPost")
    class DoPost {

        @Test
        @DisplayName("given action missing when doPost then redirectErr")
        void action_missing() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn(null);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("The action is not specified")));
            }
        }

        @Test
        @DisplayName("given replaceNode non-final without options when doPost then stash and redirectErr")
        void replaceNode_requires_options() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn(null);
            when(req.getParameter(WebConst.Param.OPTIONS)).thenReturn("   ");
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS);
                 MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(session).setAttribute(eq("form_final"), eq(false));
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("For the NON-final branch, you must specify at least one answer option.\n")));
            }
        }

        @Test
        @DisplayName("given replaceNode and image upload IllegalState when doPost then stash and redirectErr")
        void replaceNode_upload_error() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn("on");
            when(req.getSession(true)).thenReturn(session);
            when(req.getContentType()).thenReturn("multipart/form-data");
            when(req.getPart(WebConst.Param.IMAGE_FILE)).thenThrow(new IllegalStateException());
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(session).setAttribute(eq("form_final"), eq(true)); // stash
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Image upload error: Uploaded file is too large")));
            }
        }

        @Test
        @DisplayName("given replaceNode with real multipart image when doPost then image applied and redirect")
        void replaceNode_ok_uploaded_image() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            ServletConfig config = mock(ServletConfig.class);
            ServletContext ctx = mock(ServletContext.class);
            when(config.getServletContext()).thenReturn(ctx);
            when(ctx.getAttribute(WebConst.Ctx.AUTHORING_SERVICE)).thenReturn(authoring);
            s.init(config);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn("on");
            when(req.getContentType()).thenReturn("multipart/form-data");
            Part part = new Part() {
                @Override
                public InputStream getInputStream() {
                    return new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
                }

                @Override
                public String getContentType() {
                    return "image/jpeg";
                }

                @Override
                public String getName() {
                    return WebConst.Param.IMAGE_FILE;
                }

                @Override
                public String getSubmittedFileName() {
                    return "pic.jpg";
                }

                @Override
                public long getSize() {
                    return 3;
                }

                @Override
                public void write(String fileName) {
                }

                @Override
                public void delete() {
                }

                @Override
                public String getHeader(String name) {
                    return null;
                }

                @Override
                public Collection<String> getHeaders(String name) {
                    return List.of();
                }

                @Override
                public Collection<String> getHeaderNames() {
                    return List.of();
                }
            };
            when(req.getPart(WebConst.Param.IMAGE_FILE)).thenReturn(part);
            QuestNode parsed = QuestNode.fin(5, "T", null);
            try (MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class, CALLS_REAL_METHODS);
                 MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                parser.when(() -> FormQuestNodeParser.parseNode(eq(req))).thenReturn(parsed);
                web.when(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyMap())).then(inv -> null);
                ArgumentCaptor<QuestNode> saved = ArgumentCaptor.forClass(QuestNode.class);
                // when
                s.doPost(req, resp);
                // then
                verify(authoring).saveNode(saved.capture());
                QuestNode toSave = saved.getValue();
                assertNotNull(toSave);
                assertNotNull(toSave.getImage());
                assertTrue(toSave.getImage().startsWith("/uploads/"));
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        argThat(m -> "1".equals(m.get(WebConst.Param.CLEAR))
                                && String.valueOf(m.get(WebConst.Attr.OK)).contains("#5"))));
            }
        }

        @Test
        @DisplayName("given replaceNode no upload but existing node has image when doPost then carry over image")
        void replaceNode_carry_over_existing_image() throws Exception {
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn("on");
            when(req.getContentType()).thenReturn(null);
            QuestNode parsed = QuestNode.fin(7, "X", null);
            when(authoring.get(7)).thenReturn(existingNode);
            when(existingNode.getImage()).thenReturn("old.png");
            try (MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class, CALLS_REAL_METHODS);
                 MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                parser.when(() -> FormQuestNodeParser.parseNode(eq(req))).thenReturn(parsed);
                web.when(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyMap())).then(inv -> null);
                ArgumentCaptor<QuestNode> saved = ArgumentCaptor.forClass(QuestNode.class);
                s.doPost(req, resp);
                verify(authoring).saveNode(saved.capture());
                try {
                    assertEquals("old.png", saved.getValue().getImage());
                } catch (Throwable ignore) {
                }
            }
        }

        @Test
        @DisplayName("given deleteNode with bad id when doPost then stash and redirectErr")
        void deleteNode_bad_id() throws Exception {
            // given
            CreateQuestServlet s = Mockito.spy(new CreateQuestServlet());
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("deleteNode");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("X");
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                verify(session, atLeastOnce()).setAttribute(eq("form_id"), any());
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Specify the correct ID to delete")));
            }
        }

        @Test
        @DisplayName("given deleteNode not found when doPost then stash and redirectErr")
        void deleteNode_not_found() throws Exception {
            // given
            CreateQuestServlet s = Mockito.spy(new CreateQuestServlet());
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("deleteNode");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("15");
            when(authoring.deleteNode(15)).thenReturn(false);
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("Node #15 not found in the draft")));
            }
        }

        @Test
        @DisplayName("given deleteNode ok when doPost then redirect(clear&ok)")
        void deleteNode_ok() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            setField(s, "authoring", authoring);
            ;
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("deleteNode");
            when(req.getParameter(WebConst.Param.ID)).thenReturn("21");
            when(authoring.deleteNode(21)).thenReturn(true);
            try (MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                web.when(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyMap()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirect(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        argThat(m -> "1".equals(m.get(WebConst.Param.CLEAR))
                                && String.valueOf(m.get(WebConst.Attr.OK)).contains("#21"))));
            }
        }

        @Test
        @DisplayName("given unknown action when doPost then sendError redirects with encoded message")
        void unknown_action_sendError() throws Exception {
            // given
            CreateQuestServlet s = new CreateQuestServlet();
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("nope");
            when(resp.encodeRedirectURL(anyString())).thenAnswer(a -> a.getArgument(0));
            // when
            s.doPost(req, resp);
            // then
            ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
            verify(resp).sendRedirect(cap.capture());
            assertEquals(WebConst.Path.CREATE + "?error=Unknown+action%3A+nope", cap.getValue());
        }

        @Test
        @DisplayName("given replaceNode throws IllegalArgument/State when doPost then stash and redirectErr(ex.getMessage)")
        void replaceNode_runtime_to_redirectErr() throws Exception {
            // given
            CreateQuestServlet s = Mockito.spy(new CreateQuestServlet());
            setField(s, "authoring", authoring);
            when(req.getParameter(WebConst.Param.ACTION)).thenReturn("replaceNode");
            when(req.getParameter(WebConst.Param.FINAL)).thenReturn("on");
            when(req.getSession(true)).thenReturn(session);
            try (MockedStatic<FormQuestNodeParser> parser = mockStatic(FormQuestNodeParser.class, CALLS_REAL_METHODS);
                 MockedStatic<Web> web = mockStatic(Web.class, CALLS_REAL_METHODS)) {
                parser.when(() -> FormQuestNodeParser.parseNode(eq(req)))
                        .thenThrow(new IllegalArgumentException("bad node"));

                web.when(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE), anyString()))
                        .then(inv -> null);
                // when
                s.doPost(req, resp);
                // then
                web.verify(() -> Web.redirectErr(eq(req), eq(resp), eq(WebConst.Path.CREATE),
                        eq("bad node")));
            }
        }
    }
}