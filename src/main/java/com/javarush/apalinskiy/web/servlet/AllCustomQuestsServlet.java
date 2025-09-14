package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
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
import java.util.List;

/**
 * Servlet responsible for displaying the list of all published custom quests.
 * <p>
 * This servlet provides a read-only view of the catalog for end-users:
 * it lists all quests stored in the catalog repository, attaches owner names,
 * and forwards the data to a JSP view for rendering.
 * <p>
 * <b>Initialization:</b><br>
 * On startup, retrieves {@link QuestAuthoringService} and {@link UserService}
 * from the servlet context. If either service is missing, the servlet fails
 * with {@link UnavailableException}.
 * <p>
 * <b>GET:</b><br>
 * Renders a page with the list of all available custom quests.
 * Flash messages and errors (if any) are pulled from the request/session.
 * Quests are enriched with owner display names and filtered/attached
 * using helper methods in {@link Web}.
 * <p>
 * <b>Security:</b> This servlet is public and does not perform authorization;
 * visibility of actions in the view (e.g. owner actions) is controlled via request attributes.
 *
 * @author Your Name
 * @since 1.0
 */
public class AllCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AllCustomQuestsServlet.class);

    /**
     * Provides access to quest catalog and draft editor operations.
     */
    private QuestAuthoringService authoring;
    /**
     * Provides access to user information, used to resolve quest owners.
     */
    private UserService userService;

    /**
     * Initializes servlet dependencies by looking up context beans for
     * {@link QuestAuthoringService} and {@link UserService}.
     * <p>
     * If either service is not available in the servlet context,
     * the servlet cannot function and is marked unavailable.
     *
     * @param c servlet configuration
     * @throws ServletException     if superclass initialization fails
     * @throws UnavailableException if required services are not found
     */
    @Override
    public void init(ServletConfig c) throws ServletException {
        super.init(c);
        try {
            authoring = Web.ctxBean(c.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            this.userService = Web.ctxBean(c.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
            log.debug("AllCustomQuestsServlet initialized with QuestAuthoringService and UserService");
        } catch (IllegalStateException e) {
            log.error("Initialization failed: QuestAuthoringService or UserService not found", e);
            throw new UnavailableException("QuestAuthoringService or UserService not found");
        }
    }

    /**
     * Handles GET requests by rendering the list of all custom quests.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Pulls flash and error messages from the request/session.</li>
     *   <li>Retrieves all quests from the catalog via {@code authoring}.</li>
     *   <li>Attaches owner display names using {@code userService}.</li>
     *   <li>Logs how many items were found.</li>
     *   <li>Applies additional filtering and attaches quests using
     *       {@link Web#filterAndAttachQuests(HttpServletRequest, List)}.</li>
     *   <li>Sets common view attributes: {@code pageTitleKey}, {@code showOwnerActions}, {@code selfUrl}.</li>
     *   <li>Forwards the request to {@link WebConst.Jsp#QUESTS_LIST} for rendering.</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if forwarding the request/response fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        List<CustomQuest> items = authoring.listAllFromCatalog();
        Web.attachOwnerNamesById(req, items, userService);
        log.info("All custom quests page opened, items={}", items.size());
        Web.filterAndAttachQuests(req, items);
        req.setAttribute("pageTitleKey", "all.quests");
        req.setAttribute("showOwnerActions", Boolean.FALSE);
        req.setAttribute("selfUrl", req.getContextPath() + req.getServletPath());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
