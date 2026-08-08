package com.travel.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class UserLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object user = request.getSession().getAttribute("user");
        if (user != null) {
            return true;
        }

        String redirect = buildRedirectTarget(request);
        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":401,\"msg\":\"请先登录\",\"loginUrl\":\""
                            + request.getContextPath()
                            + "/user/login\",\"redirect\":\""
                            + escapeJson(redirect)
                            + "\"}");
            return false;
        }

        String encodedRedirect = URLEncoder.encode(redirect, StandardCharsets.UTF_8.name());
        response.sendRedirect(request.getContextPath() + "/user/login?redirect=" + encodedRedirect);
        return false;
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String xRequestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(xRequestedWith);
    }

    private String buildRedirectTarget(HttpServletRequest request) {
        if (isAjaxRequest(request)) {
            String refererRedirect = buildRedirectFromReferer(request);
            if (refererRedirect != null) {
                return refererRedirect;
            }
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            uri = uri + "?" + query;
        }
        return uri;
    }

    private String buildRedirectFromReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.trim().isEmpty()) {
            return null;
        }

        try {
            URI refererUri = URI.create(referer);
            if (!isSameOrigin(request, refererUri)) {
                return null;
            }

            String path = refererUri.getPath();
            if (path == null || path.isEmpty()) {
                return null;
            }

            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                if (!path.startsWith(contextPath)) {
                    return null;
                }
                path = path.substring(contextPath.length());
            }

            if (path.isEmpty()) {
                path = "/";
            }

            String query = refererUri.getRawQuery();
            if (query != null && !query.isEmpty()) {
                path = path + "?" + query;
            }
            return path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isSameOrigin(HttpServletRequest request, URI uri) {
        if (uri.getHost() == null || uri.getScheme() == null) {
            return false;
        }
        if (!request.getScheme().equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        if (!request.getServerName().equalsIgnoreCase(uri.getHost())) {
            return false;
        }

        int requestPort = normalizePort(request.getScheme(), request.getServerPort());
        int refererPort = normalizePort(uri.getScheme(), uri.getPort());
        return requestPort == refererPort;
    }

    private int normalizePort(String scheme, int port) {
        if (port != -1) {
            return port;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return 80;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
