package com.zyp.help.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
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
    public FilterRegistrationBean<SecurityFilterImpl> securityFilterRegistration() {
        FilterRegistrationBean<SecurityFilterImpl> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityFilterImpl());
        registration.addUrlPatterns("/*");  // 过滤所有请求
        registration.setOrder(1);  // 设置执行顺序（数字越小越优先）
        registration.setName("securityFilter");
        // log.info("安全过滤器注册完成，拦截路径: /*");
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
                "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|\\.\\.%2f|%2e%2e%5c|\\.\\.%5f|%2e%2e%5f|etc/passwd)",
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
            log.info("========================================");
            log.info("安全过滤器初始化完成");
            // log.info("拦截路径: /*");
            // log.info("防护功能:");
            // log.info("  - JNDI注入防护");
            // log.info("  - 路径遍历防护");
            // log.info("  - WEB-INF/META-INF访问防护");
            // log.info("  - HTTP方法限制");
            log.info("========================================");
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            // 获取原始请求URI（包括未解码的路径遍历模式）
            String rawUri = (String) request.getAttribute("javax.servlet.include.request_uri");
            if (rawUri == null) {
                rawUri = request.getRequestURI();
            }

            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String method = request.getMethod();
            String remoteAddr = request.getRemoteAddr();

            // 0. 记录所有请求（用于调试和审计）- 已禁用
            // if (queryString != null && !queryString.isEmpty()) {
            //     log.info("收到请求: {} {}?{} from {}", method, uri, queryString, remoteAddr);
            // } else {
            //     log.info("收到请求: {} {} from {}", method, uri, remoteAddr);
            // }

            // 1. 检查HTTP方法是否合法
            if (!ALLOWED_METHODS.contains(method.toUpperCase())) {
                // log.warn("⚠️  [SECURITY] 拒绝不支持的HTTP方法: {} | URI: {} | IP: {}", method, uri, remoteAddr);
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("405 Method Not Allowed");
                return;
            }

            // 2. 检查JNDI注入攻击 - 检查URI和查询字符串（包括URL解码）
            if (containsJndiInjection(uri) || containsJndiInjection(queryString)) {
                // log.warn("⚠️  [SECURITY] 检测到JNDI注入攻击尝试 (URI/Query): {} | Query: {} | IP: {}", uri, queryString, remoteAddr);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("400 Bad Request - JNDI Injection Detected");
                return;
            }

            // 2.1 升级：使用全量/循环解码，彻底把多重编码打回原形
            String decodedUri = decodeUrlFully(uri);
            String decodedQuery = decodeUrlFully(queryString);
            // 检查解码后的 JNDI 注入
            if (containsJndiInjection(decodedUri) || containsJndiInjection(decodedQuery)) {
                // log.warn("⚠️  [SECURITY] 检测到JNDI注入攻击尝试 (Decoded): {} | DecodedQuery: {} | IP: {}", uri, decodedQuery, remoteAddr);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("400 Bad Request - JNDI Injection Detected");
                return;
            }

            // 2.2 检查所有请求参数
            if (hasJndiInParameters(request)) {
                // log.warn("⚠️  [SECURITY] 检测到JNDI注入攻击尝试 (Parameters): {} | IP: {}", uri, remoteAddr);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("400 Bad Request - JNDI Injection Detected");
                return;
            }

            // 3. 检查请求头中的JNDI注入
            if (hasJndiInHeaders(request)) {
                // log.warn("⚠️  [SECURITY] 检测到请求头中的JNDI注入攻击: {} | IP: {}", uri, remoteAddr);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("400 Bad Request - JNDI Injection Detected");
                return;
            }

            // 4.
            if (containsPathTraversal(rawUri) || containsPathTraversal(uri) ||
                    containsPathTraversal(queryString)) {
                // log.warn("⚠️  [SECURITY] 检测到路径遍历攻击尝试: Raw={} | Cleaned={} | IP: {}", rawUri, uri, remoteAddr);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("403 Forbidden - Path Traversal Detected");
                return;
            }

            // 4. 检查路径遍历攻击
            // 检查原始URI和清理后的URI:   注意：Tomcat可能会自动清理/../ ，所以我们需要检查多个来源
            // 增加对深度解码后内容的过滤:  这样黑客的 .%252e/ 无论藏在原始 URI 还是变体里，解完码变成 ../ 就会在这里被直接干掉！
            if (containsPathTraversal(rawUri) ||
                    containsPathTraversal(uri) ||
                    containsPathTraversal(queryString) ||
                    containsPathTraversal(decodedUri) ||
                    containsPathTraversal(decodedQuery)) {

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("403 Forbidden - Path Traversal Detected");
                return;
            }

            // 5. 检查WEB-INF/META-INF访问尝试
            if (containsWebInfAccess(uri)) {
                // log.warn("⚠️  [SECURITY] 检测到WEB-INF/META-INF访问尝试: {} | IP: {}", uri, remoteAddr);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("403 Forbidden - Access Denied");
                return;
            }

            // 所有安全检查通过
            // log.debug("✓ 请求通过安全检查: {} {}", method, uri);

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
         * URL解码
         *
         * @param value 待解码的字符串
         * @return 解码后的字符串，如果为null则返回null
         */
        private String decodeUrl(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (Exception e) {
                return value;
            }
        }

        /**
         * 深度/循环 URL 解码（对付黑客的双重或多重 URL 编码绕过）
         * 彻底还原例如：%252e -> %2e -> .
         *
         * @param value 待解码的字符串
         * @return 完全解码后的字符串
         */
        private String decodeUrlFully(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            String decoded = value;
            String previous;
            try {
                int count = 0;
                do {
                    previous = decoded;
                    decoded = URLDecoder.decode(decoded, "UTF-8");
                    count++;
                } while (!decoded.equals(previous) && count < 3);
            } catch (Exception e) {
                return value;
            }
            return decoded;
        }

        /**
         * 检查请求参数中是否包含JNDI注入
         *
         * @param request HTTP请求对象
         * @return 如果参数中包含JNDI注入返回true
         */
        private boolean hasJndiInParameters(HttpServletRequest request) {
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);

                if (paramValues != null) {
                    for (String paramValue : paramValues) {
                        if (containsJndiInjection(paramValue)) {
                            // log.debug("检测到JNDI注入参数: {}={}", paramName, paramValue);
                            return true;
                        }
                        // 也检查解码后的值
                        String decodedValue = decodeUrlFully(paramValue);
                        if (containsJndiInjection(decodedValue)) {
                            // log.debug("检测到JNDI注入参数(解码后): {}={}", paramName, decodedValue);
                            return true;
                        }
                    }
                }
            }
            return false;
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
