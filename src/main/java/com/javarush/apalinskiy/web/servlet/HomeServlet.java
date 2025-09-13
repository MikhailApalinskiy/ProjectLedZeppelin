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
 * Minimal front controller for the application root.
 * <p>
 * Serves the home page when the request targets the context root (i.e., {@code /}).
 * Any other non-root paths handled by this servlet result in 404.
 * Also sets no-cache headers to prevent browsers and proxies from caching the home page.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li><b>GET</b>: forwards to {@code WebConst.Jsp.INDEX} only for the exact root path; otherwise 404.</li>
 *   <li><b>POST</b>: not allowed; responds with 405.</li>
 * </ul>
 *
 * <h3>Caching</h3>
 * <ul>
 *   <li>Sets {@code Cache-Control: no-store, no-cache, must-revalidate, max-age=0} and {@code Pragma: no-cache}.</li>
 * </ul>
 *
 * @see Web
 * @see WebConst
 */
public class HomeServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(HomeServlet.class);

    /**
     * Serves the home page for the context root and rejects other paths with 404.
     * <p>
     * Steps:
     * <ol>
     *   <li>Set strict no-cache headers on the response.</li>
     *   <li>Compute the path segment after the context path.</li>
     *   <li>If it is exactly {@code "/"} or empty, forward to {@code WebConst.Jsp.INDEX}.</li>
     *   <li>Otherwise, respond with 404.</li>
     * </ol>
     * </p>
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException if forwarding fails
     * @throws IOException      if I/O errors occur
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
     * Rejects POST requests to the home endpoint with HTTP 405 (Method Not Allowed).
     *
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws IOException if sending the error fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        log.warn("POST not allowed on home uri={}", req.getRequestURI());
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
