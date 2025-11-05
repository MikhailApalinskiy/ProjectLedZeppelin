package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.Optional;

/**
 * Abstract servlet providing shared logic for handling slot-based save systems.
 *
 * <p>Subclasses define specific endpoints (via {@link #path()} and {@link #listJsp()})
 * and may optionally override {@link #confirmJsp()} to enable confirmation steps.
 * This base class manages common HTTP operations for displaying, updating, and deleting slots.</p>
 *
 * <p>Typical subclasses include save/load slot lists for game progress management.</p>
 */
public abstract class AbstractSlotsServlet extends BaseSaveServlet {

    private static final Logger log = LoggerFactory.getLogger(AbstractSlotsServlet.class);

    /**
     * @return the URL path mapped to this servlet (used for redirects and logging)
     */
    protected abstract String path();

    /**
     * @return the JSP path used to render the list of slots
     */
    protected abstract String listJsp();

    /**
     * @return optional JSP path for confirmation pages (nullable)
     */
    protected String confirmJsp() {
        return null;
    }

    /**
     * Handles GET requests — displays all slots for the authenticated user.
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = requireAuthOrRedirect(req, resp, path());
        if (u == null) {
            return;
        }
        Web.pullFlash(req, WebConst.Attr.FLASH);
        req.setAttribute("slots", buildSlotsAll(String.valueOf(u.getUserId())));
        log.info("Slots GET userId={} path={}", u.getUserId(), path());
        req.getRequestDispatcher(listJsp()).forward(req, resp);
    }

    /**
     * Handles POST requests for slot operations such as GO, DELETE, CONFIRM, and CANCEL.
     *
     * <p>Each operation delegates to its corresponding handler method:
     * {@link #handleGo}, {@link #handleDelete}, {@link #handleConfirm}, or {@link #handleCancel}.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if forwarding fails
     */
    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        User u = requireAuthOrRedirect(req, resp, path());
        if (u == null) {
            return;
        }
        final String userId = String.valueOf(u.getUserId());
        final String op = req.getParameter(WebConst.Param.OP);
        final int slot = Web.parseIntOrDefault(req.getParameter(WebConst.Param.SLOT), -1);
        final String next = Optional.ofNullable(req.getParameter(WebConst.Param.NEXT)).orElse(WebConst.Path.QUEST);
        final String questId = Web.trimOrNull(req.getParameter(WebConst.Param.CUSTOM));
        if (op == null || slot < 0 || slot >= WebConst.SLOT_COUNT) {
            log.warn("Slots POST invalid params userId={} op={} slot={} next={} questId={}",
                    userId, op, slot, next, questId);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }
        switch (op) {
            case WebConst.Op.GO -> {
                log.info("Slots GO userId={} slot={} next={} questId={}", userId, slot, next, questId);
                handleGo(req, resp, userId, questId, slot, next);
            }
            case WebConst.Op.DELETE -> {
                log.info("Slots DELETE userId={} slot={} questId={}", userId, slot, questId);
                handleDelete(req, resp, userId, questId, slot);
            }
            case WebConst.Op.CONFIRM -> {
                if (confirmJsp() == null) {
                    log.warn("Slots CONFIRM not allowed userId={} slot={}", userId, slot);
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
                log.info("Slots CONFIRM userId={} slot={} next={} questId={}", userId, slot, next, questId);
                handleConfirm(req, resp, userId, questId, slot, next);
            }
            case WebConst.Op.CANCEL -> {
                if (confirmJsp() == null) {
                    log.warn("Slots CANCEL not allowed userId={} slot={}", userId, slot);
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
                log.info("Slots CANCEL userId={} slot={} questId={}", userId, slot, questId);
                handleCancel(req, resp, userId, questId, slot);
            }
            default -> {
                log.warn("Slots POST unknown op userId={} op={} slot={}", userId, op, slot);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown op");
            }
        }
    }

    /**
     * Handles the "GO" operation — usually starts or loads a quest.
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param userId  user identifier
     * @param questId quest identifier
     * @param slot    slot index
     * @param next    redirect target path
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if forwarding fails
     */
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, String questId, int slot, String next)
            throws IOException, ServletException {
        log.warn("Slots GO not implemented userId={} slot={}", userId, slot);
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Handles the "DELETE" operation — clears the specified slot.
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param userId  user identifier
     * @param questId quest identifier
     * @param slot    slot index to delete
     * @throws IOException if redirect fails
     */
    @SuppressWarnings("unused")
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp, String userId, String questId, int slot) throws IOException {
        saveState.clearGlobalSlot(userId, slot);
        req.getSession().setAttribute(WebConst.Attr.FLASH, "Slot №" + (slot + 1) + " delete.");
        log.debug("Slots DELETE redirect userId={} path={}", userId, path());
        Web.redirectKeep(req, resp, path(), WebConst.ParamGroup.SLOT_NAV);
    }

    /**
     * Handles the "CONFIRM" operation — optional confirmation logic.
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param userId  user identifier
     * @param questId quest identifier
     * @param slot    slot index
     * @param next    redirect target
     * @throws IOException if forwarding fails
     */
    protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                 String userId, String questId, int slot, String next)
            throws IOException {
        log.warn("Slots CONFIRM not implemented userId={} slot={}", userId, slot);
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Handles the "CANCEL" operation — redirects back to the previous view.
     *
     * @param req     HTTP request
     * @param resp    HTTP response
     * @param userId  user identifier
     * @param questId quest identifier
     * @param slot    slot index
     * @throws IOException if redirect fails
     */
    @SuppressWarnings("unused")
    protected void handleCancel(HttpServletRequest req, HttpServletResponse resp,
                                String userId, String questId, int slot)
            throws IOException {
        String back = Web.addParamsFromReqEncoded(
                req, req.getContextPath() + path(),
                WebConst.Param.NEXT, WebConst.Param.PURPOSE, WebConst.Param.NODE, WebConst.Param.CUSTOM
        );
        log.debug("Slots CANCEL redirect userId={} to={}", userId, back);
        resp.sendRedirect(resp.encodeRedirectURL(back));
    }
}
