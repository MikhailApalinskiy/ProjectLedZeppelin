package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
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
import java.util.List;

/**
 * Public servlet that renders a paginated/filterable list of all published custom quests.
 * <p>
 * The servlet delegates data access to {@link QuestAuthoringService} and prepares
 * view-layer attributes used by a shared JSP list template.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>On {@link #init(ServletConfig)}: resolves {@link QuestAuthoringService} from the servlet context.</li>
 *   <li>If the service is missing, initialization fails with {@link UnavailableException}.</li>
 * </ul>
 *
 * <h3>GET flow</h3>
 * <ol>
 *   <li>Pulls flash messages ({@code FLASH}, {@code ERROR}).</li>
 *   <li>Loads all catalog items via {@link QuestAuthoringService#listAllFromCatalog()}.</li>
 *   <li>Calls {@link Web#filterAndAttachQuests(HttpServletRequest, java.util.List)}
 *       to apply request-driven filters/sorting and attach model attributes for the JSP.</li>
 *   <li>Sets additional attributes:
 *     <ul>
 *       <li>{@code pageTitleKey} = {@code "all.quests"} (i18n key for the title);</li>
 *       <li>{@code showOwnerActions} = {@code false} (owner-only actions hidden for the public page);</li>
 *       <li>{@code selfUrl} = contextPath + servletPath (canonical link to this listing).</li>
 *     </ul>
 *   </li>
 *   <li>Forwards to {@code WebConst.Jsp.QUESTS_LIST}.</li>
 * </ol>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Intended to be publicly accessible; any authentication/authorization should be handled upstream if needed.</li>
 *   <li>Logging includes the total count of items loaded from the catalog.</li>
 * </ul>
 *
 * @see QuestAuthoringService
 * @see CustomQuest
 * @see Web#filterAndAttachQuests(HttpServletRequest, java.util.List)
 * @see WebConst
 */
public class AllCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AllCustomQuestsServlet.class);

    private QuestAuthoringService authoring;

    /**
     * Resolves {@link QuestAuthoringService} from the servlet context.
     *
     * @throws UnavailableException if the service is not found
     */
    @Override
    public void init(ServletConfig c) throws ServletException {
        super.init(c);
        try {
            authoring = Web.ctxBean(c.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            log.debug("AllCustomQuestsServlet initialized with QuestAuthoringService");
        } catch (IllegalStateException e) {
            log.error("Initialization failed: QuestAuthoringService not found", e);
            throw new UnavailableException("QuestAuthoringService not found");
        }
    }

    /**
     * Renders the list of all custom quests using the shared list JSP.
     * <p>
     * Pulls flash messages, loads catalog items, applies request-driven filters/sorting,
     * and forwards to {@code WebConst.Jsp.QUESTS_LIST}.
     * </p>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        List<CustomQuest> items = authoring.listAllFromCatalog();
        log.info("All custom quests page opened, items={}", items.size());
        Web.filterAndAttachQuests(req, items);
        req.setAttribute("pageTitleKey", "all.quests");
        req.setAttribute("showOwnerActions", Boolean.FALSE);
        req.setAttribute("selfUrl", req.getContextPath() + req.getServletPath());
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
