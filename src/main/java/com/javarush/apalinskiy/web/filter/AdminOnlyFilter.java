package com.javarush.apalinskiy.web.filter;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AdminOnlyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse w = (HttpServletResponse) resp;
        User me = (User) r.getSession().getAttribute(WebConst.Attr.USER);
        if (me == null || me.getRole() != Role.ADMIN) {
            Web.redirectErr(r, w, WebConst.Path.HOME, "Access denied");
            return;
        }
        chain.doFilter(req, resp);
    }
}
