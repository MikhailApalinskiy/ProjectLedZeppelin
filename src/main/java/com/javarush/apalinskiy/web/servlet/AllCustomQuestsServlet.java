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

/**
 * Servlet responsible for displaying the list of all published custom quests.
 *
 * <p>This servlet provides a paginated view of all quests available in the global catalog.
 * It is typically used on the public “All Quests” page, accessible to both logged-in and
 * anonymous users.</p>
 *
 * <p>The servlet supports search and pagination through query parameters parsed by
 * {@link Web.Params}. It also attaches quest owner names to the list using
 * {@link UserService} for display in JSP.</p>
 */
public class AllCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AllCustomQuestsServlet.class);

    private QuestAuthoringService authoring;
    private UserService userService;

    /**
     * Initializes the servlet and retrieves required beans from the application context.
     *
     * @param c servlet configuration
     * @throws ServletException if service beans are missing from the context
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
     * Handles GET requests and renders a paginated list of all published quests.
     *
     * <p>The method extracts pagination and search parameters using {@link Web#extract(HttpServletRequest)},
     * retrieves paged results from the {@link QuestAuthoringService}, and forwards them to the
     * JSP page defined by {@link WebConst.Jsp#QUESTS_LIST}.</p>
     *
     * <p>Attributes added to the request scope:</p>
     * <ul>
     *     <li>{@code pageTitleKey} — localization key for the page title</li>
     *     <li>{@code showOwnerActions} — flag indicating no owner-specific actions</li>
     *     <li>{@code selfUrl} — the current servlet path for UI links</li>
     * </ul>
     *
     * @param req  HTTP request containing search and pagination parameters
     * @param resp HTTP response used to render the JSP
     * @throws ServletException if JSP forwarding fails
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        Web.Params params = Web.extract(req);
        QuestAuthoringService.Paged<CustomQuest> paged =
                authoring.listAllFromCatalogPaged(params.q, params.page, params.size);
        Web.applyPagedList(req, paged, userService, params.q);
        req.setAttribute("pageTitleKey", "all.quests");
        req.setAttribute("showOwnerActions", Boolean.FALSE);
        req.setAttribute("selfUrl", req.getContextPath() + req.getServletPath());
        log.info("All custom quests paged: q='{}' page={}/{} total={}", params.q, paged.getPage(), paged.getPages(), paged.getTotal());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
