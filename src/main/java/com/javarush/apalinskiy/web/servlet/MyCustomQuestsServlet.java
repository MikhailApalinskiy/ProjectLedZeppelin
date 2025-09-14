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
 * Servlet responsible for displaying quests owned by the currently authenticated user.
 * <p>
 * Provides a personalized view into the quest catalog, listing only the quests
 * created by the user. The list is enriched with owner display names and
 * forwarded to the shared JSP for rendering.
 * <p>
 * <b>Initialization:</b><br>
 * Requires {@link QuestAuthoringService} and {@link UserService} to be present
 * in the servlet context. If either is missing, the servlet is marked unavailable.
 * <p>
 * <b>GET:</b><br>
 * Displays all quests owned by the logged-in user. Anonymous access is denied
 * and redirected to the login page.
 * <p>
 * Request attributes set for the view:
 * <ul>
 *   <li>{@code pageTitleKey} = "my.quests"</li>
 *   <li>{@code showOwnerActions} = true (so that edit/delete actions are visible)</li>
 *   <li>{@code selfUrl} = current servlet URL</li>
 * </ul>
 *
 * @author Your Name
 * @since 1.0
 */
public class MyCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MyCustomQuestsServlet.class);

    /**
     * Provides access to quest catalog operations.
     */
    private QuestAuthoringService authoring;

    /**
     * Provides user lookup for attaching owner names to quests.
     */
    private UserService userService;

    /**
     * Initializes backend services from the servlet context.
     * <p>
     * Required:
     * <ul>
     *   <li>{@link QuestAuthoringService}</li>
     *   <li>{@link UserService}</li>
     * </ul>
     * If either service is missing, the servlet is marked unavailable.
     *
     * @param config servlet configuration
     * @throws ServletException     if superclass initialization fails
     * @throws UnavailableException if required services are not found
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
     * Handles GET requests by displaying the list of quests owned by the current user.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Checks session for {@link User}; redirects to login if absent.</li>
     *   <li>Pulls flash and error messages from the request/session.</li>
     *   <li>Retrieves quests owned by the current user from {@code authoring}.</li>
     *   <li>Attaches owner names to quests via {@code userService}.</li>
     *   <li>Logs the number of items retrieved.</li>
     *   <li>Applies filtering and attaches quests using
     *       {@link Web#filterAndAttachQuests(HttpServletRequest, List)}.</li>
     *   <li>Sets view attributes: {@code pageTitleKey}, {@code showOwnerActions}, {@code selfUrl}.</li>
     *   <li>Forwards to {@link WebConst.Jsp#QUESTS_LIST}.</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if forwarding or redirect fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (user == null) {
            log.warn("Unauthorized access attempt to my quests, redirecting to login");
            Web.redirect(req, resp, WebConst.Path.LOGIN, Map.of());
            return;
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        List<CustomQuest> items = authoring.listOwnerFromCatalog(user.getUserId());
        Web.attachOwnerNamesById(req, items, userService);
        log.info("User {} (id={}) requested their quests, found {} item(s)",
                user.getUserLogin(), user.getUserId(), items.size());
        Web.filterAndAttachQuests(req, items);
        req.setAttribute("pageTitleKey", "my.quests");
        req.setAttribute("showOwnerActions", Boolean.TRUE);
        req.setAttribute("selfUrl", req.getContextPath() + req.getServletPath());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
