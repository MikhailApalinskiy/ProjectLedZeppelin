package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import com.javarush.apalinskiy.web.view.SlotView;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.Optional;

/**
 * Base servlet for listing and manipulating user's global save slots.
 * <p>
 * This class centralizes the GET/POST flow and logging; concrete subclasses
 * provide view paths and (optionally) confirm flow behavior.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Requires authenticated user (delegates to {@code requireAuthOrRedirect}).</li>
 *   <li>On {@code GET}: pulls flash message, loads all slots via {@code buildSlotsAll(userId)},
 *       and forwards to {@link #listJsp()}.</li>
 *   <li>On {@code POST}: routes by operation:
 *     <ul>
 *       <li>{@code GO} &rarr; {@link #handleGo(HttpServletRequest, HttpServletResponse, String, String, int, String)}</li>
 *       <li>{@code DELETE} &rarr; {@link #handleDelete(HttpServletRequest, HttpServletResponse, String, String, int)}</li>
 *       <li>{@code CONFIRM} &rarr; {@link #handleConfirm(HttpServletRequest, HttpServletResponse, String, String, int, String)}</li>
 *       <li>{@code CANCEL} &rarr; {@link #handleCancel(HttpServletRequest, HttpServletResponse, String, String, int)}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Parameters consumed on POST</h3>
 * <ul>
 *   <li>{@code op} — one of {@code GO}, {@code DELETE}, {@code CONFIRM}, {@code CANCEL}.</li>
 *   <li>{@code slot} — zero-based slot index; must be in {@code [0, WebConst.SLOT_COUNT)}.</li>
 *   <li>{@code next} — optional path to continue after a successful operation (defaults to {@code WebConst.Path.QUEST}).</li>
 *   <li>{@code custom} — optional quest id (used by some handlers).</li>
 * </ul>
 *
 * <h3>Confirm flow</h3>
 * <p>
 * Subclasses that support multi-step confirmation must override {@link #confirmJsp()} to return a JSP path.
 * If {@link #confirmJsp()} returns {@code null}, {@code CONFIRM}/{@code CANCEL} are rejected with 405.
 * </p>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Flash messages are stored under {@code WebConst.Attr.FLASH}.</li>
 *   <li>Slot list is exposed to the view as request attribute {@code "slots"}.</li>
 *   <li>Persistence primitives such as {@code saveState.clearGlobalSlot(...)} are inherited from {@code BaseSaveServlet}.</li>
 * </ul>
 *
 * @see BaseSaveServlet
 * @see SlotView
 * @see WebConst
 * @see Web
 */
public abstract class AbstractSlotsServlet extends BaseSaveServlet {

    private static final Logger log = LoggerFactory.getLogger(AbstractSlotsServlet.class);

    /**
     * @return servlet-relative path used for redirects back to the slots page
     * (e.g., {@code /slots} or {@code /profile/saves})
     */
    protected abstract String path();

    /**
     * @return JSP path for the slots list view (e.g., {@code /WEB-INF/jsp/slots/list.jsp})
     */
    protected abstract String listJsp();

    /**
     * @return JSP path for the confirm view; return {@code null} if the confirm flow is not supported.
     */
    protected String confirmJsp() {
        return null;
    }

    /**
     * Handles listing of slots for the authenticated user and forwards to {@link #listJsp()}.
     * <ul>
     *   <li>Redirects to login if user is not authenticated.</li>
     *   <li>Reads flash message and exposes it as request attribute.</li>
     *   <li>Builds slots via {@code buildSlotsAll(userId)} and stores as {@code "slots"} attribute.</li>
     * </ul>
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
     * Validates input and dispatches to operation-specific handlers.
     * <p>
     * Returns 400 on missing/invalid parameters; returns 405 for operations that are
     * not supported by the subclass (i.e., when {@link #confirmJsp()} is {@code null}).
     * </p>
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
     * Handles the "GO" operation — typically "load/go to this slot".
     * <p>
     * Default implementation rejects the method with 405. Subclasses should implement
     * navigation and/or loading logic, then redirect to {@code next}.
     * </p>
     *
     * @param userId  current user id (stringified)
     * @param questId target quest id (may be {@code null} depending on flow)
     * @param slot    zero-based slot index
     * @param next    next path to continue on success
     */
    protected void handleGo(HttpServletRequest req, HttpServletResponse resp,
                            String userId, String questId, int slot, String next)
            throws IOException, ServletException {
        log.warn("Slots GO not implemented userId={} slot={}", userId, slot);
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Handles the "DELETE" operation: clears the global slot and redirects back to {@link #path()}.
     * <p>
     * Adds a flash message like {@code "Slot №<n> delete."} and preserves slot navigation params.
     * </p>
     */
    @SuppressWarnings("unused")
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp, String userId, String questId, int slot) throws IOException {
        saveState.clearGlobalSlot(userId, slot);
        req.getSession().setAttribute(WebConst.Attr.FLASH, "Slot №" + (slot + 1) + " delete.");
        log.debug("Slots DELETE redirect userId={} path={}", userId, path());
        Web.redirectKeep(req, resp, path(), WebConst.ParamGroup.SLOT_NAV);
    }

    /**
     * Handles the "CONFIRM" step of a multi-step operation.
     * <p>
     * Default implementation rejects the method with 405. Subclasses should render
     * a confirm view (or redirect to it) using {@link #confirmJsp()}.
     * </p>
     */
    protected void handleConfirm(HttpServletRequest req, HttpServletResponse resp,
                                 String userId, String questId, int slot, String next)
            throws IOException {
        log.warn("Slots CONFIRM not implemented userId={} slot={}", userId, slot);
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Handles user-initiated cancellation from the confirm step.
     * <p>
     * Redirects back to {@link #path()} while keeping a subset of navigation params
     * ({@code next}, {@code purpose}, {@code node}, {@code custom}).
     * </p>
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
