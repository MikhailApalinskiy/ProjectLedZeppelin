package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

public class GoLoadsServlet extends AbstractSlotsServlet {

    @Override
    protected String path() {
        return WebConst.Path.LOADS;
    }

    @Override
    protected String listJsp() {
        return WebConst.Jsp.LOADS;
    }

    @Override
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, String questIdIgnored, int slot, String next)
            throws IOException {
        Optional<SaveStateService.GlobalSlot> opt = saveState.getGlobalSlot(userId, slot);
        if (opt.isEmpty()) {
            Web.redirectKeep(req, resp, path(), WebConst.ParamGroup.SLOT_NAV);
            return;
        }
        SaveStateService.GlobalSlot g = opt.get();
        String qname = (g.questName() != null && !g.questName().isBlank())
                ? g.questName()
                : ("main".equals(g.questId()) ? "Main quest" : "Custom quest");
        String title = (g.title() != null && !g.title().isBlank())
                ? g.title()
                : ("Node #" + g.nodeId());
        req.getSession().setAttribute(WebConst.Attr.FLASH,
                "Slot № " + (slot + 1) + " is loaded" + " — " + qname + " • " + title + ".");
        String qid = (g.questId() == null || g.questId().isBlank() || "main".equals(g.questId())) ? null : g.questId();
        String target = Web.questUrl(req, g.nodeId(), qid);
        resp.sendRedirect(resp.encodeRedirectURL(target));
    }
}
