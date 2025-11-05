package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Servlet responsible for displaying the list of quests created by the current user.
 *
 * <p>Accessible only to authenticated users. Retrieves paged results from
 * {@link QuestAuthoringService} and applies pagination parameters
 * via {@link Web#applyPagedList}.</p>
 *
 * <p>Forwards the result to the standard quest list JSP view.</p>
 */
public class MyCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MyCustomQuestsServlet.class);

    private QuestAuthoringService authoring;
    private UserService userService;

    /**
     * Initializes required services from the servlet context.
     *
     * @param config servlet configuration
     * @throws ServletException if required beans are missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(config.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
        } catch (IllegalStateException e) {
            log.error("Init failed: QuestAuthoringService or UserService not found in ServletContext");
            throw new UnavailableException("QuestAuthoringService or UserService not found in ServletContext");
        }
        log.debug("MyCustomQuestsServlet initialized");
    }

    /**
     * Displays a paginated list of quests belonging to the current user.
     *
     * <p>If the user is not authenticated, redirects to the login page.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException on forward errors
     * @throws IOException      on redirect or I/O failure
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User me = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (me == null) {
            log.warn("Unauthorized access to my quests -> redirect to login");
            Web.redirect(req, resp, WebConst.Path.LOGIN, Map.of());
            return;
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        Web.Params params = Web.extract(req);
        QuestAuthoringService.Paged<CustomQuest> paged =
                authoring.listOwnerFromCatalogPaged(me.getUserId(), params.q, params.page, params.size, true);
        Web.applyPagedList(req, paged, userService, params.q);
        req.setAttribute("pageTitleKey", "my.quests");
        req.setAttribute("showOwnerActions", Boolean.TRUE);
        req.setAttribute("selfUrl", req.getContextPath() + req.getServletPath());
        log.info("My quests paged: userId={} q='{}' page={}/{} total={}",
                me.getUserId(), params.q, paged.getPage(), paged.getPages(), paged.getTotal());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
