package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;


public class AllCustomQuestsServlet extends HttpServlet {
    private QuestAuthoringService authoring;

    @Override
    public void init(ServletConfig c) throws ServletException {
        super.init(c);
        try {
            authoring = Web.ctxBean(c.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException("QuestAuthoringService not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        List<CustomQuest> items = authoring.listAllFromCatalog();
        Web.attachQuestLists(req, items);
        req.setAttribute("pageTitleKey", "all.quests");
        req.setAttribute("showOwnerActions", Boolean.FALSE);
        Web.forward(req, resp, WebConst.Jsp.QUESTS_LIST);
    }
}
