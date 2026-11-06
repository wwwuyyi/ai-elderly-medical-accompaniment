package com.example.escort.config;

import com.example.escort.security.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc配置
 * - 跨域（CORS）
 * - 登录拦截器注册 + 公开路径排除
 * - 文件上传目录静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    /**
     * 跨域域名白名单，来自 application.yml 的 cors.allowed-origins。
     * 安全说明：allowCredentials(true) 时禁止使用通配符 "*"，否则任意站点均可携带凭证访问后端。
     */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 跨域配置
     * 仅允许配置文件中指定的前端域名（默认 Vue dev server localhost:5173）跨域请求。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 拦截器注册
     * 排除公开接口：认证/验证码/Knife4j文档/文件上传访问/静态资源
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/captcha",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/upload/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                );
    }

    /**
     * 静态资源映射：将 ./upload/ 目录映射为 /upload/ 访问路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:./upload/");
    }
}