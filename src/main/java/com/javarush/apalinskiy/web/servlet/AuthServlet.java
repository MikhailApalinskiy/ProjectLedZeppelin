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
 * Servlet responsible for handling authentication-related actions such as
 * user login and registration.
 *
 * <p>This servlet processes both GET and POST requests for {@code /login}
 * and {@code /register} paths. It interacts with {@link UserService}
 * to perform credential verification, user creation, and session management.</p>
 *
 * <p>Features:</p>
 * <ul>
 *     <li>Displays login and registration pages</li>
 *     <li>Validates credentials and initializes authenticated sessions</li>
 *     <li>Handles duplicate logins and invalid input gracefully</li>
 *     <li>Redirects users back to intended target (via {@code next} parameter) after successful auth</li>
 * </ul>
 */
public class AuthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);

    private transient UserService userService;

    /**
     * Initializes the servlet by retrieving the {@link UserService} bean from the servlet context.
     *
     * @param config servlet configuration provided by the container
     * @throws ServletException if the service cannot be loaded
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
     * Handles GET requests for authentication-related pages.
     *
     * <p>Supported endpoints:</p>
     * <ul>
     *     <li>{@code /login} — renders the login form</li>
     *     <li>{@code /register} — renders the registration form</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if JSP forwarding fails
     * @throws IOException      if an I/O error occurs
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
     * Handles POST requests for authentication actions.
     *
     * <p>Supported endpoints:</p>
     * <ul>
     *     <li>{@code /login} — verifies credentials and logs the user in</li>
     *     <li>{@code /register} — registers a new user account</li>
     * </ul>
     *
     * @param req  current HTTP request
     * @param resp current HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if an I/O error occurs
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
     * Processes user login attempts.
     *
     * <p>On successful authentication, the session is renewed to prevent fixation attacks,
     * and the user is redirected either to their requested page (via {@code next}) or to the home page.</p>
     *
     * @param req  current HTTP request containing login credentials
     * @param resp current HTTP response used for redirection
     * @throws IOException      if redirect fails
     * @throws ServletException if forwarding to the login form fails
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
     * Processes user registration requests.
     *
     * <p>Registers a new user with role {@link Role#USER}. Handles duplicate login errors
     * and invalid input by redisplaying the registration form with an appropriate message.</p>
     *
     * @param req  HTTP request containing registration parameters
     * @param resp HTTP response used for redirection
     * @throws IOException      if redirect fails
     * @throws ServletException if forwarding fails
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