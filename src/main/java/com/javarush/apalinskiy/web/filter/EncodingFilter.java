package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * Servlet filter that enforces UTF-8 character encoding for all requests and responses.
 * <p>
 * This filter sets the character encoding of incoming {@link ServletRequest}
 * and outgoing {@link ServletResponse} to {@link WebConst.Charset#UTF8}.
 * It ensures consistent text handling across the application (e.g. form data,
 * query parameters, and response output).
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Set request encoding to UTF-8 before request processing.</li>
 *   <li>Set response encoding to UTF-8 before sending content to clients.</li>
 *   <li>Delegate control to the next filter or servlet in the chain.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>
 * Typically mapped to all application URLs in {@code web.xml} or via annotations
 * to guarantee consistent encoding in the entire web application.
 * </p>
 */
public class EncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(WebConst.Charset.UTF8);
        response.setCharacterEncoding(WebConst.Charset.UTF8);
        chain.doFilter(request, response);
    }
}
