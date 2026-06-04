package com.zyp.help.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 安全过滤器配置 - 防止常见Web攻击
 * 
 * <p>主要功能：
 * <ul>
 *   <li>防止JNDI注入攻击（如Log4Shell漏洞）</li>
 *   <li>防止路径遍历攻击（Path Traversal）</li>
 *   <li>限制HTTP方法，只允许安全的请求方法</li>
 *   <li>检测并阻止恶意请求头</li>
 * </ul>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j(topic = "安全过滤器")
@Configuration
public class SecurityFilter {

    /**
     * 注册安全过滤器
     * 
     * @return FilterRegistrationBean 过滤器注册Bean
     */
    @Bean
    public FilterRegistrationBean<SecurityFilterImpl> securityFilter() {
        FilterRegistrationBean<SecurityFilterImpl> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityFilterImpl());
        registration.addUrlPatterns("/*");  // 过滤所有请求
        registration.setOrder(1);  // 设置执行顺序（数字越小越优先）
        registration.setName("securityFilter");
        log.info("安全过滤器注册完成，拦截路径: /*");
        return registration;
    }

    /**
     * 安全过滤器实现类
     */
    @Slf4j(topic = "安全过滤器")
    public static class SecurityFilterImpl implements Filter {

        /**
         * JNDI注入攻击模式匹配
         * 用于检测和阻止类似 ${jndi:ldap://...} 的攻击载荷
         */
        private static final Pattern JNDI_PATTERN = Pattern.compile(
            "\\$\\{\\s*jndi:", 
            Pattern.CASE_INSENSITIVE
        );

        /**
         * 路径遍历攻击模式匹配
         * 用于检测和阻止类似 ../../../etc/passwd 的攻击载荷
         */
        private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|\\.\\.%2f|%2e%2e%5c)", 
            Pattern.CASE_INSENSITIVE
        );

        /**
         * WEB-INF/META-INF访问尝试模式
         */
        private static final Pattern WEB_INF_PATTERN = Pattern.compile(
            "(WEB-INF|META-INF)", 
            Pattern.CASE_INSENSITIVE
        );

        /**
         * 允许的HTTP方法白名单
         */
        private static final Set<String> ALLOWED_METHODS = new HashSet<>(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
        ));

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            log.info("安全过滤器初始化完成");
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, 
                            FilterChain filterChain) throws IOException, ServletException {
            
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            
            String uri = request.getRequestURI();
            String method = request.getMethod();
            
            // 1. 检查HTTP方法是否合法
            if (!ALLOWED_METHODS.contains(method.toUpperCase())) {
                log.warn("拒绝不支持的HTTP方法: {} from {}", method, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                response.getWriter().write("Method Not Allowed");
                return;
            }
            
            // 2. 检查JNDI注入攻击
            if (containsJndiInjection(uri) || containsJndiInjection(request.getQueryString())) {
                log.warn("检测到JNDI注入攻击尝试: {} from {}", uri, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Bad Request");
                return;
            }
            
            // 3. 检查请求头中的JNDI注入
            if (hasJndiInHeaders(request)) {
                log.warn("检测到请求头中的JNDI注入攻击: {} from {}", uri, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Bad Request");
                return;
            }
            
            // 4. 检查路径遍历攻击
            if (containsPathTraversal(uri)) {
                log.warn("检测到路径遍历攻击尝试: {} from {}", uri, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Forbidden");
                return;
            }
            
            // 5. 检查WEB-INF/META-INF访问尝试
            if (containsWebInfAccess(uri)) {
                log.warn("检测到WEB-INF/META-INF访问尝试: {} from {}", uri, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Forbidden");
                return;
            }
            
            // 所有安全检查通过，继续处理请求
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
            log.info("安全过滤器销毁");
        }

        /**
         * 检查字符串中是否包含JNDI注入模式
         * 
         * @param value 待检查的字符串
         * @return 如果包含JNDI注入模式返回true
         */
        private boolean containsJndiInjection(String value) {
            if (value == null || value.isEmpty()) {
                return false;
            }
            return JNDI_PATTERN.matcher(value).find();
        }

        /**
         * 检查请求头中是否包含JNDI注入
         * 
         * @param request HTTP请求对象
         * @return 如果请求头中包含JNDI注入返回true
         */
        private boolean hasJndiInHeaders(HttpServletRequest request) {
            // 检查常见的可能被注入的请求头
            String[] headersToCheck = {
                "User-Agent", "Referer", "X-Forwarded-For", 
                "X-Real-IP", "Host", "Accept", "Accept-Language"
            };
            
            for (String headerName : headersToCheck) {
                String headerValue = request.getHeader(headerName);
                if (containsJndiInjection(headerValue)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 检查是否包含路径遍历攻击模式
         * 
         * @param path 待检查的路径
         * @return 如果包含路径遍历模式返回true
         */
        private boolean containsPathTraversal(String path) {
            if (path == null || path.isEmpty()) {
                return false;
            }
            return PATH_TRAVERSAL_PATTERN.matcher(path).find();
        }

        /**
         * 检查是否包含WEB-INF或META-INF访问尝试
         * 
         * @param path 待检查的路径
         * @return 如果包含WEB-INF/META-INF访问返回true
         */
        private boolean containsWebInfAccess(String path) {
            if (path == null || path.isEmpty()) {
                return false;
            }
            return WEB_INF_PATTERN.matcher(path).find();
        }
    }
}
