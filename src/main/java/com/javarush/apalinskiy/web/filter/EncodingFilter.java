package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * Servlet filter that enforces UTF-8 character encoding for all incoming requests and outgoing responses.
 *
 * <p>This filter ensures consistent text encoding across the entire web application,
 * preventing issues with international characters or symbols. It should typically be
 * applied to all URL patterns to guarantee correct encoding for parameters, forms, and content.</p>
 *
 * <p>The filter uses {@link WebConst.Charset#UTF8} as the standard character set.</p>
 */
public class EncodingFilter implements Filter {

    /**
     * Sets the UTF-8 encoding for the request and response before continuing the filter chain.
     *
     * @param request  incoming servlet request
     * @param response outgoing servlet response
     * @param chain    filter chain used to pass the request to the next element
     * @throws IOException      if an I/O error occurs during processing
     * @throws ServletException if request forwarding fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(WebConst.Charset.UTF8);
        response.setCharacterEncoding(WebConst.Charset.UTF8);
        chain.doFilter(request, response);
    }
}
