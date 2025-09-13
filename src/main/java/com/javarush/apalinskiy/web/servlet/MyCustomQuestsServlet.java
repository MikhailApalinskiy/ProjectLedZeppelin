package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
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
 * Displays the current user's own published custom quests.
 * <p>
 * Requires an authenticated {@link User} in the session. The servlet pulls the list from
 * {@link QuestAuthoringService#listOwnerFromCatalog(String)} and then delegates common
 * filtering/sorting/pagination to {@link Web#filterAndAttachQuests(HttpServletRequest, java.util.List)}.
 * Results are rendered via a shared JSP.
 * </p>
 *
 * <h3>View & attributes</h3>
 * <ul>
 *   <li>Forwards to: {@code WebConst.Jsp.QUESTS_LIST}</li>
 *   <li>Request attributes set:
 *     <ul>
 *       <li>{@code pageTitleKey} = {@code "my.quests"}</li>
 *       <li>{@code showOwnerActions} = {@code true} (enables owner-only actions in the view)</li>
 *       <li>{@code selfUrl} = {@code contextPath + servletPath}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Flash</h3>
 * <p>Reads and exposes {@code FLASH} and {@code ERROR} messages via {@link Web#pullFlash}.</p>
 *
 * @see QuestAuthoringService
 * @see CustomQuest
 * @see Web#filterAndAttachQuests(HttpServletRequest, java.util.List)
 * @see WebConst
 */
public class MyCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MyCustomQuestsServlet.class);

    private QuestAuthoringService authoring;

    /**
     * Resolves {@link QuestAuthoringService} from the servlet context.
     *
     * @param config servlet config provided by the container
     * @throws ServletException     if initialization fails
     * @throws UnavailableException if the authoring service is missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(config.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
        } catch (IllegalStateException e) {
            log.error("Init failed: QuestAuthoringService not found in ServletContext");
            throw new UnavailableException("QuestAuthoringService not found in ServletContext");
        }
        log.debug("MyCustomQuestsServlet initialized");
    }

    /**
     * Renders the list of the current user's custom quests.
     * <p>
     * Flow:
     * <ol>
     *   <li>Ensure the user is authenticated (expects {@code WebConst.Attr.USER} in session). If absent, redirects to login.</li>
     *   <li>Pull flash messages ({@code FLASH}, {@code ERROR}).</li>
     *   <li>Load owner's quests via {@link QuestAuthoringService#listOwnerFromCatalog(String)}.</li>
     *   <li>Apply request-driven filtering/sorting and attach list attributes via {@link Web#filterAndAttachQuests}.</li>
     *   <li>Set additional view flags ({@code pageTitleKey}, {@code showOwnerActions}, {@code selfUrl}).</li>
     *   <li>Forward to {@code WebConst.Jsp.QUESTS_LIST}.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if I/O errors occur
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (user == null) {
            log.warn("Unauthorized access attempt to my quests, redirecting to login");
            Web.redirect(req, resp, WebConst.Path.LOGIN, java.util.Map.of());
            return;
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        List<CustomQuest> items = authoring.listOwnerFromCatalog(user.getUserLogin());
        log.info("User {} (id={}) requested their quests, found {} item(s)",
                user.getUserLogin(), user.getUserId(), items.size());
        Web.filterAndAttachQuests(req, items);
        req.setAttribute("pageTitleKey", "my.quests");
        req.setAttribute("showOwnerActions", Boolean.TRUE);
        req.setAttribute("selfUrl", req.getContextPath() + req.getServletPath());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
