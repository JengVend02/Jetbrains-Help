package com.zyp.help.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * <p>统一处理应用程序中的各种异常，提供友好的错误响应。
 * 主要功能：
 * <ul>
 *   <li>处理JNDI注入相关的MIME类型解析异常</li>
 *   <li>处理不支持的HTTP方法异常</li>
 *   <li>处理资源未找到异常（路径遍历等）</li>
 *   <li>处理其他通用异常</li>
 * </ul>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j(topic = "全局异常处理")
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理无效的MIME类型异常（通常由JNDI注入攻击引起）
     * 
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 错误响应实体
     */
    @ExceptionHandler(InvalidMimeTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMimeTypeException(
            InvalidMimeTypeException e, HttpServletRequest request) {
        
        log.warn("检测到无效的MIME类型请求（可能是JNDI注入攻击）: {} from {}", 
                e.getMessage(), request.getRemoteAddr());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("message", "Bad Request");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * 处理不支持的HTTP方法异常
     * 
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 错误响应实体
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        
        log.debug("不支持的HTTP方法: {} from {}", e.getMethod(), request.getRemoteAddr());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", HttpStatus.METHOD_NOT_ALLOWED.value());
        errorResponse.put("message", "Method Not Allowed");
        errorResponse.put("supportedMethods", e.getSupportedMethods());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * 处理资源未找到异常（路径遍历、WEB-INF访问等）
     * 
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 错误响应实体
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        
        String requestURL = e.getRequestURL();
        log.warn("资源访问被拒绝（可能是路径遍历或敏感文件访问）: {} from {}", 
                requestURL, request.getRemoteAddr());
        
        // 检测是否是恶意访问尝试
        boolean isMalicious = requestURL != null && (
            requestURL.contains("..") || 
            requestURL.toUpperCase().contains("WEB-INF") ||
            requestURL.toUpperCase().contains("META-INF") ||
            requestURL.contains("/etc/") ||
            requestURL.contains("\\windows\\")
        );
        
        if (isMalicious) {
            log.error("检测到恶意访问尝试: {} from {}", requestURL, request.getRemoteAddr());
        }
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", HttpStatus.NOT_FOUND.value());
        errorResponse.put("message", isMalicious ? "Forbidden" : "Resource Not Found");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        HttpStatus status = isMalicious ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND;
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * 捕获客户端中途断开连接的异常，避免打印大量无用堆栈
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e, HttpServletRequest request) {
        // 仅记录 info 或 debug 日志，无需回写任何响应体
        log.info("客户端已断开连接: {}", request.getRequestURI());
    }

    /**
     * 处理通用异常
     * 
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 错误响应实体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception e, HttpServletRequest request) {
        
        log.error("未处理的异常: {} from {}", e.getClass().getSimpleName(), 
                request.getRemoteAddr(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("message", "Internal Server Error");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}
