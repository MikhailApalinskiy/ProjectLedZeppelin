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

public class MyCustomQuestsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MyCustomQuestsServlet.class);

    private QuestAuthoringService authoring;

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
