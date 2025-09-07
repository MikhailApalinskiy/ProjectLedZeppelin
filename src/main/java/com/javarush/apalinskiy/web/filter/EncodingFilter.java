package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.web.util.WebConst;
import jakarta.servlet.*;

import java.io.IOException;

public class EncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(WebConst.Charset.UTF8);
        response.setCharacterEncoding(WebConst.Charset.UTF8);
        chain.doFilter(request, response);
    }
}
