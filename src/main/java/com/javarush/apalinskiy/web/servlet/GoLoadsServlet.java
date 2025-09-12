package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.service.save.SaveStateService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class GoLoadsServlet extends AbstractSlotsServlet {

    private static final Logger log = LoggerFactory.getLogger(GoLoadsServlet.class);

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
            log.debug("Load slot: empty slot userId={} slot={}", userId, slot);
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
        log.info("Load slot success userId={} slot={} nodeId={} questId={} target={}",
                userId, slot, g.nodeId(), (qid == null ? "main" : qid), target);
        resp.sendRedirect(resp.encodeRedirectURL(target));
    }
}
