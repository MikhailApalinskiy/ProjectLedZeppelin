package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.application.quests.QuestAuthoringService;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.quest.CustomQuest;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserPublishedQuestsServlet extends HttpServlet {

    private UserService userService;
    private QuestAuthoringService authoring;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        try {
            this.userService = Web.ctxBean(ctx, WebConst.Ctx.USER_SERVICE, UserService.class);
            this.authoring = Web.ctxBean(ctx, WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException("Required services not found: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Web.pullFlash(req, WebConst.Attr.FLASH);
        Web.pullFlash(req, WebConst.Attr.ERROR);
        String userId = Web.trimOrNull(req.getParameter("id"));
        User viewUser = null;
        if (userId != null) {
            Optional<User> opt = userService.findById(userId);
            viewUser = opt.orElse(null);
        }
        List<CustomQuest> items;
        if (viewUser == null) {
            items = Collections.emptyList();
        } else {
            items = authoring.listOwnerFromCatalog(viewUser.getUserLogin());
            User me = (User) req.getSession().getAttribute(WebConst.Attr.USER);
            boolean isOwner = (me != null && me.getUserId() != null && me.getUserId().equals(viewUser.getUserId()));
            boolean isAdmin = (me != null && me.getRole() == Role.ADMIN);
            if (!isOwner && !isAdmin) {
                items = items.stream().filter(CustomQuest::isPublished).collect(Collectors.toList());
            }
        }
        req.setAttribute("viewUser", viewUser);
        Web.attachQuestLists(req, items);
        String selfUrl = req.getContextPath() + req.getServletPath()
                + (userId == null ? "" : "?id=" + Web.urlEncode(userId));
        req.setAttribute("selfUrl", selfUrl);
        Web.forward(req, resp, WebConst.Jsp.USER_QUESTS);
    }
}
