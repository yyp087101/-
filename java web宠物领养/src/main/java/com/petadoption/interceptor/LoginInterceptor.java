package com.petadoption.interceptor;

import com.petadoption.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin") && user.getRole() != 1) {
            response.sendRedirect("/index");
            return false;
        }
        return true;
    }
}
