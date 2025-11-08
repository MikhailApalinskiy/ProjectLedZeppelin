package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("AllCustomQuestsServlet (unit)")
@ExtendWith(MockitoExtension.class)
class AllCustomQuestsServletTest {

    @Mock
    ServletConfig config;
    @Mock
    ServletContext ctx;
    @Mock
    QuestAuthoringService authoring;
    @Mock
    UserService userService;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;

    private AllCustomQuestsServlet sut;

    private QuestAuthoringService.Paged<CustomQuest> paged(List<CustomQuest> items, int total, int page, int size) {
        return new QuestAuthoringService.Paged<>(items, total, page, size);
    }

    private void initServletHappy() throws ServletException {
        sut = new AllCustomQuestsServlet();
        when(config.getServletContext()).thenReturn(ctx);
        try (MockedStatic<Web> web = mockStatic(Web.class)) {
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                    .thenReturn(authoring);
            web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                    .thenReturn(userService);
            sut.init(config);
        }
    }

    @Nested
    @DisplayName("init(config)")
    class InitPhase {

        @Test
        @DisplayName("Given beans exist in context — When init — Then fields are set via Web.ctxBean")
        void initOk() throws ServletException {
            // Given
            sut = new AllCustomQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenReturn(authoring);
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class))
                        .thenReturn(userService);
                // When
                sut.init(config);
                // Then
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class));
                web.verify(() -> Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class));
            }
        }

        @Test
        @DisplayName("Given missing beans — When init — Then throws UnavailableException")
        void initMissingBeans() {
            // Given
            sut = new AllCustomQuestsServlet();
            when(config.getServletContext()).thenReturn(ctx);
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                web.when(() -> Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class))
                        .thenThrow(new IllegalStateException("no bean"));
                // When / Then
                assertThrows(UnavailableException.class, () -> sut.init(config));
            }
        }
    }

    @Nested
    @DisplayName("doGet(req, resp)")
    class DoGet {

        @BeforeEach
        void setUp() throws ServletException {
            initServletHappy();
        }

        @Test
        @DisplayName("Given query/page/size — When doGet — Then authoring queried, attributes set, forward to JSP")
        void happyPath() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                // Given
                Web.Params params = new Web.Params("demo", 2, 5);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                web.when(() -> Web.extract(req)).thenReturn(params);
                var items = List.of(mock(CustomQuest.class), mock(CustomQuest.class));
                var pageOut = paged(items, 7, 2, 5);
                when(authoring.listAllFromCatalogPaged("demo", 2, 5)).thenReturn(pageOut);
                web.when(() -> Web.applyPagedList(eq(req), same(pageOut), same(userService), eq("demo"))).thenAnswer(inv -> null);
                when(req.getContextPath()).thenReturn("/app");
                when(req.getServletPath()).thenReturn("/quests");
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).listAllFromCatalogPaged("demo", 2, 5);
                web.verify(() -> Web.applyPagedList(eq(req), same(pageOut), same(userService), eq("demo")));
                verify(req).setAttribute("pageTitleKey", "all.quests");
                verify(req).setAttribute("showOwnerActions", Boolean.FALSE);
                verify(req).setAttribute("selfUrl", "/app/quests");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST));
            }
        }

        @Test
        @DisplayName("Given null query and default paging — When doGet — Then works and forwards")
        void nullQueryDefaults() throws Exception {
            try (MockedStatic<Web> web = mockStatic(Web.class)) {
                Web.Params params = new Web.Params(null, 1, 10);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.FLASH)).thenAnswer(inv -> null);
                web.when(() -> Web.pullFlash(req, WebConst.Attr.ERROR)).thenAnswer(inv -> null);
                web.when(() -> Web.extract(req)).thenReturn(params);
                var pageOut = paged(List.of(), 0, 1, 10);
                when(authoring.listAllFromCatalogPaged(null, 1, 10)).thenReturn(pageOut);
                web.when(() -> Web.applyPagedList(eq(req), same(pageOut), same(userService), isNull()))
                        .thenAnswer(inv -> null);
                when(req.getContextPath()).thenReturn("");
                when(req.getServletPath()).thenReturn("/quests");
                // When
                sut.doGet(req, resp);
                // Then
                verify(authoring).listAllFromCatalogPaged(null, 1, 10);
                web.verify(() -> Web.applyPagedList(eq(req), same(pageOut), same(userService), isNull()));
                verify(req).setAttribute("pageTitleKey", "all.quests");
                verify(req).setAttribute("showOwnerActions", Boolean.FALSE);
                verify(req).setAttribute("selfUrl", "/quests");
                web.verify(() -> Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST));
            }
        }
    }
}