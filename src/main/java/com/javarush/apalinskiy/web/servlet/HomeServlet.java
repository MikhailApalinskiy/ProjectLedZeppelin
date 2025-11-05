package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Root servlet that serves the home (index) page of the application.
 *
 * <p>Handles requests to the root context path ("/") and forwards them
 * to {@link WebConst.Jsp#INDEX}. Any other URI under the same mapping
 * results in a {@code 404 Not Found} error.</p>
 *
 * <p>All responses are marked with no-cache headers to prevent browsers
 * from caching dynamic content.</p>
 */
public class HomeServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(HomeServlet.class);

    /**
     * Handles GET requests to the application root.
     *
     * <p>Forwards "/" to the main index JSP, otherwise returns 404.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException on forwarding errors
     * @throws IOException      on I/O failures
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        String contextPath = req.getContextPath();
        String requestUri = req.getRequestURI();
        String rest = requestUri.substring(contextPath.length());
        if (rest.equals("/") || rest.isEmpty()) {
            log.debug("Home page requested uri={}", requestUri);
            Web.forward(req, resp, WebConst.Jsp.INDEX);
        } else {
            log.warn("Not found uri={} rest={}", requestUri, rest);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Rejects POST requests to the home path.
     *
     * <p>Home page is read-only; attempts to send POST result in
     * {@code 405 Method Not Allowed}.</p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException on write error
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        log.warn("POST not allowed on home uri={}", req.getRequestURI());
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
