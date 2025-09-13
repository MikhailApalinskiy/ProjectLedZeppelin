package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
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
import java.util.Optional;

/**
 * Authentication servlet handling both login and registration flows.
 * <p>
 * This servlet is mapped to two paths (typically {@code /login} and {@code /register})
 * and dispatches by {@link HttpServletRequest#getServletPath()}:
 * <ul>
 *   <li><b>GET</b> — forwards to corresponding JSP pages;</li>
 *   <li><b>POST</b> — performs login or registration.</li>
 * </ul>
 * User session is renewed on successful authentication to prevent session fixation.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>On {@link #init(ServletConfig)} resolves {@link UserService} from {@code ServletContext}.</li>
 *   <li>If the service is not available, initialization fails with {@link UnavailableException}.</li>
 * </ul>
 *
 * <h3>Security notes</h3>
 * <ul>
 *   <li>Passwords are accepted via form parameters and passed to {@link UserService} for verification/creation.</li>
 *   <li>On successful login/registration, session is recreated using {@link Web#renewSessionAndPut}.</li>
 *   <li>Authorization to protected resources should be enforced by upstream filters.</li>
 * </ul>
 *
 * <h3>Views</h3>
 * <ul>
 *   <li>Login page: {@code WebConst.Jsp.LOGIN}</li>
 *   <li>Register page: {@code WebConst.Jsp.REGISTER}</li>
 * </ul>
 *
 * <h3>Redirection</h3>
 * <p>
 * After successful login/registration, user is redirected to {@link Web#safeNextOrHome},
 * honoring the optional {@code next} parameter if safe.
 * </p>
 *
 * <h3>Error handling</h3>
 * <ul>
 *   <li>Unknown servlet path → 404.</li>
 *   <li>Login failure → sets {@code WebConst.Attr.ERROR = WebConst.Msg.BAD_CREDENTIALS} and forwards to login JSP.</li>
 *   <li>Registration failures:
 *     <ul>
 *       <li>{@link DuplicateLoginException}/{@link IllegalArgumentException} → message shown on the register page;</li>
 *       <li>{@link IllegalStateException} → generic internal error message.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @see UserService
 * @see Web
 * @see WebConst
 */
public class AuthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);

    /** Resolved from the servlet context; marked transient to avoid accidental serialization. */
    private transient UserService userService;

    /**
     * Resolves {@link UserService} from the servlet context.
     *
     * @throws UnavailableException if the service cannot be found
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.userService = Web.ctxBean(config.getServletContext(), WebConst.Ctx.USER_SERVICE, UserService.class);
            log.debug("AuthServlet initialized");
        } catch (IllegalStateException e) {
            log.error("Initialization failed: UserService not found in ServletContext", e);
            throw new UnavailableException("UserService not found in ServletContext.");
        }
    }

    /**
     * Serves authentication pages.
     * <ul>
     *   <li><b>/login</b> → forwards to {@code WebConst.Jsp.LOGIN}</li>
     *   <li><b>/register</b> → forwards to {@code WebConst.Jsp.REGISTER}</li>
     * </ul>
     * Unknown paths respond with 404.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (WebConst.Path.LOGIN.equals(path)) {
            log.debug("GET /login");
            Web.forward(req, resp, WebConst.Jsp.LOGIN);
        } else if (WebConst.Path.REGISTER.equals(path)) {
            log.debug("GET /register");
            Web.forward(req, resp, WebConst.Jsp.REGISTER);
        } else {
            log.warn("GET unknown path={}", path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Dispatches POST requests to the appropriate handler by path.
     * <ul>
     *   <li><b>/login</b> → {@link #handleLogin(HttpServletRequest, HttpServletResponse)}</li>
     *   <li><b>/register</b> → {@link #handleRegister(HttpServletRequest, HttpServletResponse)}</li>
     * </ul>
     * Unknown paths respond with 404.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (WebConst.Path.LOGIN.equals(path)) {
            handleLogin(req, resp);
        } else if (WebConst.Path.REGISTER.equals(path)) {
            handleRegister(req, resp);
        } else {
            log.warn("POST unknown path={}", path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles the login flow.
     * <p>
     * Parameters:
     * <ul>
     *   <li>{@code userLogin} — user login (username);</li>
     *   <li>{@code password} — user password.</li>
     * </ul>
     * Behavior:
     * <ul>
     *   <li>On success: renews session with the authenticated {@link User} and redirects to a safe {@code next} or home.</li>
     *   <li>On failure: forwards back to login with error and echoes {@code userLogin}.</li>
     * </ul>
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String login = req.getParameter(WebConst.Param.USER_LOGIN);
        String pass = req.getParameter(WebConst.Param.PASSWORD);
        Optional<User> found = userService.login(login, pass);
        if (found.isPresent()) {
            Web.renewSessionAndPut(req, WebConst.Attr.USER, found.get());
            String target = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
            log.info("Login success login='{}' userId={} redirect={}", login, found.get().getUserId(), target);
            resp.sendRedirect(resp.encodeRedirectURL(target));
        } else {
            log.warn("Login failed login='{}'", login);
            req.setAttribute(WebConst.Attr.ERROR, WebConst.Msg.BAD_CREDENTIALS);
            req.setAttribute("userLogin", login);
            Web.forward(req, resp, WebConst.Jsp.LOGIN);
        }
    }

    /**
     * Handles the registration flow for a new user (role defaults to {@link Role#USER}).
     * <p>
     * Parameters:
     * <ul>
     *   <li>{@code userName} — display name;</li>
     *   <li>{@code userLogin} — desired username (must be unique);</li>
     *   <li>{@code password} — desired password.</li>
     * </ul>
     * Behavior:
     * <ul>
     *   <li>On success: registers the user, renews session, redirects to a safe {@code next} or home.</li>
     *   <li>On validation conflict: forwards back to the register page with an error and echoes inputs.</li>
     *   <li>On internal failures: forwards with a generic internal error message.</li>
     * </ul>
     */
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String name = req.getParameter(WebConst.Param.USER_NAME);
        String login = req.getParameter(WebConst.Param.USER_LOGIN);
        String pass = req.getParameter(WebConst.Param.PASSWORD);
        try {
            User user = userService.register(Role.USER, name, login, pass);
            Web.renewSessionAndPut(req, WebConst.Attr.USER, user);
            String target = Web.safeNextOrHome(req, req.getParameter(WebConst.Param.NEXT));
            log.info("Register success userId={} login='{}' redirect={}", user.getUserId(), user.getUserLogin(), target);
            resp.sendRedirect(resp.encodeRedirectURL(target));
        } catch (DuplicateLoginException | IllegalArgumentException e) {
            log.warn("Register failed login='{}' reason={}", login, e.getMessage());
            req.setAttribute(WebConst.Attr.ERROR, e.getMessage());
            req.setAttribute("userName", name);
            req.setAttribute("userLogin", login);
            Web.forward(req, resp, WebConst.Jsp.REGISTER);
        } catch (IllegalStateException e) {
            log.error("Register internal error login='{}'", login, e);
            req.setAttribute(WebConst.Attr.ERROR, WebConst.Msg.INTERNAL_ERROR);
            req.setAttribute("userName", name);
            req.setAttribute("userLogin", login);
            Web.forward(req, resp, WebConst.Jsp.REGISTER);
        }
    }
}