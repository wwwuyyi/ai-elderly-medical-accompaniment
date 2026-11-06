package com.example.escort.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 对所有非公开接口校验JWT Token，解析后写入UserContext。
 * 公开接口（登录/注册/验证码/Knife4j文档）在 WebMvcConfig 中排除。
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已失效\",\"data\":null}");
            } catch (Exception ignored) {
            }
            return false;
        }

        // 写入上下文
        UserContext.setUserId(jwtUtil.getUserId(token));
        UserContext.setUserRole(jwtUtil.getUserRole(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 防止线程池复用ThreadLocal泄漏
        UserContext.clear();
    }

    /**
     * 从请求头提取Token，格式：Authorization: Bearer {token}
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}