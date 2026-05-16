package com.zyp.help.controller;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import cn.hutool.core.io.FileUtil;
import com.zyp.help.context.AgentContextHolder;
import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件下载控制器
 *
 * <p>此控制器专门用于提供文件下载服务，主要包括：
 * <ul>
 *   <li>ja-netfilter代理工具的下载</li>
 *   <li>其他相关工具文件的下载</li>
 * </ul>
 *
 * <p>ja-netfilter是一个基于Java Instrumentation API实现的动态字节码修改工具，
 * 主要用于在应用程序运行时动态修改字节码，实现特定的功能。
 *
 * <p>下载的文件包含：
 * <ul>
 *   <li>ja-netfilter核心程序</li>
 *   <li>预配置的配置文件</li>
 *   <li>相关的说明文档</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "文件下载")  // 使用自定义日志主题
@RestController
@RequiredArgsConstructor  // Lombok自动生成构造函数
public class ZipController {

    /**
     * 下载ja-netfilter代理工具接口
     *
     * <p>此接口提供预配置的ja-netfilter工具包下载服务。
     * ja-netfilter是一个强大的Java字节码修改工具，可以在运行时动态修改Java应用程序。
     *
     * <p>下载的文件包括：
     * <ul>
     *   <li>ja-netfilter.jar - 核心程序</li>
     *   <li>plugins/ - 各种功能插件</li>
     *   <li>config/ - 配置文件，已预配置了JetBrains产品的相关参数</li>
     *   <li>README.md - 使用说明</li>
     * </ul>
     *
     * <p>使用方法：
     * <ol>
     *   <li>下载并解压文件</li>
     *   <li>在JetBrains IDE启动参数中添加：{@code -javaagent:/path/to/ja-netfilter.jar}</li>
     *   <li>重新启动IDE即可</li>
     * </ol>
     *
     * <p>注意事项：
     * <ul>
     *   <li>请确保使用合法的许可证</li>
     *   <li>仅供学习和研究使用</li>
     *   <li>不得用于商业目的</li>
     * </ul>
     *
     * @return 包含ja-netfilter工具的ZIP文件响应
     */
    @GetMapping("ja-netfilter")
    public ResponseEntity<Resource> downloadJaNetfilter() {
        log.info("接收到ja-netfilter下载请求");

        // 从AgentContextHolder获取ja-netfilter ZIP文件
        File jaNetfilterZipFile = AgentContextHolder.jaNetfilterZipFile();

        log.debug("ja-netfilter文件路径: {}, 文件大小: {} 字节",
                 jaNetfilterZipFile.getAbsolutePath(),
                 jaNetfilterZipFile.length());

        // 构建响应，设置为附件下载
        ResponseEntity<Resource> response = ResponseEntity.ok()
            .header(CONTENT_DISPOSITION, "attachment;filename=" + jaNetfilterZipFile.getName())  // 设置下载文件名
            .contentType(APPLICATION_OCTET_STREAM)  // 设置内容类型为二进制流
            .body(new InputStreamResource(FileUtil.getInputStream(jaNetfilterZipFile)));  // 设置响应体为文件流

        log.info("ja-netfilter下载响应已生成");

        return response;
    }
}
