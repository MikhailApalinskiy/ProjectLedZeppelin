package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


public class DeleteQuestServlet extends HttpServlet {

    private transient QuestAuthoringService authoring;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(config.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException("QuestAuthoringService not found in ServletContext");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String back = req.getContextPath() + WebConst.Path.MY_QUESTS;
        User user = (User) req.getSession().getAttribute(WebConst.Attr.USER);
        if (user == null) {
            resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + WebConst.Path.LOGIN));
            return;
        }
        String questId = Web.trimOrNull(req.getParameter(WebConst.Param.ID));
        if (questId == null) {
            req.getSession().setAttribute(WebConst.Attr.ERROR, "The quest ID is not specified.");
            resp.sendRedirect(resp.encodeRedirectURL(back));
            return;
        }
        try {
            boolean removed = authoring.deleteFromCatalogIfOwner(questId, user.getUserLogin());
            if (removed) {
                req.getSession().setAttribute(WebConst.Attr.FLASH, "The quest has been deleted.");
            } else {
                req.getSession().setAttribute(WebConst.Attr.ERROR, "Cannot be deleted: not found or you are not the owner.");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            req.getSession().setAttribute(WebConst.Attr.ERROR, "Deletion error: " + ex.getMessage());
        }
        resp.sendRedirect(resp.encodeRedirectURL(back));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
