package com.zyp.help.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StreamUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用企业级安全过滤器配置 - 抵御常见 OWASP Top 10 攻击
 *
 * <p>全面覆盖：URI、QueryString、Request Headers、Parameters、Cookies、JSON Body/XML Body。
 *
 * @author zyp
 * @version 2.0.0
 * @since 2026-07-14
 */
@Slf4j(topic = "安全过滤器")
@Configuration
public class SecurityFilter {

    @Bean
    public FilterRegistrationBean<SecurityFilterImpl> securityFilterRegistration() {
        FilterRegistrationBean<SecurityFilterImpl> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityFilterImpl());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("securityFilter");
        return registration;
    }

    @Slf4j(topic = "安全过滤器")
    public static class SecurityFilterImpl implements Filter {

        // ==========================================
        // 核心检测正则规则（可自由追加）
        // ==========================================

        // 1. JNDI 注入正则
        private static final Pattern JNDI_PATTERN = Pattern.compile(
                "\\$\\{\\s*jndi:",
                Pattern.CASE_INSENSITIVE
        );

        // 2. 路径遍历正则（全面覆盖了 ../, ..\, 各种 URL 百分号编码以及系统敏感路径如 /etc/passwd, c:\windows）
        private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
                "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|\\.\\.%2f|%2e%2e%5c|\\.\\.%5f|%2e%2e%5f|etc/passwd|win\\.ini|boot\\.ini)",
                Pattern.CASE_INSENSITIVE
        );

        // 3. 敏感系统目录访问 (WEB-INF, META-INF 等隐藏资源)
        private static final Pattern SYSTEM_DIR_PATTERN = Pattern.compile(
                "(WEB-INF|META-INF|\\.git|\\.svn|\\.env)",
                Pattern.CASE_INSENSITIVE
        );

        // 4. 通用特殊字符深度清洗正则（用于拦截非法畸形十六进制或空字节截断，如 Tomcat 报错里的 %%00）
        private static final Pattern MALFORMED_CHAR_PATTERN = Pattern.compile(
                "(%00|%%00|\\x00)",
                Pattern.CASE_INSENSITIVE
        );

        // 5. 允许的 HTTP 方法
        private static final Set<String> ALLOWED_METHODS = new HashSet<>(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
        ));

        @Override
        public void init(FilterConfig filterConfig) {
            log.info("========================================");
            log.info(" 2.0 通用高安全防护过滤器初始化完成");
            log.info(" 防护范围: URI / Query / Headers / Parameters / Cookies / Request Body");
            log.info("========================================");
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                             FilterChain filterChain) throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String method = request.getMethod();

            // 1. 验证 HTTP 方法
            if (!ALLOWED_METHODS.contains(method.toUpperCase())) {
                sendBlockResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "405 Method Not Allowed");
                return;
            }

            // 2. 包装 Request（支持重复读取 Request Body）
            // 注意：排除特殊的上传请求（如 multipart/form-data），避免影响大文件上传
            HttpServletRequest requestToUse = request;
            String contentType = request.getContentType();
            if (contentType != null && !contentType.toLowerCase().startsWith("multipart/")) {
                try {
                    requestToUse = new RepeatedlyReadRequestWrapper(request);
                } catch (Exception e) {
                    log.error("包装安全 Request 失败: ", e);
                }
            }

            // 3. 通用深度安全检测
            if (isMaliciousRequest(requestToUse)) {
                sendBlockResponse(response, HttpServletResponse.SC_FORBIDDEN, "403 Forbidden - Security Rules Violated");
                return;
            }

            // 安全检查通过，传递包装后的 Request 对象
            filterChain.doFilter(requestToUse, servletResponse);
        }

        /**
         * 通用检测调度引擎（核心逻辑）
         */
        private boolean isMaliciousRequest(HttpServletRequest request) {
            // 1. 检测 URI、原始URI 及 QueryString（进行深度URL解码）
            String rawUri = (String) request.getAttribute("javax.servlet.include.request_uri");
            if (rawUri == null) { rawUri = request.getRequestURI(); }
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();

            if (checkMaliciousContent(rawUri) || checkMaliciousContent(uri) || checkMaliciousContent(queryString)) {
                return true;
            }

            // 2. 检测所有请求参数 (Parameters - GET/POST 表单参数)
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (checkMaliciousContent(paramName)) return true;

                String[] paramValues = request.getParameterValues(paramName);
                if (paramValues != null) {
                    for (String value : paramValues) {
                        if (checkMaliciousContent(value)) return true;
                    }
                }
            }

            // 3. 检测所有请求头 (Headers - 杜绝硬编码白名单)
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                Enumeration<String> headers = request.getHeaders(headerName);
                while (headers.hasMoreElements()) {
                    String headerValue = headers.nextElement();
                    if (checkMaliciousContent(headerValue)) return true;
                }
            }

            // 4. 检测所有 Cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (checkMaliciousContent(cookie.getName()) || checkMaliciousContent(cookie.getValue())) {
                        return true;
                    }
                }
            }

            // 5. 检测 JSON / XML / Text Request Body (通过包装类读取)
            if (request instanceof RepeatedlyReadRequestWrapper) {
                String body = ((RepeatedlyReadRequestWrapper) request).getBody();
                if (checkMaliciousContent(body)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * 针对单一值的多维度深度扫描（可横向追加任何防护正则）
         */
        private boolean checkMaliciousContent(String value) {
            if (value == null || value.isEmpty()) {
                return false;
            }

            // 1. 检测原始值
            if (hitSecurityRules(value)) return true;

            // 2. 进行深度解码防御（防范多重、变异编码绕过，诸如 %%00, %252e）
            String decodedValue = decodeUrlFully(value);
            if (hitSecurityRules(decodedValue)) return true;

            return false;
        }

        /**
         * 规则匹配器
         */
        private boolean hitSecurityRules(String text) {
            if (text == null || text.isEmpty()) return false;

            // a. 校验空字节及畸形编码
            if (MALFORMED_CHAR_PATTERN.matcher(text).find()) return true;

            // b. 校验 JNDI 注入
            if (JNDI_PATTERN.matcher(text).find()) return true;

            // c. 校验 路径遍历
            if (PATH_TRAVERSAL_PATTERN.matcher(text).find()) return true;

            // d. 校验 隐藏敏感系统目录访问
            if (SYSTEM_DIR_PATTERN.matcher(text).find()) return true;

            return false;
        }

        /**
         * 递归深度 URL 解码，还原其本质字符
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
                    // 使用 UTF-8 标准解码
                    decoded = URLDecoder.decode(decoded, "UTF-8");
                    count++;
                    // 防止死循环，最大解码 3 次，基本上多重编码在这个深度已被还原
                } while (!decoded.equals(previous) && count < 3);
            } catch (Exception e) {
                return value; // 出现无法解码字符（如非法 %）时，退回当前文本
            }
            return decoded;
        }

        /**
         * 发送拦截响应
         */
        private void sendBlockResponse(HttpServletResponse response, int status, String message) throws IOException {
            response.setStatus(status);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(message);
            response.flushBuffer();
        }

        @Override
        public void destroy() {
            log.info("安全过滤器已销毁");
        }
    }

    /**
     * 自定义 HttpServletRequest 包装类。
     * 用于解决：ServletInputStream 只能读取一次，导致后续 Controller 获取不到 Body 内容的痛点。
     */
    private static class RepeatedlyReadRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] bodyBytes;

        public RepeatedlyReadRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            // 缓存 Request Body 的数据，便于多次读取及过滤扫描
            this.bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        }

        public String getBody() {
            return new String(bodyBytes, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bodyBytes);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream(), StandardCharsets.UTF_8));
        }
    }
}