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


public class AllCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AllCustomQuestsServlet.class);

    private QuestAuthoringService authoring;

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
